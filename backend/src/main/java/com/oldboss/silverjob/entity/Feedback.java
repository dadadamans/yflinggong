package com.oldboss.silverjob.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String role;

    private String content;

    @TableField("feedback_type")
    private String feedbackType;

    @TableField("related_order_id")
    private Long relatedOrderId;

    private String status;

    @TableField("admin_reply")
    private String adminReply;

    @TableField("handled_by")
    private Long handledBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}