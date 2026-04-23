package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.mapper.BindRelationMapper;
import com.oldboss.silverjob.mapper.MessageMapper;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.dto.MessageSendRequestDTO;
import com.oldboss.silverjob.vo.MessageItemVO;
import com.oldboss.silverjob.websocket.NativeWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class MessageService {

    private static final DateTimeFormatter MESSAGE_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_CONTENT_LENGTH = 500;

    private final MessageMapper messageMapper;
    private final BindRelationMapper bindRelationMapper;
    private final AuthService authService;
    private final NativeWebSocketHandler webSocketHandler;

    /**
     * 构造留言服务并注入所需依赖。
     * @param messageMapper 留言数据访问对象
     * @param bindRelationMapper 绑定关系数据访问对象
     * @param authService 认证服务
     * @param webSocketHandler WebSocket 处理器
     */
    public MessageService(MessageMapper messageMapper, BindRelationMapper bindRelationMapper, AuthService authService, NativeWebSocketHandler webSocketHandler) {
        this.messageMapper = messageMapper;
        this.bindRelationMapper = bindRelationMapper;
        this.authService = authService;
        this.webSocketHandler = webSocketHandler;
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
            log.info("当前用户不能使用留言功能");
            throw new BizException("当前身份不能使用留言功能");
        }

        Map<String, Object> relation = getRequiredRelation(currentUser);
        if (relation == null) {
            log.info("留言为空");
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
     * @param request 留言请求
     */
    @Transactional
    public void sendMessage(String authorization, MessageSendRequestDTO request) {
        CurrentUser currentUser = authService.requireUser(authorization);
        String role = currentUser.getRoleType();

        if (!"elderly".equals(role) && !"child".equals(role)) {
            log.info("当前用户不能使用留言功能");
            throw new BizException("当前身份不能使用留言功能");
        }

        String content = safe(request.getContent());
        if (content.isEmpty()) {
            log.info("留言为空");
            throw new BizException("请输入留言内容");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            log.info("留言内容过长");
            throw new BizException("留言内容过长，请精简后再发");
        }

        Map<String, Object> relation = getRequiredRelation(currentUser);
        if (relation == null) {
            log.info("当前用户未绑定");
            throw new BizException("尚未绑定，无法使用留言功能");
        }

        Boolean confirmed = Boolean.TRUE.equals(relation.get("confirmed"));
        if (!confirmed) {
            log.info("当前用户绑定关系未确认");
            throw new BizException("绑定关系尚未确认，无法使用留言功能");
        }

        Long bindRelationId = ((Number) relation.get("id")).longValue();
        long userId = currentUser.getUserId();
        messageMapper.insertMessage(bindRelationId, userId, role, content);

        String recipientRole = "elderly".equals(role) ? "child" : "elderly";
        Long recipientUserId = getRecipientUserId(bindRelationId, recipientRole);
        log.info("SendMessage: sender={}, recipientRole={}, recipientUserId={}", userId, recipientRole, recipientUserId);
        if (recipientUserId != null) {
            Map<String, Object> pushMessage = new LinkedHashMap<>();
            pushMessage.put("type", "new_message");
            pushMessage.put("bindRelationId", bindRelationId);
            log.info("SendMessage: calling sendMessageToUser");
            webSocketHandler.sendMessageToUser(recipientUserId, "/queue/message", pushMessage);
            log.info("SendMessage: push completed");
        } else {
            log.info("SendMessage: no recipient found");
        }
    }

    /**
     * 将指定留言标记为已读。
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

    /**
     * 根据当前用户角色获取其对应的绑定关系记录。
     * @param user 当前登录用户
     * @return 绑定关系记录
     */
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

    /**
     * 将角色编码转换为中文名称。
     * @param role 角色编码
     * @return 中文名称
     */
    private String roleText(String role) {
        if ("elderly".equals(role)) return "老人";
        if ("child".equals(role)) return "子女";
        if ("employer".equals(role)) return "雇主";
        if ("admin".equals(role)) return "管理员";
        return role;
    }

    /**
     * 将字符串安全归一化为去首尾空格的非 null 字符串。
     * @param value 原始值
     * @return 归一化结果
     */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 将数据库查询结果转换为留言展示对象。
     * @param row 数据库记录
     * @return 留言展示对象
     */
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

    /**
     * 安全地将任意对象转换为 Long。
     * @param value 原始值
     * @return Long 值或 null
     */
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

    /**
     * 安全地将任意对象转换为 Boolean。
     * @param value 原始值
     * @return Boolean 值或 null
     */
    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 将任意对象转换为字符串，空值返回 null。
     * @param value 原始值
     * @return 字符串或 null
     */
    private String strOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 根据绑定关系ID和接收方角色获取接收方用户ID。
     * @param bindRelationId 绑定关系ID
     * @param recipientRole 接收方角色（elderly 或 child）
     * @return 接收方用户ID 或 null
     */
    private Long getRecipientUserId(Long bindRelationId, String recipientRole) {
        try {
            Map<String, Object> relation = bindRelationMapper.selectById(bindRelationId);
            if (relation == null || !Boolean.TRUE.equals(relation.get("confirmed"))) {
                return null;
            }
            if ("elderly".equals(recipientRole)) {
                Object elderlyId = relation.get("elderly_user_id");
                return elderlyId instanceof Number n ? n.longValue() : null;
            } else if ("child".equals(recipientRole)) {
                Object childId = relation.get("child_user_id");
                return childId instanceof Number n ? n.longValue() : null;
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get recipient user id: {}", e.getMessage());
            return null;
        }
    }
}
