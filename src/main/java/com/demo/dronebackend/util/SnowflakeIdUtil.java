package com.demo.dronebackend.util;

/**
 * 雪花算法 ID 生成器（long 类型）
 */
public class SnowflakeIdUtil {

    private static final long START_TIMESTAMP = 1700000000000L; // 可定义为项目启动时的时间戳
    private static final long MACHINE_ID_BITS = 5L;  // 机器 id 占用位数
    private static final long DATACENTER_ID_BITS = 5L;  // 数据中心 id 占用位数
    private static final long SEQUENCE_BITS = 12L; // 序列号占用位数

    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    private static final long MACHINE_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATACENTER_ID_BITS;

    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long machineId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdUtil(long datacenterId, long machineId) {
        if (machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException("机器ID超出范围");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("数据中心ID超出范围");
        }
        this.machineId = machineId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long current = System.currentTimeMillis();

        if (current < lastTimestamp) {
            throw new RuntimeException("系统时钟回拨，拒绝生成 ID");
        }

        if (current == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 当前毫秒的序列号用完，等待下一毫秒
                while ((current = System.currentTimeMillis()) <= lastTimestamp) {
                    ;
                }
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = current;

        return ((current - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_SHIFT)
                | (machineId << MACHINE_SHIFT)
                | sequence;
    }

    // 单例工厂
    public static final SnowflakeIdUtil INSTANCE = new SnowflakeIdUtil(1, 1);
}
