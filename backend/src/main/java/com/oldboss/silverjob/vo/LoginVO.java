package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private String currentRole;
    private UserProfileVO userInfo;
}
