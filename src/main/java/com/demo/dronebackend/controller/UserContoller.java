package com.demo.dronebackend.controller;


import com.demo.dronebackend.dto.LoginRequest;
import com.demo.dronebackend.exception.BusinessException;
import com.demo.dronebackend.model.Result;
import com.demo.dronebackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class UserContoller {

    private final UserService userService;


    @PostMapping("/login-pwd")
    public Result loginByPassword( @Valid @RequestBody LoginRequest req) throws BusinessException {
        return userService.loginByPassword(req);
    }


}
