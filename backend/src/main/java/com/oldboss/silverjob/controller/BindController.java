package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.ApiResponse;
import com.oldboss.silverjob.payload.BindConfirmRequest;
import com.oldboss.silverjob.service.DemoStateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bind")
public class BindController {

    private final DemoStateService demoStateService;

    public BindController(DemoStateService demoStateService) {
        this.demoStateService = demoStateService;
    }

    @PostMapping("/createCode")
    public ApiResponse<Map<String, Object>> createCode(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(demoStateService.createBindCode(authorization), "已生成新绑定码");
    }

    @PostMapping("/confirm")
    public ApiResponse<Map<String, Object>> confirm(@RequestHeader("Authorization") String authorization,
                                                    @RequestBody BindConfirmRequest request) {
        demoStateService.confirmBind(authorization, request);
        return ApiResponse.ok(Collections.emptyMap(), "绑定成功");
    }

    @GetMapping("/elderlyInfo")
    public ApiResponse<Map<String, Object>> elderlyInfo(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(demoStateService.bindInfo(authorization));
    }

    @GetMapping("/orderList")
    public ApiResponse<List<Map<String, Object>>> orderList(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(demoStateService.bindOrderList(authorization));
    }
}
