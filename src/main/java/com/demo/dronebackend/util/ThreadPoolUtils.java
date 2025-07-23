package com.demo.dronebackend.util;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolUtils {

    /**
     * 创建有界的缓存线程池
     *
     * @param threadNamePrefix 线程名称前缀
     * @param corePoolSize 核心线程数
     * @param maxPoolSize 最大线程数
     * @param keepAliveTime 空闲线程存活时间
     * @param queueCapacity 队列容量
     * @return ExecutorService
     */
    public static ExecutorService newBoundedCachedThreadPool(
            String threadNamePrefix,
            int corePoolSize,
            int maxPoolSize,
            long keepAliveTime,
            int queueCapacity) {

        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueCapacity);
        ThreadFactory threadFactory = new NamedThreadFactory(threadNamePrefix);

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy() // 可根据需要改为 CallerRunsPolicy
        );
    }

    /**
     * 优雅关闭线程池
     *
     * @param executor 要关闭的线程池
     * @param timeout  超时时间
     * @param unit     时间单位
     */
    public static void shutdownGracefully(ExecutorService executor, long timeout, TimeUnit unit) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeout, unit)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(timeout, unit)) {
                    System.err.println("线程池未能在超时时间内关闭");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 自定义线程工厂，支持线程命名
     */
    static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger count = new AtomicInteger(1);

        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + count.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    }
}
