package com.demo.dronebackend.dto.screen;


import lombok.Data;

@Data
public class SystemLogDTO {
    private String operationType;
    private String createdTime;
    private String description;
}
