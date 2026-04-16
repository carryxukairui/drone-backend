# 硬件设备上报流程分析

## 概述

本文档分析无人机反制系统中，硬件设备（探测器、干扰器等）向平台上报数据的完整流程。从 `DeviceReportParserFactory` 出发，整个流程可分为 **接收 → 解析 → 业务处理 → 推送** 四个阶段。

---

## 1. 解析层：工厂 + 责任链模式

### 1.1 核心分发器：DeviceReportParserFactory

```java
@Component
public class DeviceReportParserFactory {
    private final List<DeviceReportParser> parsers;
    public DeviceReportParserFactory(List<DeviceReportParser> parsers) {
        this.parsers = parsers;
    }
    public List<DeviceConvertible> parse(JsonNode jsonNode) throws Exception{
        for (DeviceReportParser parser : parsers) {
            if (parser.supports(jsonNode)) {
                return parser.parse(jsonNode);
            }
        }
        throw new IllegalArgumentException("不支持的厂商数据格式");
    }
}
```

Spring 通过构造函数自动注入所有 `DeviceReportParser` 实现类，形成**责任链**。工厂遍历所有解析器，调用 `supports(jsonNode)` 判断哪个解析器能够处理当前 JSON 格式。

### 1.2 解析器实现

| 解析器 | 匹配条件 | 输出类型 |
|--------|----------|----------|
| `ADeviceParser` | `scannerD` 存在 **且** 子元素包含 `tempeature`（厂商 A 特有的拼写） | `ADeviceReport` |
| `DefaultDeviceParser` | `scannerD` 和 `station_id` 存在 | `DefaultDeviceReport` |

两个解析器的工作流程相似：
1. 读取根节点的 `station_id`
2. 遍历 `scannerD` 数组
3. 通过 `ObjectMapper.treeToValue()` 映射为对应报告对象
4. 为缺失的 `lng`、`lat`、`ip`、`stationId`、`id` 填充默认值
5. 返回 `List<DeviceConvertible>`

### 1.3 无人机探测解析（并行体系）

除设备状态外，系统还有一套独立的解析体系处理无人机探测数据：
- `DroneReportParserFactory` + `DefaultDroneParser`
- 输出 `AlarmConvertible`
- 映射字段包括：`drone_uuid`、`model`、`longitude`、`latitude`、`height`、`frequency`、`scanID`、`Op_Lon`、`Op_Lat` 等

---

## 2. 接收层：双通道入口

硬件数据通过 **HTTP REST** 和 **MQTT** 两种协议接入，最终都汇集到 `HardwareController` 处理。

### 2.1 HTTP REST 接口

`HardwareController` 提供两个 POST 端点：

| 端点 | 方法 | 用途 | 后续处理 |
|------|------|------|----------|
| `POST /sys/portable/status/report` | `reportStatus()` | 设备状态上报 | `DeviceReportParserFactory` |
| `POST /sys/portable/drone/report` | `reportDrone()` | 无人机探测上报 | `DroneReportParserFactory` |

### 2.2 MQTT 订阅

`MqttServiceImpl` 在 `@PostConstruct initSubscribe()` 中订阅三个主题：

| MQTT 主题 | Spring 事件 | 监听方法 |
|-----------|-------------|----------|
| `device/status` | `DeviceReportEvent` | `@EventListener onDeviceReport()` |
| `device/remoteID/report` | `DroneReportEvent` | `@EventListener onDroneReport()` |
| `device/jammer/status` | `DeviceReportEvent` | `@EventListener onDeviceReport()` |

MQTT 消息到达后，通过 Spring 事件机制异步分发到 `HardwareController` 的事件监听器中，处理逻辑与 HTTP 接口**完全一致**。

---

## 3. 业务处理层

### 3.1 设备状态处理流程

```
HardwareController
        ↓
DeviceReportParserFactory.parse(jsonNode)
        ↓
List<DeviceConvertible>
        ↓
DeviceServiceImpl.handleDeviceReport(DeviceConvertible)
```

`DeviceServiceImpl.handleDeviceReport()` 内部逻辑：

1. **转换**：`DeviceConvertible.toDevice()` 转成 `Device` 实体
2. **查库更新**：根据设备 ID 查询现有记录，更新 `linkStatus = 1`（在线）、经纬度、IP、温度、上报时间
3. **特殊覆盖**：对 `RemoteID_95238`、`RemoteID_95239`、`RemoteID_95240` 三个设备 ID 硬编码经纬度覆盖
4. **持久化**：保存到数据库
5. **WebSocket 推送**：将更新后的设备列表推送到 `device:{userId}` 频道
6. **离线检测**：启动/重置 20 秒定时器。若超时未收到新报文，调用 `markOffline()` 将 `linkStatus` 设为 0 并再次推送
7. **无人值守模式**：若用户 `unattended = 1`，查询近期系统日志并推送到 `noAttended:{userId}`

### 3.2 无人机探测（告警）处理流程

```
HardwareController
        ↓
DroneReportParserFactory.parse(jsonNode)
        ↓
List<AlarmConvertible>
        ↓
AlarmServiceImpl.handleDroneReport(AlarmConvertible)
```

`AlarmServiceImpl.handleDroneReport()` 内部逻辑：

1. **转换**：`AlarmConvertible.toAlarm()` 转成 `Alarm` 实体
2. **生成 ID & 写入**：生成新 ID 并执行 upsert 操作写入数据库
3. **用户解析**：根据上报设备反查所属用户
4. **无人值守处置**：若用户处于无人值守模式，调用 `UnattendedService.onTdoaAlarm()` 进行自动处置
5. **常规处置判断**：非无人值守模式下，检查告警是否位于有效处置区域内，若是则标记 `isDisposed = 1`
6. **WebSocket 缓冲推送**：针对同一 `(userId, droneSn)` 的告警进行缓冲，达到 10 条或等待 1.5 秒后批量推送到前端，避免频繁刷新导致页面闪烁

---

## 4. 推送层：WebSocket 架构

| 组件 | 职责 |
|------|------|
| `DeviceWebSocketHandler` | 管理 `device` 和 `unattended` 类型 WebSocket 连接，存储用户偏好到 session attributes |
| `AlarmWebSocketHandler` | 管理 `alarm` 类型 WebSocket 连接 |
| `WebSocketService` | 维护 `ConcurrentMap<String, CopyOnWriteArrayList<WebSocketSession>>`，提供 `sendDeviceListToUser()` 和 `sendAlarmListToUser()` |

---

## 5. 整体架构图

```
┌─────────────────────────────────────┐
│           硬件设备                   │
│  (探测器 / 干扰器 / RemoteID 设备)    │
└──────────────┬──────────────────────┘
               │
    ┌──────────┴──────────┐
    │                     │
    ▼                     ▼
 HTTP POST             MQTT
    │                     │
    │    ┌────────────────┘
    │    │
    ▼    ▼
┌─────────────────────────────────────┐
│       HardwareController            │
│  (REST Controller + Event Listener) │
└──────────────┬──────────────────────┘
               │
    ┌──────────┴──────────┐
    │                     │
    ▼                     ▼
DeviceReportParserFactory  DroneReportParserFactory
    │                     │
    ▼                     ▼
List<DeviceConvertible>  List<AlarmConvertible>
    │                     │
    ▼                     ▼
DeviceServiceImpl        AlarmServiceImpl
    │                     │
    ▼                     ▼
  数据库更新               数据库 upsert
    │                     │
    ▼                     ▼
WebSocket 推送            WebSocket 缓冲推送
(device:{userId})        (alarm 频道)
```

---

## 6. 关键源码文件

| 文件 | 作用 |
|------|------|
| `src/main/java/com/demo/dronebackend/factory/DeviceReportParserFactory.java` | 设备状态解析工厂，责任链分发器 |
| `src/main/java/com/demo/dronebackend/factory/DefaultDeviceParser.java` | 默认厂商设备解析器 |
| `src/main/java/com/demo/dronebackend/factory/ADeviceParser.java` | 厂商 A 设备解析器 |
| `src/main/java/com/demo/dronebackend/controller/HardwareController.java` | 数据接收入口（HTTP + 事件监听） |
| `src/main/java/com/demo/dronebackend/service/impl/MqttServiceImpl.java` | MQTT 订阅与事件发布 |
| `src/main/java/com/demo/dronebackend/service/impl/DeviceServiceImpl.java` | 设备状态业务逻辑 |
| `src/main/java/com/demo/dronebackend/service/impl/AlarmServiceImpl.java` | 告警业务逻辑 |
| `src/main/java/com/demo/dronebackend/ws/WebSocketService.java` | WebSocket 推送服务 |

---

## 7. 设计要点总结

1. **适配器模式**：通过 `DeviceConvertible` / `AlarmConvertible` 接口，屏蔽不同厂商的 JSON 差异，统一转换为内部领域对象。
2. **责任链模式**：`DeviceReportParserFactory` 不依赖具体解析器，新增厂商只需新增 `DeviceReportParser` 实现并注册为 Spring Bean。
3. **双协议兼容**：HTTP 和 MQTT 共用同一套业务逻辑，通过 Spring Event 解耦 MQTT 消息接收与业务处理。
4. **实时性保障**：设备状态通过 WebSocket 实时推送，配合 20 秒定时器实现在线/离线状态自动切换。
5. **前端体验优化**：告警数据采用缓冲批量推送（10条/1.5秒），避免高频单条推送导致页面闪烁。
