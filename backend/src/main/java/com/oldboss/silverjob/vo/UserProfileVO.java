package com.oldboss.silverjob.vo;

import lombok.Data;

@Data
public class UserProfileVO {
    private Long id;
    private String nickname;
    private String mobile;
    private String name;
    private String realName;
    private Object age;
    private String gender;
    private String city;
    private String healthDesc;
    private String skillTags;
    private String emergencyContact;
    private String emergencyMobile;
    private String relation;
    private String note;
    private String remark;
    private String fontSize;
    private Integer totalScore;
    private Integer commentCount;
    private Double avgRating;
}
