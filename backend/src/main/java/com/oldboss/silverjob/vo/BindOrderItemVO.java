package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class BindOrderItemVO {
    private Long id;
    private String code;
    private String title;
    private String address;
    private String timeText;
    private String elderlyName;
    private Integer salary;
    private Double platformFee;
    private Double displaySalary;
    private String settleText;
    private String status;
}
