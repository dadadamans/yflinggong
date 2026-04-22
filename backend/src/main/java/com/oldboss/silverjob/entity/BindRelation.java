package com.oldboss.silverjob.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bind_relation")
public class BindRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("elderly_user_id")
    private Long elderlyUserId;

    @TableField("child_user_id")
    private Long childUserId;

    @TableField("bind_code")
    private String bindCode;

    @TableField("code_created_at")
    private LocalDateTime codeCreatedAt;

    private Boolean confirmed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;
}