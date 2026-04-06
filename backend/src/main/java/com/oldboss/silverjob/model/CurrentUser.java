package com.oldboss.silverjob.model;

import java.util.Map;

public class CurrentUser {

    private final String token;
    private final String roleType;
    private final Map<String, Object> profile;

    public CurrentUser(String token, String roleType, Map<String, Object> profile) {
        this.token = token;
        this.roleType = roleType;
        this.profile = profile;
    }

    public String getToken() {
        return token;
    }

    public String getRoleType() {
        return roleType;
    }

    public Map<String, Object> getProfile() {
        return profile;
    }
}
