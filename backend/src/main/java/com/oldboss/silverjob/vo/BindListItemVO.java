package com.oldboss.silverjob.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BindListItemVO {
    private Long id;
    private String bindCode;
    private Boolean confirmed;
    private Object createdAt;
    private String elderlyNickname;
    private String elderlyRealName;
    private String elderlyMobile;
    private String childNickname;
    private String childRealName;
    private String childMobile;
    private String relation;
}
