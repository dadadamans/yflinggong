package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class CurrentUserVO {
    private String currentRole;
    private String roleType;
    private UserProfileVO userInfo;
}
