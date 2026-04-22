package com.oldboss.silverjob.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("role_type")
    private String roleType;

    private String nickname;

    private String mobile;

    @TableField("real_name")
    private String realName;

    private String relation;

    private String age;

    private String gender;

    private String city;

    @TableField("health_desc")
    private String healthDesc;

    @TableField("skill_tags")
    private String skillTags;

    @TableField("emergency_contact")
    private String emergencyContact;

    @TableField("emergency_mobile")
    private String emergencyMobile;

    private String remark;

    private String note;

    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("health_report_url")
    private String healthReportUrl;

    @TableField("health_report_status")
    private String healthReportStatus;

    @TableField("health_report_time")
    private LocalDateTime healthReportTime;

    @TableField("health_condition")
    private String healthCondition;

    @TableField("font_size")
    private String fontSize;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("comment_count")
    private Integer commentCount;
}
