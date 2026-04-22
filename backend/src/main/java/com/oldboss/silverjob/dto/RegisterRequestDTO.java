package com.oldboss.silverjob.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDTO {
    @NotBlank(message = "请输入账号")
    @Size(max = 64, message = "账号长度不能超过64位")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 64, message = "密码长度需为6-64位")
    private String password;

    @NotBlank(message = "请选择身份")
    private String roleType;

    @Size(max = 32, message = "手机号长度不能超过32位")
    private String mobile;

    @Size(max = 64, message = "真实姓名长度不能超过64位")
    private String realName;

    @Size(max = 64, message = "昵称长度不能超过64位")
    private String nickname;

    @Size(max = 32, message = "关系长度不能超过32位")
    private String relation;

    @Size(max = 16, message = "绑定码长度不能超过16位")
    private String bindCode;
}
