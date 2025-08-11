package com.demo.dronebackend.model;

import lombok.Data;

import java.time.Instant;

@Data
public class GeoEntry {
    public enum Status { PENDING, OK, ERROR }

    private volatile double lon;
    private volatile double lat;
    private volatile String address;
    private volatile Instant lastLocationUpdate;
    private volatile Instant lastResolvedTime;
    private volatile Instant lastResolveAttempt; // 最近一次尝试解析的时间（用于限频）
    private volatile Status status;
    private volatile int retryCount;

}
