package com.demo.dronebackend.enums;

import lombok.Getter;

@Getter
public enum DroneType {
    legal("legal"),
    illegal("illegal");


    private final String desc;

    private DroneType(String desc){
        this.desc = desc;
    }
}
