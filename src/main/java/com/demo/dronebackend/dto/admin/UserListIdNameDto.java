package com.demo.dronebackend.dto.admin;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListIdNameDto {
    private Long id;
    private String name;
}
