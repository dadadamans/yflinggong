package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.common.PageQuery;
import com.oldboss.silverjob.common.PageResult;
import com.oldboss.silverjob.common.PageUtils;
import com.oldboss.silverjob.dto.UserSaveRequestDTO;
import com.oldboss.silverjob.mapper.BindRelationMapper;
import com.oldboss.silverjob.mapper.TaskMapper;
import com.oldboss.silverjob.mapper.UserMapper;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.vo.HealthReportVO;
import com.oldboss.silverjob.vo.UserInfoVO;
import com.oldboss.silverjob.vo.UserProfileVO;
import com.oldboss.silverjob.vo.UserSummaryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务
 * 处理用户资料管理、启用禁用、体检报告等业务逻辑
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final BindRelationMapper bindRelationMapper;
    private final TaskMapper taskMapper;
    private final AuthService authService;

    /**
     * 构造用户服务并注入所需依赖。
     * @param userMapper 用户数据访问对象
     * @param bindRelationMapper 绑定关系数据访问对象
     * @param taskMapper 任务数据访问对象
     * @param authService 认证服务
     */
    public UserService(UserMapper userMapper, BindRelationMapper bindRelationMapper, TaskMapper taskMapper, AuthService authService) {
        this.userMapper = userMapper;
        this.bindRelationMapper = bindRelationMapper;
        this.taskMapper = taskMapper;
        this.authService = authService;
    }

    /**
     * 获取所有用户列表（管理员专用）
     * @param authorization Authorization 请求头
     * @return 所有用户列表
     */
    public List<UserSummaryVO> listAllUsers(String authorization) {
        authService.requireAdmin(authorization);
        return userMapper.selectList(null).stream().map(this::userToMap).toList();
    }

    /**
     * 分页获取全部用户列表（管理员专用）
     * @param authorization Authorization 请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页用户列表
     */
    public PageResult<UserSummaryVO> listAllUsers(String authorization, Integer page, Integer pageSize) {
        authService.requireAdmin(authorization);
        PageQuery pageQuery = PageUtils.normalize(page, pageSize);
        List<UserSummaryVO> list = userMapper.selectUsersPage(pageQuery.getOffset(), pageQuery.getPageSize())
                .stream()
                .map(this::userToMap)
                .toList();
        long total = userMapper.countUsers();
        return PageUtils.buildPageResult(list, total, pageQuery);
    }

    /**
     * 按条件分页获取用户列表（管理员专用）
     * @param authorization Authorization 请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @param roleType 角色类型筛选
     * @param enabled 启用状态筛选
     * @param healthStatus 体检状态筛选
     * @return 分页用户列表
     */
    public PageResult<UserSummaryVO> listAllUsers(String authorization, Integer page, Integer pageSize,
                                                  String roleType, Boolean enabled, String healthStatus) {
        authService.requireAdmin(authorization);
        PageQuery pageQuery = PageUtils.normalize(page, pageSize);
        List<UserSummaryVO> list = userMapper.selectUsersPageFiltered(pageQuery.getOffset(), pageQuery.getPageSize(),
                        emptyToNull(roleType), enabled, emptyToNull(healthStatus))
                .stream()
                .map(this::userToMap)
                .toList();
        long total = userMapper.countUsersFiltered(emptyToNull(roleType), enabled, emptyToNull(healthStatus));
        return PageUtils.buildPageResult(list, total, pageQuery);
    }

    /**
     * 获取当前登录用户资料
     * @param authorization Authorization 请求头
     * @return 用户资料，包含角色和详细信息
     */
    public UserInfoVO userInfo(String authorization) {
        CurrentUser currentUser = authService.requireUser(authorization);
        
        // 从数据库获取完整用户信息（包括评分字段）
        Map<String, Object> dbUser = userMapper.selectUserById(currentUser.getUserId());
        if (dbUser == null) {
            throw new BizException("用户不存在");
        }
        
        // 转换为基础profile格式（包含id）
        Map<String, Object> profileBase = toProfile(dbUser);
        profileBase.put("id", currentUser.getUserId());
        
        UserInfoVO result = new UserInfoVO();
        result.setRole(currentUser.getRoleType());
        result.setRoleText(roleText(currentUser.getRoleType()));
        result.setProfile(toUserProfileVO(profileBase));
        return result;
    }

    /**
     * 保存当前用户资料
     * @param authorization Authorization 请求头
     * @param payload 用户资料数据
     */
    /**
     * 保存当前登录用户资料，并在名称变化时同步任务中的展示名称。
     * @param authorization Authorization 请求头
     * @param payload 用户资料数据
     */
    @Transactional
    public void saveUserInfo(String authorization, UserSaveRequestDTO payload) {
        CurrentUser currentUser = authService.requireUser(authorization);
        Long userId = currentUser.getUserId();

        Map<String, Object> existing = userMapper.selectUserById(userId);
        if (existing == null) {
            throw new BizException("用户不存在");
        }

        String oldDisplayName = displayName(existing);
        Map<String, Object> merged = new LinkedHashMap<>(toProfile(existing));
        toPayloadMap(payload).forEach((key, value) -> {
            if (value != null) {
                merged.put(key, value);
            }
        });

        String nickname = str(merged.get("nickname"));
        String realName = resolveProfileRealName(currentUser.getRoleType(), merged, existing);
        String mobile = str(merged.get("mobile"));
        String gender = str(merged.get("gender"));
        Short age = parseShort(merged.get("age"));
        String city = str(merged.get("city"));
        String healthDesc = str(merged.get("healthDesc"));
        String skillTags = str(merged.get("skillTags"));
        String emergencyContact = str(merged.get("emergencyContact"));
        String emergencyMobile = str(merged.get("emergencyMobile"));
        String relation = str(merged.get("relation"));
        String remark = str(merged.get("remark"));
        String note = str(merged.get("note"));
        String fontSize = str(merged.get("fontSize"));

        userMapper.updateUserFull(userId, emptyToNull(nickname), emptyToNull(realName), emptyToNull(mobile),
                emptyToNull(gender), age, emptyToNull(city), emptyToNull(healthDesc), emptyToNull(skillTags),
                emptyToNull(emergencyContact), emptyToNull(emergencyMobile), emptyToNull(relation),
                emptyToNull(remark), emptyToNull(note), emptyToNull(fontSize));

        String newDisplayName = displayName(userMapper.selectUserById(userId));
        if (!oldDisplayName.equals(newDisplayName)) {
            syncDisplayName(currentUser.getRoleType(), oldDisplayName, newDisplayName);
}
    }

    /**
     * 设置老人字体大小（子女端专用）
     * @param authorization Authorization 请求头
     * @param elderlyId 老人用户ID
     * @param fontSize 字体大小
     */
    public void setElderlyFontSize(String authorization, Long elderlyId, String fontSize) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!"child".equals(currentUser.getRoleType())) {
            throw new BizException("只有子女才能设置老人字体大小");
        }

        Map<String, Object> relation = bindRelationMapper.selectByChildId(currentUser.getUserId());
        if (relation == null) {
            throw new BizException("您还未绑定老人");
        }

        Long boundElderlyId = ((Number) relation.get("elderly_user_id")).longValue();
        if (!elderlyId.equals(boundElderlyId)) {
            throw new BizException("无法修改非绑定老人的设置");
        }

        userMapper.updateFontSize(elderlyId, fontSize);
    }

    /**
     * 当用户显示名称变化后，同步任务表中的冗余名称字段。
     * @param role 用户角色
     * @param oldName 旧名称
     * @param newName 新名称
     */
    private void syncDisplayName(String role, String oldName, String newName) {
        if (oldName.equals(newName)) {
            return;
        }
        if ("elderly".equals(role)) {
            taskMapper.syncElderlyName(oldName, newName);
        }
        if ("employer".equals(role) || "child".equals(role)) {
            taskMapper.syncPublisherName(oldName, newName, role);
        }
    }

    /**
     * 将用户实体转换为用户列表展示对象。
     * @param user 用户实体
     * @return 用户摘要对象
     */
    private UserSummaryVO userToMap(com.oldboss.silverjob.entity.User user) {
        UserSummaryVO vo = new UserSummaryVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRoleType(user.getRoleType());
        vo.setNickname(user.getNickname());
        vo.setMobile(user.getMobile());
        vo.setRealName(user.getRealName());
        vo.setRelation(user.getRelation());
        vo.setHealthReportUrl(user.getHealthReportUrl());
        vo.setHealthReportStatus(user.getHealthReportStatus());
        vo.setHealthReportTime(user.getHealthReportTime());
        vo.setEnabled(user.getEnabled());
        return vo;
    }

    /**
     * 将统一资料结构转换为前端使用的用户资料对象。
     * @param profile 资料 Map
     * @return 用户资料对象
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

        Integer totalScore = toInteger(profile.get("totalScore"));
        Integer commentCount = toInteger(profile.get("commentCount"));
        vo.setTotalScore(totalScore);
        vo.setCommentCount(commentCount);
        if (commentCount != null && commentCount > 0 && totalScore != null) {
            vo.setAvgRating((double) totalScore / commentCount);
        } else {
            vo.setAvgRating(0.0);
        }

        return vo;
    }

    /**
     * 将数据库用户记录转换为统一资料结构。
     * @param user 数据库用户记录
     * @return 统一资料 Map
     */
    private Map<String, Object> toProfile(Map<String, Object> user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nickname", nullToEmpty(user.get("nickname")));
        result.put("mobile", nullToEmpty(user.get("mobile")));

        String roleType = str(user.get("role_type"));
        if ("elderly".equals(roleType)) {
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
        }
        
        // 评分相关字段
        result.put("totalScore", toInteger(user.get("total_score")));
        result.put("commentCount", toInteger(user.get("comment_count")));
        
        // 计算平均分
        Integer ts = toInteger(user.get("total_score"));
        Integer cc = toInteger(user.get("comment_count"));
        if (cc != null && cc > 0 && ts != null) {
            result.put("avgRating", (double) ts / cc);
        } else {
            result.put("avgRating", 0.0);
        }
        
        return result;
    }

    /**
     * 将角色编码转换为中文名称。
     * @param role 角色编码
     * @return 中文角色名称
     */
    private String roleText(String role) {
        if ("elderly".equals(role)) return "老人";
        if ("employer".equals(role)) return "雇主";
        if ("child".equals(role)) return "子女";
        if ("admin".equals(role)) return "管理员";
        return role;
    }

    /**
     * 计算用户在界面中使用的显示名称。
     * @param user 用户记录
     * @return 显示名称
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
     * 获取用户主显示名称，优先真实姓名，其次昵称。
     * @param user 用户记录
     * @return 主名称
     */
    private String primaryName(Map<String, Object> user) {
        String realName = nullToEmpty(user.get("real_name"));
        String nickname = nullToEmpty(user.get("nickname"));
        return realName.isEmpty() ? nickname : realName;
    }

    /**
     * 按角色规则决定资料保存时最终写入的真实姓名。
     * @param role 当前角色
     * @param merged 合并后的资料数据
     * @param existing 数据库原始数据
     * @return 真实姓名
     */
    private String resolveProfileRealName(String role, Map<String, Object> merged, Map<String, Object> existing) {
        if ("elderly".equals(role)) {
            String realName = str(merged.get("realName"));
            return realName.isEmpty() ? str(existing.get("real_name")) : realName;
        }
        String name = str(merged.get("name"));
        if (!name.isEmpty()) {
            return name;
        }
        return str(existing.get("real_name"));
    }

    /**
     * 将任意对象安全转为去首尾空格的字符串。
     * @param value 原始值
     * @return 字符串结果
     */
    private String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 将任意对象安全转为字符串，空值返回 null。
     * @param value 原始值
     * @return 字符串或 null
     */
    private String strOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 将输入值解析为年龄字段需要的 Short 类型。
     * @param value 原始值
     * @return 解析结果
     */
    private Short parseShort(Object value) {
        String text = str(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Short.valueOf(text);
        } catch (NumberFormatException ex) {
            throw new BizException("年龄格式不正确");
        }
    }

    /**
     * 将空值转换为空字符串。
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
     * 将用户保存请求对象展开为便于合并处理的 Map。
     * @param payload 保存请求
     * @return 请求字段映射
     */
    private Map<String, Object> toPayloadMap(UserSaveRequestDTO payload) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nickname", payload.getNickname());
        map.put("realName", payload.getRealName());
        map.put("name", payload.getName());
        map.put("mobile", payload.getMobile());
        map.put("age", payload.getAge());
        map.put("gender", payload.getGender());
        map.put("city", payload.getCity());
        map.put("healthDesc", payload.getHealthDesc());
        map.put("skillTags", payload.getSkillTags());
        map.put("emergencyContact", payload.getEmergencyContact());
        map.put("emergencyMobile", payload.getEmergencyMobile());
        map.put("relation", payload.getRelation());
        map.put("remark", payload.getRemark());
        map.put("note", payload.getNote());
        map.put("fontSize", payload.getFontSize());
        return map;
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
     * 安全地将任意对象转换为 Integer。
     * @param value 原始值
     * @return Integer 值或 null
     */
    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 检查用户体检报告是否已通过审核
     * @param userId 用户ID
     * @return 是否已通过审核
     */
    public boolean isHealthReportApproved(Long userId) {
        Map<String, Object> user = userMapper.selectUserById(userId);
        if (user == null) {
            return false;
        }
        String status = str(user.get("health_report_status"));
        String condition = str(user.get("health_condition"));
        return "approved".equals(status) && !condition.isEmpty();
    }

    /**
     * 保存用户体检报告URL（老人端专用）
     * @param authorization Authorization 请求头
     * @param fileUrl 体检报告文件路径
     */
    public void saveHealthReportUrl(String authorization, String fileUrl) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!"elderly".equals(currentUser.getRoleType())) {
            throw new BizException("只有老人才能上传体检报告");
        }
        userMapper.updateHealthReport(currentUser.getUserId(), fileUrl, "pending", null);
    }

    /**
     * 审核用户体检报告（管理员专用）
     * @param authorization Authorization 请求头
     * @param userId 用户ID
     * @param approved 审核状态（true为通过，false为拒绝）
     */
    public void reviewHealthReport(String authorization, Long userId, boolean approved, String healthCondition) {
        authService.requireAdmin(authorization);
        Map<String, Object> user = userMapper.selectUserById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        String status = approved ? "approved" : "rejected";
        String fileUrl = str(user.get("health_report_url"));
        if (fileUrl.isEmpty()) {
            throw new BizException("该用户没有上传体检报告");
        }
        if (approved && (healthCondition == null || healthCondition.isEmpty())) {
            throw new BizException("请选择身体状况");
        }
        userMapper.updateHealthReport(userId, fileUrl, status, approved ? healthCondition : null);
    }

    /**
     * 获取当前用户体检报告状态
     * @param authorization Authorization 请求头
     * @return 体检报告状态信息
     */
    public HealthReportVO getHealthReportStatus(String authorization) {
        CurrentUser currentUser = authService.requireUser(authorization);
        Map<String, Object> user = userMapper.selectUserById(currentUser.getUserId());
        HealthReportVO result = new HealthReportVO();
        result.setHasReport(user != null && !str(user.get("health_report_url")).isEmpty());
        result.setHealthReportStatus(user != null ? str(user.get("health_report_status")) : "");
        result.setHealthReportUrl(user != null ? str(user.get("health_report_url")) : "");
        result.setHealthReportTime(user != null ? user.get("health_report_time") : null);
        return result;
    }

    /**
     * 启用或禁用用户账号（管理员专用）
     * @param authorization Authorization 请求头
     * @param userId 用户ID
     * @param enabled 启用状态
     */
    public void setUserEnabled(String authorization, Long userId, boolean enabled) {
        authService.requireAdmin(authorization);
        Map<String, Object> user = userMapper.selectUserById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        String roleType = str(user.get("role_type"));
        if ("admin".equals(roleType)) {
            throw new BizException("无法操作管理员账号");
        }
        userMapper.setEnabled(userId, enabled);
    }
}
