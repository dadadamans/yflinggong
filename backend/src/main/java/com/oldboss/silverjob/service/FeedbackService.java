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
        feedback.setFeedbackType(request.getFeedbackType() != null ? request.getFeedbackType() : "other");
        feedback.setRelatedOrderId(request.getRelatedOrderId());
        feedback.setStatus("pending");
        feedback.setCreatedAt(LocalDateTime.now());

        feedbackMapper.insert(feedback);
    }

    public List<FeedbackItemVO> getMyFeedbackList(String authorization) {
        CurrentUser currentUser = authService.requireUser(authorization);
        List<Map<String, Object>> rows = feedbackMapper.selectByUserIdWithDetail(currentUser.getUserId());
        return convertToVO(rows);
    }

    public List<FeedbackItemVO> getAllFeedbackList() {
        List<Map<String, Object>> rows = feedbackMapper.selectAllWithDetail();
        return convertToVO(rows);
    }

    @Transactional
    public void handleFeedback(String token, Long id, String status, String reply) {
        CurrentUser admin = authService.requireUser(token);
        if (!"admin".equals(admin.getRoleType())) {
            throw new BizException("无权处理反馈");
        }

        Feedback f = feedbackMapper.selectById(id);
        if (f == null) {
            throw new BizException("反馈记录不存在");
        }

        f.setStatus(status);
        f.setAdminReply(reply);
        f.setHandledBy(admin.getUserId());
        f.setUpdatedAt(LocalDateTime.now());

        feedbackMapper.updateById(f);
    }

    private List<FeedbackItemVO> convertToVO(List<Map<String, Object>> rows) {
        List<FeedbackItemVO> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            FeedbackItemVO vo = new FeedbackItemVO();
            vo.setId(toLong(r.get("id")));
            vo.setUserId(toLong(r.get("user_id")));
            vo.setUserName(r.get("user_name") != null ? r.get("user_name").toString() : "匿名用户");
            vo.setRole(r.get("role") != null ? r.get("role").toString() : "");
            vo.setContent(r.get("content") != null ? r.get("content").toString() : "");
            vo.setFeedbackType(r.get("feedback_type") != null ? r.get("feedback_type").toString() : "其他问题");
            vo.setRelatedOrderId(toLong(r.get("related_order_id")));
            vo.setOrderTitle(r.get("order_title") != null ? r.get("order_title").toString() : "无关联任务");
            vo.setStatus(r.get("status") != null ? r.get("status").toString() : "pending");
            vo.setAdminReply(r.get("admin_reply") != null ? r.get("admin_reply").toString() : null);
            vo.setHandledBy(toLong(r.get("handled_by")));
            vo.setCreatedAt(toDateTime(r.get("created_at")));
            vo.setUpdatedAt(toDateTime(r.get("updated_at")));
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

    private LocalDateTime toDateTime(Object value) {
    if (value == null) return null;
    if (value instanceof LocalDateTime) return (LocalDateTime) value;
    if (value instanceof java.sql.Timestamp) {
        return ((java.sql.Timestamp) value).toLocalDateTime();
    }
    return null;
}
}