package com.demo.dronebackend.controller;

public class DroneReportEvent {
    private final Object source;
    private final String payload;
    public DroneReportEvent(Object source, String payload) {
        this.source = source; this.payload = payload;
    }
    public String getPayload() { return payload; }
}
