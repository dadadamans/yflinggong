package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.ApiResponse;
import com.oldboss.silverjob.payload.MessageSendRequest;
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
@RequestMapping("/api/message")
public class MessageController {

    private final DemoStateService demoStateService;

    public MessageController(DemoStateService demoStateService) {
        this.demoStateService = demoStateService;
    }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(demoStateService.messageList(authorization));
    }

    @PostMapping("/send")
    public ApiResponse<Map<String, Object>> send(@RequestHeader("Authorization") String authorization,
                                                 @RequestBody MessageSendRequest request) {
        demoStateService.sendMessage(authorization, request);
        return ApiResponse.ok(Collections.emptyMap(), "留言已发送");
    }
}
