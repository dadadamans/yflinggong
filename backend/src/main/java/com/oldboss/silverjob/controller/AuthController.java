package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.ApiResponse;
import com.oldboss.silverjob.payload.LoginRequest;
import com.oldboss.silverjob.service.DemoStateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DemoStateService demoStateService;

    public AuthController(DemoStateService demoStateService) {
        this.demoStateService = demoStateService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(demoStateService.login(request), "登录成功");
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        demoStateService.logout(authorization);
        return ApiResponse.ok(Collections.emptyMap(), "退出成功");
    }

    @GetMapping("/currentUser")
    public ApiResponse<Map<String, Object>> currentUser(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(demoStateService.currentUser(authorization));
    }
}
