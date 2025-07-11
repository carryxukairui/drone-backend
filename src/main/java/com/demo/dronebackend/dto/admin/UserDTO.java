package com.demo.dronebackend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;

    private String name;

    private String sex;

    private String phone;

    private String permission;

    private String organization;

}
