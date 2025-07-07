package com.demo.dronebackend.dto.disposal;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteRequest<T> {
    @NotEmpty(message = "ids 不能为空")
    private List<T> ids;
}
