package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.Result;
import com.oldboss.silverjob.dto.FeedbackRequestDTO;
import com.oldboss.silverjob.service.FeedbackService;
import com.oldboss.silverjob.vo.FeedbackItemVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public Result<Void> submit(@RequestHeader("Authorization") String authorization,
                              @Valid @RequestBody FeedbackRequestDTO request) {
        log.info("提交反馈");
        feedbackService.submitFeedback(authorization, request);
        return Result.success(null, "反馈已提交");
    }

    @GetMapping
    public Result<List<FeedbackItemVO>> myList(@RequestHeader("Authorization") String authorization) {
        log.info("获取我的反馈列表");
        return Result.success(feedbackService.getMyFeedbackList(authorization));
    }

    @GetMapping("/admin")
    public Result<List<FeedbackItemVO>> adminList(@RequestHeader("Authorization") String authorization) {
        log.info("管理员获取所有反馈");
        return Result.success(feedbackService.getAllFeedbackList());
    }

    @PutMapping("/{id}")
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                     @RequestBody Map<String, String> body) {
        log.info("更新反馈状态: {}", id);
        String status = body.get("status");
        feedbackService.updateFeedbackStatus(id, status);
        return Result.success(null, "状态已更新");
    }
}