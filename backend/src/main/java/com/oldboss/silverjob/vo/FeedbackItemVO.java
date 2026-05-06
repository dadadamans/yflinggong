package com.oldboss.silverjob.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackItemVO {

    private Long id;

    private Long userId;

    private String userName;

    private String role;

    private String content;

    private String feedbackType;

    private Long relatedOrderId;

    private String orderTitle;

    private String status;

    private String adminReply;

    private Long handledBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}