package com.demo.dronebackend.model;

import com.demo.dronebackend.util.ThreadPoolUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class TimingWheelDelayManager {
    private static final Logger log = LoggerFactory.getLogger(TimingWheelDelayManager.class);
    
    // 任务存储：<设备ID, 任务详情>
    private final ConcurrentHashMap<String, TimeoutTask> taskMap = new ConcurrentHashMap<>(512);
    
    // 时间轮调度器（单线程）
    private final ScheduledExecutorService wheelScheduler = Executors.newSingleThreadScheduledExecutor(
        r -> new Thread(r, "Timing-Wheel-Thread")
    );
    
    // 任务执行线程池（与时间轮分离）
    private final ExecutorService taskExecutor = ThreadPoolUtils.newBoundedCachedThreadPool(
        "Task-Worker", 
        50,  // 核心线程
        200, // 最大线程
        60,  // 空闲时间(秒)
        10000 // 队列容量
    );
    
    // 轮询间隔（可配置）
    @Value("${timing-wheel.interval:200}")
    private int wheelIntervalMs = 200;

    @PostConstruct
    public void init() {
        // 启动时间轮：动态间隔配置
        wheelScheduler.scheduleAtFixedRate(
            this::processExpiredTasks,
            0, // 立即开始
            wheelIntervalMs, 
            TimeUnit.MILLISECONDS
        );
        
        log.info("Timing wheel started with interval: {}ms", wheelIntervalMs);
    }

    /**
     * 注册/更新延时任务
     * 
     * @param key     任务唯一标识（推荐格式 "设备类型:设备ID"）
     * @param delay   延迟时间
     * @param unit    时间单位
     * @param action  到期执行的动作
     * @param replace 是否替换已有任务（默认true）
     */
    public void scheduleTask(String key, long delay, TimeUnit unit, 
                            Runnable action, boolean replace) {
        final long triggerTime = System.currentTimeMillis() + unit.toMillis(delay);
        final TimeoutTask newTask = new TimeoutTask(triggerTime, action);
        
        if (replace) {
            // 原子替换（避免任务执行期间的竞态条件）
            taskMap.compute(key, (k, oldTask) -> {
                if (oldTask != null) {
                    log.debug("Replaced task for key: {}", key);
                }
                return newTask;
            });
        } else {
            // 仅当不存在时添加
            taskMap.putIfAbsent(key, newTask);
        }
    }

    /**
     * 取消任务
     * 
     * @param key 任务标识
     * @return 是否成功取消
     */
    public boolean cancelTask(String key) {
        TimeoutTask removed = taskMap.remove(key);
        return removed != null;
    }

    /**
     * 核心处理逻辑：检测并执行到期任务
     */
    private void processExpiredTasks() {
        final long now = System.currentTimeMillis();
        final List<Map.Entry<String, TimeoutTask>> expiredEntries = new ArrayList<>(50);
        
        // 阶段1：快速收集到期任务（最小化锁持有时间）
        taskMap.entrySet().removeIf(entry -> {
            if (entry.getValue().triggerTime <= now) {
                expiredEntries.add(entry);
                return true; // 从Map中移除
            }
            return false;
        });
        
        // 阶段2：异步执行任务（不阻塞时间轮）
        for (Map.Entry<String, TimeoutTask> entry : expiredEntries) {
            taskExecutor.execute(() -> {
                try {
                    entry.getValue().action.run();
                    log.debug("Executed task for key: {}", entry.getKey());
                } catch (Exception e) {
                    log.error("Task execution failed | key={} | error={}", 
                              entry.getKey(), e.getMessage(), e);
                }
            });
        }
    }

    /**
     * 获取待处理任务数（监控用）
     */
    public int pendingTaskCount() {
        return taskMap.size();
    }

    @PreDestroy
    public void shutdown() {
        // 1. 停止接受新任务
        wheelScheduler.shutdownNow();
        
        // 2. 执行剩余任务（根据业务需求选择）
        taskMap.forEach((key, task) -> {
            if (task.triggerTime <= System.currentTimeMillis()) {
                taskExecutor.execute(task.action);
            }
        });
        
        // 3. 优雅关闭执行线程池
        ThreadPoolUtils.shutdownGracefully(taskExecutor, 5, TimeUnit.SECONDS);
        log.info("Timing wheel shutdown completed");
    }

    // 任务数据结构
    private static class TimeoutTask {
        final long triggerTime; // 触发时间戳(毫秒)
        final Runnable action;  // 执行动作
        
        TimeoutTask(long triggerTime, Runnable action) {
            this.triggerTime = triggerTime;
            this.action = action;
        }
    }
}