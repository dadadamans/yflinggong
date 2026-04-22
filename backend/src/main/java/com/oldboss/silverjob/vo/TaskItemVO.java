package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class TaskItemVO {
    private Long id;
    private String title;
    private String taskType;
    private String address;
    private String timeText;
    private Integer salary;
    private Double platformFee;
    private Double displaySalary;
    private String content;
    private String publisherName;
    private Long publisherId;
    private Double publisherAvgRating;
    private Integer publisherCommentCount;
    private String elderlyName;
    private Long elderlyId;
    private Double elderlyAvgRating;
    private Integer elderlyCommentCount;
    private String elderlyMobile;
    private String elderlyHealthCondition;
    private String settleText;
    private String status;
    private String formattedStartTime;
    private String formattedFinishTime;
    private Boolean currentUserCommented;
}
