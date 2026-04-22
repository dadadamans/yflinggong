package com.oldboss.silverjob.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_session")
public class UserSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String token;

    @TableField("user_id")
    private Long userId;

    @TableField("role_type")
    private String roleType;

    @TableField("expired_at")
    private LocalDateTime expiredAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}