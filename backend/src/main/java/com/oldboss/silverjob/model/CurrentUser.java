package com.oldboss.silverjob.model;

import java.util.Map;

/**
 * 当前登录用户的服务层对象。
 * 这里不是数据库实体，而是从 token 解析出来后给业务层使用的一份简化数据。
 */
public class CurrentUser {

    private final String token;
    private final String roleType;
    private final Map<String, Object> profile;
    private final Long userId;

    public CurrentUser(String token, String roleType, Map<String, Object> profile) {
        this.token = token;
        this.roleType = roleType;
        this.profile = profile;
        this.userId = profile != null && profile.get("id") != null ? ((Number) profile.get("id")).longValue() : null;
    }

    /**
     * 当前请求携带的 token。
     */
    public String getToken() {
        return token;
    }

    /**
     * 当前登录用户的角色，例如 elderly、child、employer、admin。
     */
    public String getRoleType() {
        return roleType;
    }

    /**
     * 当前角色对应的资料快照。
     */
    public Map<String, Object> getProfile() {
        return profile;
    }

    /**
     * 当前登录用户的ID。
     */
    public Long getUserId() {
        return userId;
    }
}
