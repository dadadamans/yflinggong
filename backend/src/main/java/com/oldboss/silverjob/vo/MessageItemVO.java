package com.oldboss.silverjob.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MessageItemVO {
    private Long id;
    private Long senderUserId;
    private String senderRole;
    private String senderName;
    private String content;
    private Boolean isRead;
    private Object sentAt;
}
