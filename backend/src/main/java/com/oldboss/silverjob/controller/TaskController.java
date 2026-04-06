package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.ApiResponse;
import com.oldboss.silverjob.payload.IdRequest;
import com.oldboss.silverjob.payload.TaskFormRequest;
import com.oldboss.silverjob.service.DemoStateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    private final DemoStateService demoStateService;

    public TaskController(DemoStateService demoStateService) {
        this.demoStateService = demoStateService;
    }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(demoStateService.taskList(authorization));
    }

    @GetMapping("/detail")
    public ApiResponse<Map<String, Object>> detail(@RequestHeader("Authorization") String authorization,
                                                   @RequestParam("id") Long id) {
        return ApiResponse.ok(demoStateService.taskDetail(authorization, id));
    }

    @PostMapping("/add")
    public ApiResponse<Map<String, Object>> add(@RequestHeader("Authorization") String authorization,
                                                @RequestBody TaskFormRequest request) {
        demoStateService.addTask(authorization, request);
        return ApiResponse.ok(Collections.emptyMap(), "任务已发布");
    }

    @PostMapping("/grab")
    public ApiResponse<Map<String, Object>> grab(@RequestHeader("Authorization") String authorization,
                                                 @RequestBody IdRequest request) {
        demoStateService.grabTask(authorization, request.getId());
        return ApiResponse.ok(Collections.emptyMap(), "接单成功");
    }
}
