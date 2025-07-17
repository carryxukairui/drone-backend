package com.demo.dronebackend.model;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class DelayTaskManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final ConcurrentMap<String, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();
    
    /**
     * 提交延迟任务（支持任务标识）
     * @param taskKey 任务唯一标识（如无人机SN）
     * @param task 要执行的任务
     * @param delay 延迟时间
     * @param unit 时间单位
     */
    public void scheduleDelayTask(String taskKey, Runnable task, long delay, TimeUnit unit) {
        // 取消相同key的已有任务
        cancelTask(taskKey);
        
        // 提交新任务
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                task.run();
            } finally {
                taskMap.remove(taskKey);
            }
        }, delay, unit);
        
        taskMap.put(taskKey, future);
    }
    
    /**
     * 取消指定任务
     * @param taskKey 任务标识
     * @return 是否成功取消
     */
    public boolean cancelTask(String taskKey) {
        ScheduledFuture<?> future = taskMap.remove(taskKey);
        if (future != null) {
            future.cancel(false);
            return true;
        }
        return false;
    }
    
    /**
     * 关闭任务管理器
     */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
        taskMap.clear();
    }
}