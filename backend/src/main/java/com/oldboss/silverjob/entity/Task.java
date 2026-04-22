package com.oldboss.silverjob.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_type")
    private String taskType;

    private String title;

    private String address;

    @TableField("time_text")
    private String timeText;

    private Integer salary;

    private String content;

    @TableField("publisher_id")
    private Long publisherId;

    @TableField("publisher_name")
    private String publisherName;

    @TableField("publisher_role")
    private String publisherRole;

    private String status;

    @TableField("elderly_id")
    private Long elderlyId;

    @TableField("elderly_name")
    private String elderlyName;

    @TableField("employer_id")
    private Long employerId;

    @TableField("employer_name")
    private String employerName;

    @TableField("order_code")
    private String orderCode;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("finish_time")
    private LocalDateTime finishTime;

    @TableField("settle_text")
    private String settleText;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}