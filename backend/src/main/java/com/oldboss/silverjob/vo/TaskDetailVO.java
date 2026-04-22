package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class TaskDetailVO {
    private Long id;
    private String title;
    private String taskType;
    private String address;
    private String timeText;
    private Integer salary;
    private Double platformFee;
    private Double displaySalary;
    private String content;
    private Long publisherId;
    private String publisherName;
    private String publisherRole;
    private Double publisherAvgRating;
    private Integer publisherCommentCount;
    private Long elderlyId;
    private String elderlyName;
    private Double elderlyAvgRating;
    private Integer elderlyCommentCount;
    private String status;
    private String settleText;
    private String formattedStartTime;
    private String formattedFinishTime;
}
