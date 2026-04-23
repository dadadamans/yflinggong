package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.dto.FeedbackRequestDTO;
import com.oldboss.silverjob.entity.Feedback;
import com.oldboss.silverjob.mapper.FeedbackMapper;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.vo.FeedbackItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FeedbackService {

    private final FeedbackMapper feedbackMapper;
    private final AuthService authService;

    public FeedbackService(FeedbackMapper feedbackMapper, AuthService authService) {
        this.feedbackMapper = feedbackMapper;
        this.authService = authService;
    }

    @Transactional
    public void submitFeedback(String authorization, FeedbackRequestDTO request) {
        CurrentUser currentUser = authService.requireUser(authorization);

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BizException("反馈内容不能为空");
        }

        if (request.getContent().length() > 1000) {
            throw new BizException("反馈内容不能超过1000字");
        }

        Feedback feedback = new Feedback();
        feedback.setUserId(currentUser.getUserId());
        feedback.setRole(currentUser.getRoleType());
        feedback.setContent(request.getContent().trim());
        feedback.setRelatedOrderId(request.getRelatedOrderId());
        feedback.setStatus("pending");
        feedback.setCreatedAt(LocalDateTime.now());

        feedbackMapper.insertFeedback(feedback);
    }

    public List<FeedbackItemVO> getMyFeedbackList(String authorization) {
        CurrentUser currentUser = authService.requireUser(authorization);

        List<Map<String, Object>> rows = feedbackMapper.selectFeedbackByUserId(currentUser.getUserId());
        return convertToVO(rows);
    }

    public List<FeedbackItemVO> getAllFeedbackList() {
        List<Map<String, Object>> rows = feedbackMapper.selectAllFeedback();
        return convertToVO(rows);
    }

    @Transactional
    public void updateFeedbackStatus(Long feedbackId, String status) {
        if (feedbackId == null) {
            throw new BizException("反馈ID不能为空");
        }
        if (status == null || (!status.equals("pending") && !status.equals("resolved"))) {
            throw new BizException("状态只能是 pending 或 resolved");
        }

        int updated = feedbackMapper.updateStatus(feedbackId, status);
        if (updated == 0) {
            throw new BizException("反馈不存在");
        }
    }

    private List<FeedbackItemVO> convertToVO(List<Map<String, Object>> rows) {
        List<FeedbackItemVO> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            FeedbackItemVO vo = new FeedbackItemVO();
            vo.setId(toLong(row.get("id")));
            vo.setUserId(toLong(row.get("user_id")));
            vo.setUserName(toStr(row.get("user_name")));
            vo.setRole(toStr(row.get("role")));
            vo.setContent(toStr(row.get("content")));
            vo.setRelatedOrderId(toLong(row.get("related_order_id")));
            vo.setOrderTitle(toStr(row.get("order_title")));
            vo.setStatus(toStr(row.get("status")));
            vo.setCreatedAt(toDateTime(row.get("created_at")));
            list.add(vo);
        }
        return list;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toStr(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime toDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        return null;
    }
}