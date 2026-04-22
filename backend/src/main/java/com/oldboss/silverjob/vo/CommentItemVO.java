package com.oldboss.silverjob.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CommentItemVO {

    private Long id;

    private Long taskId;

    private Long orderId;

    private Long reviewerId;

    private String reviewerName;

    private String reviewerRole;

    private Long revieweeId;

    private String revieweeName;

    private Integer rating;

    private String content;

    private String commentType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}