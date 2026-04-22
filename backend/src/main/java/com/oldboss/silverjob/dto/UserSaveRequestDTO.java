package com.oldboss.silverjob.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserSaveRequestDTO {
    @Size(max = 64, message = "昵称长度不能超过64位")
    private String nickname;
    @Size(max = 64, message = "真实姓名长度不能超过64位")
    private String realName;
    @Size(max = 64, message = "姓名长度不能超过64位")
    private String name;
    @Size(max = 32, message = "手机号长度不能超过32位")
    private String mobile;
    @Pattern(regexp = "^$|^\\d{1,3}$", message = "年龄格式不正确")
    private String age;
    @Size(max = 8, message = "性别长度不能超过8位")
    private String gender;
    @Size(max = 64, message = "城市长度不能超过64位")
    private String city;
    @Size(max = 256, message = "健康描述长度不能超过256位")
    private String healthDesc;
    @Size(max = 256, message = "技能标签长度不能超过256位")
    private String skillTags;
    @Size(max = 64, message = "紧急联系人长度不能超过64位")
    private String emergencyContact;
    @Size(max = 32, message = "紧急联系电话长度不能超过32位")
    private String emergencyMobile;
    @Size(max = 32, message = "关系长度不能超过32位")
    private String relation;
    @Size(max = 256, message = "备注长度不能超过256位")
    private String remark;
    @Size(max = 256, message = "说明长度不能超过256位")
    private String note;
    @Size(max = 16, message = "字体大小长度不能超过16位")
    private String fontSize;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getHealthDesc() {
        return healthDesc;
    }

    public void setHealthDesc(String healthDesc) {
        this.healthDesc = healthDesc;
    }

    public String getSkillTags() {
        return skillTags;
    }

    public void setSkillTags(String skillTags) {
        this.skillTags = skillTags;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getEmergencyMobile() {
        return emergencyMobile;
    }

    public void setEmergencyMobile(String emergencyMobile) {
        this.emergencyMobile = emergencyMobile;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }
}
