package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class OrderItemVO {
    private Long id;
    private String orderCode;
    private String code;
    private String title;
    private String address;
    private String timeText;
    private Integer salary;
    private Double platformFee;
    private Double displaySalary;
    private String elderlyName;
    private Long publisherId;
    private String publisherName;
    private String publisherMobile;
    private String settleText;
    private String status;
    private String formattedStartTime;
    private String formattedFinishTime;
    private Boolean currentUserCommented;
}
