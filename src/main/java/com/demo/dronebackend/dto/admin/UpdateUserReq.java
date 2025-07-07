package com.demo.dronebackend.dto.admin;


import lombok.Data;

@Data
public class UpdateUserReq {

    private String name;

    private String sex;

    private String organization;

    private String phone;

}
