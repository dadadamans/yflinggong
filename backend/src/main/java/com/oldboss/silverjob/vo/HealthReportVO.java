package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class HealthReportVO {
    private Boolean hasReport;
    private String healthReportUrl;
    private String healthReportStatus;
    private Object healthReportTime;
}
