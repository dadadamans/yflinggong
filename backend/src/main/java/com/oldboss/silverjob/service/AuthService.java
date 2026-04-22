package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.entity.User;
import com.oldboss.silverjob.mapper.BindRelationMapper;
import com.oldboss.silverjob.mapper.UserMapper;
import com.oldboss.silverjob.mapper.UserSessionMapper;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.dto.LoginRequestDTO;
import com.oldboss.silverjob.dto.RegisterRequestDTO;
import com.oldboss.silverjob.vo.CurrentUserVO;
import com.oldboss.silverjob.vo.LoginVO;
import com.oldboss.silverjob.vo.UserProfileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 认证服务
 * 处理用户注册、登录、退出、Token 验证等业务逻辑
 */
@Service
@Slf4j
public class AuthService {

    private static final DateTimeFormatter TOKEN_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static final int BIND_CODE_EXPIRE_MINUTES = 30;
    private static final int MIN_PASSWORD_LENGTH = 6;

    @Value("${app.auth.session-expire-hours:24}")
    private long sessionExpireHours;

    private final BindRelationMapper bindRelationMapper;
    private final UserMapper userMapper;
    private final UserSessionMapper userSessionMapper;

    /**
     * 构造认证服务并注入所需依赖。
     * @param bindRelationMapper 绑定关系数据访问对象
     * @param userMapper 用户数据访问对象
     * @param userSessionMapper 用户会话数据访问对象
     */
    public AuthService(BindRelationMapper bindRelationMapper, UserMapper userMapper, UserSessionMapper userSessionMapper) {
        this.bindRelationMapper = bindRelationMapper;
        this.userMapper = userMapper;
        this.userSessionMapper = userSessionMapper;
    }

    /**
     * 用户注册
     * @param request 注册请求参数
     * @return 登录结果，包含 token 和用户信息
     */
    @Transactional
    public LoginVO register(RegisterRequestDTO request) {
        String username = safe(request.getUsername());
        String password = safe(request.getPassword());
        String role = safe(request.getRoleType());
        String mobile = safe(request.getMobile());
        String realName = safe(request.getRealName());
        String relation = safe(request.getRelation());
        String bindCode = safe(request.getBindCode());

        if (username.isEmpty()) {
            log.info("账号为空");
            throw new BizException("请输入账号");
        }
        if (password.isEmpty()) {
            log.info("密码为空");
            throw new BizException("请输入密码");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.info("密码错误");
            throw new BizException("密码长度不能少于6位");
        }
        if (role.isEmpty()) {
            log.info("为选择身份");
            throw new BizException("请选择身份");
        }

        if (!role.equals("elderly") && !role.equals("employer") && !role.equals("child")) {
            log.info("身份类型不支持");
            throw new BizException("身份类型不支持");
        }
        if ("child".equals(role) && bindCode.isEmpty()) {
            log.info("绑定码为空");
            throw new BizException("请输入老人绑定码");
        }

        if (userMapper.selectByUsername(username) != null) {
            log.info("账号已存在");
            throw new BizException("该账号已存在");
        }

        String nickname = safe(request.getNickname());
        if (nickname.isEmpty()) {
            nickname = ("elderly".equals(role) && !realName.isEmpty()) ? realName : username;
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashPassword(password));
        user.setRoleType(role);
        user.setNickname(nickname);
        user.setMobile(emptyToNull(mobile));
        user.setRealName(emptyToNull(realName));
        user.setRelation(emptyToNull(relation));
        user.setEnabled(true);
        userMapper.insert(user);

        if ("child".equals(role)) {
            bindChildAfterRegister(user.getId(), bindCode);
        }

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        return login(loginRequest);
    }

    /**
     * 用户登录
     * @param request 登录请求参数
     * @return 登录结果，包含 token、角色和用户信息
     */
    @Transactional
    public LoginVO login(LoginRequestDTO request) {
        String username = safe(request.getUsername());
        String password = safe(request.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            log.info("账号或密码为空");
            throw new BizException("请输入账号和密码");
        }

        Map<String, Object> user = userMapper.selectByUsername(username);
        if (user == null) {
            log.info("账号或密码错误 ， 用户不存在");
            throw new BizException("账号或密码错误");
        }

        Boolean enabled = (Boolean) user.get("enabled");
        if (enabled != null && !enabled) {
            log.info("账号被禁用");
            throw new BizException("账号已被禁用，请联系管理员");
        }

        String storedPassword = str(user.get("password_hash"));
        boolean needUpgrade = false;

        if (isBcryptHash(storedPassword)) {
            if (!verifyPassword(password, storedPassword)) {
                log.info("账号或密码错误");
                throw new BizException("账号或密码错误");
            }
        } else {
            if (!password.equals(storedPassword)) {
                log.info("账号或密码错误");
                throw new BizException("账号或密码错误");
            }
            needUpgrade = true;
        }

        Long userId = ((Number) user.get("id")).longValue();
        String role = str(user.get("role_type"));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiredAt = LocalDateTime.now().plusHours(sessionExpireHours);
        userSessionMapper.insertSession(token, userId, role, expiredAt);

        if (needUpgrade) {
            userMapper.updatePassword(userId, hashPassword(password));
        }

        LoginVO result = new LoginVO();
        result.setToken(token);
        result.setCurrentRole(role);
        result.setUserInfo(toUserProfileVO(toProfile(user)));
        return result;
    }

    /**
     * 用户退出登录
     * @param authorization Authorization 请求头
     */
    @Transactional
    public void logout(String authorization) {
        String token = extractToken(authorization);
        if (!token.isEmpty()) {
            userSessionMapper.deleteSession(token);
        }
    }

    /**
     * 获取当前登录用户信息
     * @param authorization Authorization 请求头
     * @return 当前用户信息
     */
    public CurrentUserVO currentUser(String authorization) {
        CurrentUser currentUser = requireUser(authorization);
        CurrentUserVO result = new CurrentUserVO();
        result.setCurrentRole(currentUser.getRoleType());
        result.setRoleType(currentUser.getRoleType());
        result.setUserInfo(toUserProfileVO(currentUser.getProfile()));
        return result;
    }

    /**
     * 验证用户登录状态
     * @param authorization Authorization 请求头
     * @return 当前用户信息
     * @throws BizException 未登录或登录已失效
     */
    public CurrentUser requireUser(String authorization) {
        String token = extractToken(authorization);
        if (token.isEmpty()) {
            throw new BizException(401, "未登录或登录已失效");
        }

        Map<String, Object> user = userSessionMapper.selectUserByToken(token);
        if (user == null) {
            throw new BizException(401, "未登录或登录已失效");
        }
        Boolean enabled = (Boolean) user.get("enabled");
        if (enabled != null && !enabled) {
            userSessionMapper.deleteSession(token);
            throw new BizException(403, "账号已被禁用，请联系管理员");
        }
        String roleType = str(user.get("role_type"));
        Long userId = ((Number) user.get("id")).longValue();
        Map<String, Object> profileWithId = new LinkedHashMap<>(toProfile(user));
        profileWithId.put("id", userId);
        return new CurrentUser(token, roleType, profileWithId);
    }

    /**
     * 验证管理员权限
     * @param authorization Authorization 请求头
     * @return 管理员用户信息
     * @throws BizException 非管理员
     */
    public CurrentUser requireAdmin(String authorization) {
        CurrentUser user = requireUser(authorization);
        if (!"admin".equals(user.getRoleType())) {
            throw new BizException("仅管理员可操作");
        }
        return user;
    }

    /**
     * 将数据库用户记录转换为统一的前端资料结构。
     * @param user 数据库用户记录
     * @return 用户资料 Map
     */
    private Map<String, Object> toProfile(Map<String, Object> user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nickname", nullToEmpty(user.get("nickname")));
        result.put("mobile", nullToEmpty(user.get("mobile")));

        String roleType = str(user.get("role_type"));
        if ("elderly".equals(roleType)) {
            result.put("id", user.get("id"));
            result.put("realName", nullToEmpty(user.get("real_name")));
            result.put("age", user.get("age") == null ? "" : user.get("age"));
            result.put("gender", nullToEmpty(user.get("gender")));
            result.put("city", nullToEmpty(user.get("city")));
            result.put("healthDesc", nullToEmpty(user.get("health_desc")));
            result.put("skillTags", nullToEmpty(user.get("skill_tags")));
            result.put("emergencyContact", nullToEmpty(user.get("emergency_contact")));
            result.put("emergencyMobile", nullToEmpty(user.get("emergency_mobile")));
            result.put("fontSize", nullToEmpty(user.get("font_size")));
        } else {
            result.put("name", nullToEmpty(primaryName(user)));
            if ("child".equals(roleType)) {
                result.put("relation", nullToEmpty(user.get("relation")));
                result.put("note", nullToEmpty(user.get("note")));
            }
            if ("employer".equals(roleType)) {
                result.put("remark", nullToEmpty(user.get("remark")));
            }
        }
        return result;
    }

    /**
     * 将统一资料结构转换为返回前端的用户资料对象。
     * @param profile 统一资料结构
     * @return 用户资料 VO
     */
    private UserProfileVO toUserProfileVO(Map<String, Object> profile) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(toLong(profile.get("id")));
        vo.setNickname(strOrNull(profile.get("nickname")));
        vo.setMobile(strOrNull(profile.get("mobile")));
        vo.setName(strOrNull(profile.get("name")));
        vo.setRealName(strOrNull(profile.get("realName")));
        vo.setAge(profile.get("age"));
        vo.setGender(strOrNull(profile.get("gender")));
        vo.setCity(strOrNull(profile.get("city")));
        vo.setHealthDesc(strOrNull(profile.get("healthDesc")));
        vo.setSkillTags(strOrNull(profile.get("skillTags")));
        vo.setEmergencyContact(strOrNull(profile.get("emergencyContact")));
        vo.setEmergencyMobile(strOrNull(profile.get("emergencyMobile")));
        vo.setRelation(strOrNull(profile.get("relation")));
        vo.setNote(strOrNull(profile.get("note")));
        vo.setRemark(strOrNull(profile.get("remark")));
        vo.setFontSize(strOrNull(profile.get("fontSize")));
        return vo;
    }

    /**
     * 计算用户展示名称。
     * @param user 用户记录
     * @return 展示名称
     */
    private String displayName(Map<String, Object> user) {
        if (user == null) {
            return "";
        }
        String roleType = str(user.get("role_type"));
        if ("elderly".equals(roleType)) {
            String realName = nullToEmpty(user.get("real_name"));
            return realName.isEmpty() ? nullToEmpty(user.get("nickname")) : realName;
        }
        return primaryName(user);
    }

    /**
     * 获取用户主名称，优先真实姓名，其次昵称。
     * @param user 用户记录
     * @return 主名称
     */
    private String primaryName(Map<String, Object> user) {
        String realName = nullToEmpty(user.get("real_name"));
        String nickname = nullToEmpty(user.get("nickname"));
        return realName.isEmpty() ? nickname : realName;
    }

    /**
     * 从请求头中提取 token，兼容 `Bearer token` 和纯 token 两种格式。
     * @param authorization Authorization 请求头
     * @return token 字符串
     */
    private String extractToken(String authorization) {
        if (authorization == null) {
            return "";
        }
        return authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : authorization.trim();
    }

    /**
     * 将字符串安全归一化为去首尾空格的非 null 值。
     * @param value 原始值
     * @return 归一化结果
     */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 将任意对象安全转为字符串。
     * @param value 原始值
     * @return 字符串结果
     */
    private String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 将任意对象转为字符串，空值返回 null。
     * @param value 原始值
     * @return 字符串或 null
     */
    private String strOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 将空值归一化为空字符串。
     * @param value 原始值
     * @return 非空字符串
     */
    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 将空白字符串归一化为 null。
     * @param value 原始值
     * @return 归一化结果
     */
    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /**
     * 安全地将任意对象转换为 Long。
     * @param value 原始值
     * @return Long 值或 null
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 使用 BCrypt 对密码进行哈希。
     * @param password 明文密码
     * @return 哈希后的密码
     */
    private String hashPassword(String password) {
        return encoder.encode(password);
    }

    /**
     * 校验明文密码与哈希密码是否匹配。
     * @param rawPassword 明文密码
     * @param encodedPassword 哈希密码
     * @return 是否匹配
     */
    private boolean verifyPassword(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 判断数据库中的密码字段是否为 BCrypt 哈希格式。
     * @param passwordHash 密码串
     * @return 是否为 BCrypt 哈希
     */
    private boolean isBcryptHash(String passwordHash) {
        return passwordHash != null
                && passwordHash.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    }

    /**
     * 子女注册成功后，根据绑定码补全绑定关系。
     * @param childUserId 子女用户ID
     * @param bindCode 绑定码
     */
    private void bindChildAfterRegister(Long childUserId, String bindCode) {
        Map<String, Object> relation = bindRelationMapper.selectByBindCode(bindCode);
        if (relation == null) {
            throw new BizException("绑定码不正确");
        }
        if (isBindCodeExpired(relation.get("code_created_at"))) {
            throw new BizException("绑定码已过期，请重新获取");
        }
        if (Boolean.TRUE.equals(relation.get("confirmed"))) {
            throw new BizException("该绑定码已被使用");
        }

        Map<String, Object> existingRelation = bindRelationMapper.selectByChildId(childUserId);
        if (existingRelation != null && Boolean.TRUE.equals(existingRelation.get("confirmed"))) {
            throw new BizException("您已绑定其他老人，无法重复绑定");
        }

        Long elderlyUserId = relation.get("elderly_user_id") instanceof Number number
                ? number.longValue()
                : null;
        if (elderlyUserId != null) {
            Map<String, Object> elderlyRelation = bindRelationMapper.selectByElderlyId(elderlyUserId);
            if (elderlyRelation != null && Boolean.TRUE.equals(elderlyRelation.get("confirmed"))) {
                throw new BizException("该老人已绑定其他子女，无法重复绑定");
            }
        }

        int updated = bindRelationMapper.confirmBind(childUserId, bindCode);
        if (updated == 0) {
            throw new BizException("绑定失败，请重试");
        }
    }

    /**
     * 判断注册时提交的绑定码是否已过期。
     * @param codeCreatedAt 绑定码创建时间
     * @return 是否过期
     */
    private boolean isBindCodeExpired(Object codeCreatedAt) {
        if (codeCreatedAt == null) {
            return true;
        }
        try {
            java.sql.Timestamp created = (java.sql.Timestamp) codeCreatedAt;
            long minutes = java.time.Duration.between(created.toInstant(), java.time.Instant.now()).toMinutes();
            return minutes > BIND_CODE_EXPIRE_MINUTES;
        } catch (Exception e) {
            return true;
        }
    }
}
