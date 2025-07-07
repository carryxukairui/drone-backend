package com.demo.dronebackend.enums;

import lombok.Getter;

@Getter
public enum PermissionType {
    admin("admin"),
    user("user");


    private final String desc;

    private PermissionType(String desc){
        this.desc = desc;

    }
}
