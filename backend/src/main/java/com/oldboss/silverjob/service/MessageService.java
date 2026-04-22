package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.mapper.BindRelationMapper;
import com.oldboss.silverjob.mapper.MessageMapper;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.dto.MessageSendRequestDTO;
import com.oldboss.silverjob.vo.MessageItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息服务
 * 处理老人与子女之间的留言功能
 */
@Service
public class MessageService {

    private static final DateTimeFormatter MESSAGE_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_CONTENT_LENGTH = 500;

    private final MessageMapper messageMapper;
    private final BindRelationMapper bindRelationMapper;
    private final AuthService authService;

    public MessageService(MessageMapper messageMapper, BindRelationMapper bindRelationMapper, AuthService authService) {
        this.messageMapper = messageMapper;
        this.bindRelationMapper = bindRelationMapper;
        this.authService = authService;
    }

    /**
     * 获取留言列表
     * @param authorization Authorization 请求头
     * @return 留言列表
     */
    public List<MessageItemVO> messageList(String authorization) {
        CurrentUser currentUser = authService.requireUser(authorization);
        String role = currentUser.getRoleType();

        if (!"elderly".equals(role) && !"child".equals(role)) {
            throw new BizException("当前身份不能使用留言功能");
        }

        Map<String, Object> relation = getRequiredRelation(currentUser);
        if (relation == null) {
            return List.of();
        }
        Long bindRelationId = ((Number) relation.get("id")).longValue();
        return messageMapper.selectMessagesByBindRelationId(bindRelationId).stream()
                .map(this::toMessageItemVO)
                .toList();
    }

    /**
     * 发送留言
     * @param authorization Authorization 请求头
     * @param request 留言内容
     */
    @Transactional
    public void sendMessage(String authorization, MessageSendRequestDTO request) {
        CurrentUser currentUser = authService.requireUser(authorization);
        String role = currentUser.getRoleType();

        if (!"elderly".equals(role) && !"child".equals(role)) {
            throw new BizException("当前身份不能使用留言功能");
        }

        String content = safe(request.getContent());
        if (content.isEmpty()) {
            throw new BizException("请输入留言内容");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BizException("留言内容过长，请精简后再发");
        }

        Map<String, Object> relation = getRequiredRelation(currentUser);
        if (relation == null) {
            throw new BizException("尚未绑定，无法使用留言功能");
        }

        Boolean confirmed = Boolean.TRUE.equals(relation.get("confirmed"));
        if (!confirmed) {
            throw new BizException("绑定关系尚未确认，无法使用留言功能");
        }

        Long bindRelationId = ((Number) relation.get("id")).longValue();
        long userId = currentUser.getUserId();
        messageMapper.insertMessage(bindRelationId, userId, role, content);
    }

    /**
     * 标记留言为已读
     * @param authorization Authorization 请求头
     * @param messageId 留言ID
     */
    @Transactional
    public void markAsRead(String authorization, Long messageId) {
        CurrentUser currentUser = authService.requireUser(authorization);
        String role = currentUser.getRoleType();

        if (!"elderly".equals(role) && !"child".equals(role)) {
            return;
        }

        messageMapper.markAsRead(messageId);
    }

    private Map<String, Object> getRequiredRelation(CurrentUser user) {
        long userId = user.getUserId();
        String role = user.getRoleType();

        if ("elderly".equals(role)) {
            return bindRelationMapper.selectByElderlyId(userId);
        } else if ("child".equals(role)) {
            return bindRelationMapper.selectByChildId(userId);
        }
        return null;
    }

    private String roleText(String role) {
        if ("elderly".equals(role)) return "老人";
        if ("child".equals(role)) return "子女";
        if ("employer".equals(role)) return "雇主";
        if ("admin".equals(role)) return "管理员";
        return role;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private MessageItemVO toMessageItemVO(Map<String, Object> row) {
        MessageItemVO vo = new MessageItemVO();
        vo.setId(toLong(row.get("id")));
        vo.setSenderUserId(toLong(row.get("sender_user_id")));
        vo.setSenderRole(strOrNull(row.get("sender_role")));
        vo.setSenderName(strOrNull(row.get("sender_name")));
        vo.setContent(strOrNull(row.get("content")));
        vo.setIsRead(toBoolean(row.get("is_read")));
        vo.setSentAt(row.get("sent_at"));
        return vo;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String strOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
