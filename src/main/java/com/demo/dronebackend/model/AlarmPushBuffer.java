package com.demo.dronebackend.model;

import lombok.Data;

import java.util.concurrent.ScheduledFuture;

@Data
public class AlarmPushBuffer {
    private int count = 0;
    private ScheduledFuture<?> future;
    private Long userId;
    private String droneSn;
    public int incrCount() {
        return ++this.count;
    }
}
