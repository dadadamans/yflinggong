package com.oldboss.silverjob.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("bind_relation_id")
    private Long bindRelationId;

    @TableField("sender_user_id")
    private Long senderUserId;

    @TableField("sender_role")
    private String senderRole;

    private String content;

    @TableField("is_read")
    private Boolean isRead;

    @TableField("sent_at")
    private LocalDateTime sentAt;
}