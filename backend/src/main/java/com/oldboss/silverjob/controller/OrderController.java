package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.ApiResponse;
import com.oldboss.silverjob.payload.IdRequest;
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
@RequestMapping("/api/order")
public class OrderController {

    private final DemoStateService demoStateService;

    public OrderController(DemoStateService demoStateService) {
        this.demoStateService = demoStateService;
    }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(demoStateService.orderList(authorization));
    }

    @PostMapping("/finish")
    public ApiResponse<Map<String, Object>> finish(@RequestHeader("Authorization") String authorization,
                                                   @RequestBody IdRequest request) {
        demoStateService.finishOrder(authorization, request.getTaskId());
        return ApiResponse.ok(Collections.emptyMap(), "任务已完成");
    }

    @PostMapping("/cancel")
    public ApiResponse<Map<String, Object>> cancel(@RequestHeader("Authorization") String authorization,
                                                   @RequestBody IdRequest request) {
        demoStateService.cancelOrder(authorization, request.getTaskId());
        return ApiResponse.ok(Collections.emptyMap(), "订单已取消");
    }
}
