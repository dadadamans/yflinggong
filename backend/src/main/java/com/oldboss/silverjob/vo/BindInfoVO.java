package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class BindInfoVO {
    private BindStatusVO bindInfo;
    private UserProfileVO elderlyProfile;
    private CurrentOrderVO currentOrder;
}
