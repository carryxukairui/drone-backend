# Drone Backend 项目分析文档

## 项目概述

**项目名称**: drone-backend  
**项目类型**: 无人机管控系统后端  
**技术架构**: Spring Boot 3.x + Java 21  
**主要功能**: 无人机侦测、告警管理、设备控制、无人值守自动处置

---

## 技术栈

| 类别 | 技术选型 | 版本 |
|------|---------|------|
| 基础框架 | Spring Boot | 3.3.4 |
| JDK版本 | Java | 21 |
| ORM框架 | MyBatis Plus | 3.5.9 |
| 数据库 | MySQL | - |
| 连接池 | Druid | 1.2.23 |
| 缓存 | Redis | - |
| 消息通信 | MQTT (Eclipse Paho) | 1.2.5 |
| 实时通信 | WebSocket | - |
| 认证授权 | SA-Token | 1.39.0 |
| 短信服务 | 阿里云 SMS | 3.1.1 |
| 工具类 | Hutool | 5.8.32 |
| 地图服务 | 天地图 API | - |

---

## 项目结构

```
src/main/java/com/demo/dronebackend/
├── config/           # 配置类
│   ├── MqttConfig.java           # MQTT客户端配置
│   ├── WebSocketConfig.java      # WebSocket配置
│   ├── WebConfig.java            # Web MVC配置
│   ├── AliSmsConfig.java         # 阿里云短信配置
│   ├── SchedulingConfig.java     # 定时任务配置
│   └── PerformanceLogAspect.java # 性能日志切面
├── controller/       # 控制器层
│   ├── AdminController.java      # 管理员接口
│   ├── AlarmController.java      # 告警管理接口
│   ├── DeviceController.java     # 设备管理接口
│   ├── DroneController.java      # 无人机管理接口
│   ├── DisposalController.java   # 处置记录接口
│   ├── HardwareController.java   # 硬件数据上报接口
│   ├── ScreenController.java     # 大屏数据接口
│   └── UserController.java       # 用户接口
├── service/          # 服务层
│   ├── AlarmService.java         # 告警服务
│   ├── DeviceService.java        # 设备服务
│   ├── DroneService.java         # 无人机服务
│   ├── UnattendedService.java    # 无人值守服务
│   ├── MqttService.java          # MQTT服务
│   ├── TiandituService.java      # 天地图服务
│   └── impl/                     # 服务实现类
├── mapper/           # 数据访问层
│   ├── AlarmMapper.java
│   ├── DeviceMapper.java
│   ├── DroneMapper.java
│   └── ...
├── pojo/             # 实体类
│   ├── Alarm.java                # 告警实体
│   ├── Device.java               # 设备实体
│   ├── Drone.java                # 无人机实体
│   ├── User.java                 # 用户实体
│   ├── Region.java               # 区域实体
│   └── ...
├── dto/              # 数据传输对象
│   ├── alarm/                    # 告警相关DTO
│   ├── device/                   # 设备相关DTO
│   ├── screen/                   # 大屏展示DTO
│   └── ...
├── ws/               # WebSocket相关
│   ├── WebSocketService.java     # WebSocket服务
│   ├── AlarmWebSocketHandler.java
│   └── DeviceWebSocketHandler.java
├── factory/          # 工厂模式
│   ├── DeviceReportParserFactory.java
│   └── DroneReportParserFactory.java
├── model/            # 模型/领域对象
│   ├── AlarmConvertible.java
│   ├── DeviceConvertible.java
│   └── TimingWheelDelayManager.java
├── constant/         # 常量定义
├── enums/            # 枚举类
├── exception/        # 异常处理
├── interceptor/      # 拦截器
├── util/             # 工具类
└── DroneBackendApplication.java  # 启动类
```

---

## 核心功能模块

### 1. 告警管理 (Alarm)

**功能描述**:
- 接收硬件设备上报的无人机侦测数据
- 告警信息存储与查询
- 告警轨迹记录与展示
- 支持批量删除

**核心实体**: `Alarm.java`
- 无人机信息: SN、型号、类型、ID
- 位置信息: 经度、纬度、高度、频点、带宽
- 时间信息: 起飞时间、降落时间、入侵开始时间
- 轨迹数据: JSON格式存储飞行轨迹
- 处置状态: 是否已处置

**关键接口**:
- `GET /admin/alarms` - 查询告警列表（支持分页、筛选）
- `PUT /admin/alarms/{alarm_id}` - 更新告警信息
- `DELETE /admin/alarms/{alarm_id}` - 删除单条告警
- `POST /admin/alarms/batch_delete` - 批量删除告警

---

### 2. 设备管理 (Device)

**功能描述**:
- 管控设备（侦测器、干扰器）管理
- 设备状态监控（在线/离线）
- 设备位置管理
- 设备指令下发

**设备类型**:
- `JAMMER` - 干扰器（用于反制无人机）
- 侦测设备（用于发现无人机）

**核心属性**:
- 设备名称、类型、所属用户
- 连接状态、位置坐标（经纬度）
- 覆盖范围、电量、温度
- 上报时间

---

### 3. 无人值守自动处置 (Unattended)

**功能描述**:
系统核心功能，实现自动检测并处置黑飞无人机。

**处置流程**:
```
1. 接收TDOA告警
   ↓
2. 验证触发条件（是否为黑飞无人机）
   ↓
3. 区域判断（是否在核心区/反制区）
   ↓
4. 查找最近可用干扰设备
   ↓
5. 发送干扰指令（MQTT）
   ↓
6. 10秒后自动检查，决定关闭或保持干扰
```

**关键代码**: `UnattendedService.java`

**频段配置**:
- 1.2GHz (band=9)
- 1.6GHz (band=16) - 当前默认开启
- 2.4GHz (band=24)
- 5.2GHz (band=52)
- 5.8GHz (band=58)

**干扰时长**: 20秒（可配置）

---

### 4. 硬件数据接入

**数据上报端点**:
- `POST /sys/portable/drone/report` - 无人机侦测数据上报
- `POST /sys/portable/status/report` - 设备状态上报

**数据解析**:
- 使用工厂模式解析不同类型的上报数据
- 支持多种无人机协议解析

**WebSocket实时推送**:
- 告警信息实时推送到前端
- 设备状态实时更新

---

### 5. MQTT通信

**配置参数**:
```yaml
mqtt:
  broker: tcp://localhost:1883
  clientId: DroneMqttClient
  username: mqttadmin
  password: mqttroot
```

**指令主题**:
- `device/jammer/command/startJam` - 启动干扰指令

**指令格式**:
```json
{
  "device_id": "xxx",
  "onoff_09": 1,
  "onoff_16": 1,
  "onoff_24": 1,
  "onoff_52": 1,
  "onoff_58": 1,
  "duration": 20.0
}
```

---

### 6. 用户与权限管理

**认证方式**: SA-Token
- Token有效期: 30天
- 支持并发登录
- UUID格式Token

**用户角色**:
- 管理员
- 普通用户

**功能**:
- 手机号验证码登录
- 密码登录
- 用户CRUD
- 用户绑定无人机

---

### 7. 区域管理

**区域类型**:
- 核心区 (type=1)
- 反制区 (type=2)
- 其他区域

**区域属性**:
- 中心点坐标（经纬度）
- 半径（千米）
- 所属用户

---

### 8. 大屏展示

**数据接口**: `ScreenController.java`

**展示内容**:
- 实时告警统计
- 设备状态分布
- 无人机活动热力图
- 近7天无人机统计
- 重点区域告警分布

---

## 数据库设计

### 核心表结构

**alarm（告警表）**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| drone_sn | VARCHAR | 无人机序列号 |
| drone_model | VARCHAR | 无人机型号 |
| drone_type | VARCHAR | 无人机类型 |
| last_longitude | DOUBLE | 最新经度 |
| last_latitude | DOUBLE | 最新纬度 |
| last_altitude | DOUBLE | 最新高度 |
| frequency | DOUBLE | 频点(MHz) |
| bandwidth | DOUBLE | 带宽 |
| takeoff_time | TIMESTAMP | 起飞时间 |
| landing_time | TIMESTAMP | 降落时间 |
| intrusion_start_time | TIMESTAMP | 入侵开始时间 |
| trajectory | JSON | 飞行轨迹 |
| station_id | VARCHAR | 站点ID |
| detect_type | TINYINT | 检测类型 |
| is_disposed | TINYINT | 是否已处置 |

**device（设备表）**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR | 主键 |
| device_name | VARCHAR | 设备名称 |
| device_type | VARCHAR | 设备类型 |
| device_user_id | BIGINT | 所属用户 |
| link_status | INT | 连接状态(0离线/1在线) |
| longitude | DOUBLE | 经度 |
| latitude | DOUBLE | 纬度 |
| cover_range | DOUBLE | 覆盖范围(米) |
| power | DOUBLE | 电量 |
| temperature | DOUBLE | 温度 |

**drone（无人机表）**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| drone_brand | VARCHAR | 品牌 |
| drone_model | VARCHAR | 型号 |
| drone_sn | VARCHAR | 序列号 |
| type | VARCHAR | 类型(legal/illegal) |
| user_id | BIGINT | 所属用户 |

**region（区域表）**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| center_lat | DOUBLE | 中心纬度 |
| center_lon | DOUBLE | 中心经度 |
| radius | DOUBLE | 半径(千米) |
| type | INT | 区域类型 |
| user_id | BIGINT | 所属用户 |

---

## 配置说明

### application.yaml 关键配置

```yaml
server:
  port: 9803  # 服务端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sys?...
    username: root
    password: 123456
  
  redis:
    host: 39.184.143.110
    port: 6379
    password: redisroot
    database: 2

sa-token:
  token-name: satoken
  timeout: 2592000  # 30天
  token-style: uuid

aliyun:
  sms:
    accessKeyId: xxx
    accessKeySecret: xxx
    signName: "浙江宙合数字科技"
    templateCode: "SMS_490320069"

tianditu:
  reverse-geocode:
    url: http://api.tianditu.gov.cn/geocoder
    key: e983898507c0ba8586c953218dc03b77
```

---

## 系统特性

### 1. 性能优化
- MyBatis Plus分页插件
- Redis缓存
- 数据库连接池(Druid)
- 乐观锁机制

### 2. 安全特性
- SQL注入防护 (BlockAttackInnerInterceptor)
- SA-Token认证
- 全局异常处理
- 操作日志记录

### 3. 高可用设计
- MQTT自动重连
- 干扰指令重试机制（2次重试）
- 设备离线检测（20秒间隔）

### 4. 实时性
- WebSocket实时推送
- MQTT即时通信
- 定时任务调度

---

## 接口清单

### 管理员接口
- `POST /admin/users/login` - 登录
- `POST /admin/users/sendCode` - 发送验证码
- `GET /admin/users` - 获取用户列表
- `POST /admin/users` - 创建用户
- `PUT /admin/users/{id}` - 更新用户
- `DELETE /admin/users/{id}` - 删除用户

### 告警接口
- `GET /admin/alarms` - 告警查询
- `PUT /admin/alarms/{alarm_id}` - 更新告警
- `DELETE /admin/alarms/{alarm_id}` - 删除告警
- `POST /admin/alarms/batch_delete` - 批量删除

### 设备接口
- `GET /admin/devices` - 设备列表
- `POST /admin/devices` - 创建设备
- `PUT /admin/devices/{id}` - 更新设备
- `DELETE /admin/devices/{id}` - 删除设备
- `POST /admin/devices/command` - 发送指令

### 无人机接口
- `GET /admin/drones` - 无人机列表
- `POST /admin/drones` - 添加无人机
- `PUT /admin/drones/{id}` - 更新无人机
- `DELETE /admin/drones/{id}` - 删除无人机

### 大屏接口
- `GET /screen/statistics` - 统计数据
- `GET /screen/devices` - 设备状态
- `GET /screen/alarms` - 实时告警
- `GET /screen/weekly` - 周统计数据

### 硬件上报接口
- `POST /sys/portable/drone/report` - 无人机上报
- `POST /sys/portable/status/report` - 设备状态上报

---

## 开发规范

### 代码规范
- 使用Lombok简化代码
- 统一返回格式 `Result<T>`
- 使用`@RequiredArgsConstructor`进行依赖注入
- 驼峰命名，数据库字段使用下划线命名

### 异常处理
- 全局异常拦截器
- 业务异常统一封装
- 日志分级记录 (info/error/debug)

### 日志规范
- 操作日志记录到system_log表
- 无人值守事件详细记录
- MQTT指令发送记录

---

## 部署说明

### 环境要求
- JDK 21+
- MySQL 8.0+
- Redis 6.0+
- MQTT Broker (EMQ X / Mosquitto)

### 部署步骤
1. 创建MySQL数据库并导入表结构
2. 修改application.yaml配置
3. 打包: `mvn clean package`
4. 运行: `java -jar drone-backend-0.0.1-SNAPSHOT.jar`

---

## 版本历史

| 版本 | 日期 | 主要更新 |
|------|------|---------|
| - | 2025/4/11 | 更新告警查询接口支持无人机序列号和设备ID |
| - | 近期 | 优化告警轨迹合并逻辑并修复坐标过滤问题 |
| - | 近期 | 修复硬件字段映射问题 |
| - | 近期 | 简化干扰频段判断逻辑，默认除1.6G外频段均开启 |
| - | 近期 | 优化设备离线检测逻辑，间隔20秒 |

---

## 项目统计

- **Java文件数**: 130个
- **DTO类数**: 42个
- **代码总行数**: 约8000+行（估算）

---

*文档生成时间: 2026/04/11*
*版本: v1.0*
