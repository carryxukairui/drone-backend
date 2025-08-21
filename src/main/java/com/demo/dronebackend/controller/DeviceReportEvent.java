package com.demo.dronebackend.controller;

public class DeviceReportEvent {
    private final Object source;
    private final String payload;
    public DeviceReportEvent(Object source, String payload) {
        this.source = source; this.payload = payload;
    }
    public String getPayload() { return payload; }
}
