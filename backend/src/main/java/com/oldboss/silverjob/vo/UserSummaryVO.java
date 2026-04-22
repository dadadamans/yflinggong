package com.oldboss.silverjob.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserSummaryVO {
    private Long id;
    private String username;
    private String roleType;
    private String nickname;
    private String mobile;
    private String realName;
    private String relation;
    private String healthReportUrl;
    private String healthReportStatus;
    private Object healthReportTime;
    private Boolean enabled;
}
