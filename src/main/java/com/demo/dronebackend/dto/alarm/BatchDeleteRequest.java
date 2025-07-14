package com.demo.dronebackend.dto.alarm;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteRequest {

    @NotEmpty(message = "ids 不能为空")
    private List<Long> ids;
}
