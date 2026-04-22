package com.oldboss.silverjob.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequestDTO {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "被评价人不能为空")
    private Long revieweeId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分必须是1-5星")
    @Max(value = 5, message = "评分必须是1-5星")
    private Integer rating;

    @Size(max = 500, message = "评价内容不能超过500字")
    private String content;

    @Size(max = 32, message = "评价类型长度不能超过32位")
    private String commentType;
}
