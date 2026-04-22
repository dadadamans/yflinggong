package com.oldboss.silverjob.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ElderlyFontSizeRequestDTO {
    @NotNull(message = "老人ID不能为空")
    private Long elderlyId;
    @NotBlank(message = "字体大小不能为空")
    @Size(max = 16, message = "字体大小长度不能超过16位")
    private String fontSize;

    public Long getElderlyId() {
        return elderlyId;
    }

    public void setElderlyId(Long elderlyId) {
        this.elderlyId = elderlyId;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }
}
