package com.oldboss.silverjob.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BindConfirmRequestDTO {
    @NotBlank(message = "请输入绑定码")
    @Size(max = 16, message = "绑定码长度不能超过16位")
    private String code;
}
