package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.ApiResponse;
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
@RequestMapping("/api/user")
public class UserController {

    private final DemoStateService demoStateService;

    public UserController(DemoStateService demoStateService) {
        this.demoStateService = demoStateService;
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(demoStateService.userInfo(authorization));
    }

    @PostMapping("/save")
    public ApiResponse<Map<String, Object>> save(@RequestHeader("Authorization") String authorization,
                                                 @RequestBody Map<String, Object> payload) {
        demoStateService.saveUserInfo(authorization, payload);
        return ApiResponse.ok(Collections.emptyMap(), "资料已保存");
    }
}
