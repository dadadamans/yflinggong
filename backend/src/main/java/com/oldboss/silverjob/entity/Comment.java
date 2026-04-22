package com.oldboss.silverjob.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("order_id")
    private Long orderId;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("reviewer_name")
    private String reviewerName;

    @TableField("reviewer_role")
    private String reviewerRole;

    @TableField("reviewee_id")
    private Long revieweeId;

    @TableField("reviewee_name")
    private String revieweeName;

    private Integer rating;

    private String content;

    @TableField("comment_type")
    private String commentType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
