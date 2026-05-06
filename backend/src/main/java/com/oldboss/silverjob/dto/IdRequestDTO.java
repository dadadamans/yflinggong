package com.oldboss.silverjob.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class IdRequestDTO {
    private Long id;
    private Long taskId;
    private Long userId;
    private Boolean enabled;
    @Size(max = 32, message = "健康状态说明长度不能超过32位")
    private String healthCondition;

    @AssertTrue(message = "任务ID不能为空")
    public boolean isTaskReferencePresent() {
        return id != null || taskId != null || userId != null;
    }
}
