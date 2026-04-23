package com.oldboss.silverjob.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackRequestDTO {

    @NotNull(message = "反馈内容不能为空")
    @Size(max = 1000, message = "反馈内容不能超过1000字")
    private String content;

    private Long relatedOrderId;

    private String feedbackType;
}