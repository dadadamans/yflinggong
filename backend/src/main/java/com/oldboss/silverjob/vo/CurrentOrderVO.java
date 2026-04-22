package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class CurrentOrderVO {
    private Long id;
    private String title;
    private String address;
    private Object startTime;
    private Integer salary;
    private String status;
}
