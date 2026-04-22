package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class BindStatusVO {
    private String code;
    private Boolean confirmed;
    private String elderlyName;
    private Long elderlyId;
    private String healthReportStatus;
    private String childName;
    private String childRelation;
}
