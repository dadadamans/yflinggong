package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class UserInfoVO {
    private String role;
    private String roleText;
    private UserProfileVO profile;
}
