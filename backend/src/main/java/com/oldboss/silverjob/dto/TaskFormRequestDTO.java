package com.oldboss.silverjob.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskFormRequestDTO {
    @NotBlank(message = "请输入任务标题")
    @Size(max = 128, message = "任务标题不能超过128字")
    private String title;

    @NotBlank(message = "请选择任务类型")
    private String type;

    private String category;

    @NotBlank(message = "请输入任务地址")
    @Size(max = 255, message = "任务地址不能超过255字")
    private String address;

    @NotNull(message = "请输入任务薪资")
    @Min(value = 1, message = "任务薪资必须大于0")
    private Integer salary;

    @NotBlank(message = "请输入任务时间")
    private String timeText;

    @NotBlank(message = "请输入任务内容")
    private String content;
}
