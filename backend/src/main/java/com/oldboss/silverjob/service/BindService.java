package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.common.PageQuery;
import com.oldboss.silverjob.common.PageResult;
import com.oldboss.silverjob.common.PageUtils;
import com.oldboss.silverjob.common.TaskStatus;
import com.oldboss.silverjob.mapper.BindRelationMapper;
import com.oldboss.silverjob.mapper.TaskMapper;
import com.oldboss.silverjob.mapper.UserMapper;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.dto.BindConfirmRequestDTO;
import com.oldboss.silverjob.vo.BindInfoVO;
import com.oldboss.silverjob.vo.BindListItemVO;
import com.oldboss.silverjob.vo.BindOrderItemVO;
import com.oldboss.silverjob.vo.BindStatusVO;
import com.oldboss.silverjob.vo.CurrentOrderVO;
import com.oldboss.silverjob.vo.UserProfileVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 绑定服务
 * 处理老人与子女的绑定关系、绑定码生成、确认绑定等业务逻辑
 */
@Service
public class BindService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_EXPIRE_MINUTES = 30;

    private final BindRelationMapper bindRelationMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final UserService userService;

    public BindService(BindRelationMapper bindRelationMapper, TaskMapper taskMapper, UserMapper userMapper, AuthService authService, UserService userService) {
        this.bindRelationMapper = bindRelationMapper;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * 老人生成绑定码
     * @param authorization Authorization 请求头
     * @return 绑定码信息
     */
    @Transactional
    public BindStatusVO createBindCode(String authorization) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!"elderly".equals(currentUser.getRoleType())) {
            throw new BizException("只有老人用户可以生成绑定码");
        }

        if (!userService.isHealthReportApproved(currentUser.getUserId())) {
            throw new BizException("您的体检报告还未通过审核，无法生成绑定码");
        }

        long elderlyId = currentUser.getUserId();
        Map<String, Object> relation = bindRelationMapper.selectByElderlyId(elderlyId);

        if (relation != null && Boolean.TRUE.equals(relation.get("confirmed"))) {
            throw new BizException("您已绑定子女，无法重复绑定");
        }

        String code = String.format("%06d", RANDOM.nextInt(900000) + 100000);

        if (relation == null) {
            bindRelationMapper.insertBindRelation(elderlyId, code);
        } else {
            bindRelationMapper.updateBindCode(elderlyId, code);
        }
        return toBindStatusVO(bindInfoResult(elderlyId, "elderly"));
    }

    /**
     * 子女确认绑定
     * @param authorization Authorization 请求头
     * @param request 绑定确认请求
     */
    @Transactional
    public void confirmBind(String authorization, BindConfirmRequestDTO request) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!"child".equals(currentUser.getRoleType())) {
            throw new BizException("只有子女用户可以确认绑定");
        }
        String code = safe(request.getCode());
        if (code.isEmpty()) {
            throw new BizException("请输入绑定码");
        }

        Map<String, Object> relation = bindRelationMapper.selectByBindCode(code);
        if (relation == null) {
            throw new BizException("绑定码不正确");
        }

        if (isCodeExpired(relation.get("code_created_at"))) {
            throw new BizException("绑定码已过期，请重新获取");
        }

        Boolean confirmed = Boolean.TRUE.equals(relation.get("confirmed"));
        if (confirmed) {
            throw new BizException("该绑定码已被使用");
        }

        long childId = currentUser.getUserId();

        Map<String, Object> existingRelation = bindRelationMapper.selectByChildId(childId);
        if (existingRelation != null && Boolean.TRUE.equals(existingRelation.get("confirmed"))) {
            throw new BizException("您已绑定其他老人，无法重复绑定");
        }

        Long elderlyUserId = relation.get("elderly_user_id") != null
            ? ((Number) relation.get("elderly_user_id")).longValue() : null;
        if (elderlyUserId != null) {
            Map<String, Object> elderlyRelation = bindRelationMapper.selectByElderlyId(elderlyUserId);
            if (elderlyRelation != null && Boolean.TRUE.equals(elderlyRelation.get("confirmed"))) {
                throw new BizException("该老人已绑定其他子女，无法重复绑定");
            }
        }

        int updated = bindRelationMapper.confirmBind(childId, code);
        if (updated == 0) {
            throw new BizException("绑定失败，请重试");
        }
    }

    /**
     * 解除绑定关系
     * @param authorization Authorization 请求头
     */
    @Transactional
    public void unbind(String authorization) {
        authService.requireUser(authorization);
        throw new BizException("当前版本暂不支持解绑功能");
    }

    public BindInfoVO bindInfo(String authorization) {
        CurrentUser currentUser = authService.requireUser(authorization);
        BindInfoVO result = new BindInfoVO();

        long userId = currentUser.getUserId();
        String role = currentUser.getRoleType();

        result.setBindInfo(toBindStatusVO(bindInfoResult(userId, role)));

        if ("elderly".equals(role)) {
            Map<String, Object> elderly = userMapper.selectUserById(userId);
            result.setElderlyProfile(elderly == null ? null : toUserProfileVO(toProfile(elderly)));
            result.setCurrentOrder(toCurrentOrderVO(findCurrentWorkingOrderByElderlyId(userId)));
        } else if ("child".equals(role)) {
            Map<String, Object> relation = bindRelationMapper.selectByChildId(userId);
            if (relation != null) {
                Long elderlyId = ((Number) relation.get("elderly_user_id")).longValue();
                Map<String, Object> elderly = userMapper.selectUserById(elderlyId);
                result.setElderlyProfile(elderly == null ? null : toUserProfileVO(toProfile(elderly)));
                result.setCurrentOrder(toCurrentOrderVO(findCurrentWorkingOrderByElderlyId(elderlyId)));
            }
        }

        return result;
    }

    public List<BindOrderItemVO> bindOrderList(String authorization) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!"child".equals(currentUser.getRoleType())) {
            throw new BizException("当前身份不能查看绑定订单");
        }

        long userId = currentUser.getUserId();
        String role = currentUser.getRoleType();
        Map<String, Object> relation = bindRelationMapper.selectByChildId(userId);
        if (relation == null) {
            return List.of();
        }
        Long elderlyId = ((Number) relation.get("elderly_user_id")).longValue();
        return taskMapper.selectOrdersByElderlyId(elderlyId).stream()
                .map(row -> {
                    Integer salary = (Integer) row.get("salary");
                    String taskType = (String) row.get("task_type");
                    double platformFee = calculatePlatformFee(salary, taskType);
                    double displaySalary = calculateDisplaySalary(salary, taskType, role);
                    row.put("platformFee", platformFee);
                    row.put("displaySalary", displaySalary);
                    return row;
                })
                .map(this::toBindOrderItemVO)
                .toList();
    }

    public PageResult<BindOrderItemVO> bindOrderList(String authorization, Integer page, Integer pageSize) {
        return bindOrderList(authorization, page, pageSize, null);
    }

    public PageResult<BindOrderItemVO> bindOrderList(String authorization, Integer page, Integer pageSize, String status) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!"child".equals(currentUser.getRoleType())) {
            throw new BizException("当前身份不能查看绑定订单");
        }

        Map<String, Object> relation = bindRelationMapper.selectByChildId(currentUser.getUserId());
        if (relation == null) {
            PageQuery pageQuery = PageUtils.normalize(page, pageSize);
            return PageUtils.buildPageResult(List.of(), 0, pageQuery);
        }

        Long elderlyId = ((Number) relation.get("elderly_user_id")).longValue();
        String role = currentUser.getRoleType();
        PageQuery pageQuery = PageUtils.normalize(page, pageSize);
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim();
        List<BindOrderItemVO> list = (normalizedStatus == null
                ? taskMapper.selectOrdersByElderlyIdPage(elderlyId, pageQuery.getOffset(), pageQuery.getPageSize())
                : taskMapper.selectOrdersByElderlyIdPageAndStatus(elderlyId, normalizedStatus, pageQuery.getOffset(), pageQuery.getPageSize()))
                .stream()
                .map(row -> {
                    Integer salary = (Integer) row.get("salary");
                    String taskType = (String) row.get("task_type");
                    double platformFee = calculatePlatformFee(salary, taskType);
                    double displaySalary = calculateDisplaySalary(salary, taskType, role);
                    row.put("platformFee", platformFee);
                    row.put("displaySalary", displaySalary);
                    return row;
                })
                .map(this::toBindOrderItemVO)
                .toList();
        long total = normalizedStatus == null
                ? taskMapper.countOrdersByElderlyId(elderlyId)
                : taskMapper.countOrdersByElderlyIdAndStatus(elderlyId, normalizedStatus);
        return PageUtils.buildPageResult(list, total, pageQuery);
    }

    public List<BindListItemVO> listAllBinds(String authorization) {
        authService.requireAdmin(authorization);
        return bindRelationMapper.selectAllBinds().stream()
                .map(this::toBindListItemVO)
                .toList();
    }

    public PageResult<BindListItemVO> listAllBinds(String authorization, Integer page, Integer pageSize) {
        authService.requireAdmin(authorization);
        PageQuery pageQuery = PageUtils.normalize(page, pageSize);
        List<BindListItemVO> list = bindRelationMapper.selectAllBindsPage(pageQuery.getOffset(), pageQuery.getPageSize()).stream()
                .map(this::toBindListItemVO)
                .toList();
        long total = bindRelationMapper.countAllBinds();
        return PageUtils.buildPageResult(list, total, pageQuery);
    }

    private Map<String, Object> bindInfoResult(Long userId, String role) {
        Map<String, Object> result = new LinkedHashMap<>();

        if ("child".equals(role)) {
            Map<String, Object> relation = bindRelationMapper.selectByChildId(userId);
            if (relation != null) {
                Long elderlyId = ((Number) relation.get("elderly_user_id")).longValue();
                Map<String, Object> elderly = userMapper.selectUserById(elderlyId);
                Map<String, Object> child = userMapper.selectUserById(userId);
                result.put("confirmed", "true".equals(String.valueOf(relation.get("confirmed"))));
                result.put("elderlyName", elderly == null ? "" : displayName(elderly));
                result.put("elderlyId", elderlyId);
                result.put("healthReportStatus", elderly == null ? "" : str(elderly.get("health_report_status")));
                result.put("childName", child == null ? "" : displayName(child));
                result.put("childRelation", child == null ? "" : str(child.get("relation")));
            } else {
                result.put("confirmed", false);
                result.put("elderlyName", "");
                result.put("elderlyId", null);
                result.put("healthReportStatus", "");
                result.put("childName", "");
                result.put("childRelation", "");
            }
            return result;
        }

        if ("elderly".equals(role)) {
            Map<String, Object> relation = bindRelationMapper.selectByElderlyId(userId);
            Map<String, Object> elderly = userMapper.selectUserById(userId);

            result.put("code", relation == null ? "" : relation.get("bind_code"));
            result.put("confirmed", relation != null && "true".equals(String.valueOf(relation.get("confirmed"))));
            result.put("elderlyName", elderly == null ? "" : displayName(elderly));
            result.put("elderlyId", userId);

            String healthStatus = elderly != null ? str(elderly.get("health_report_status")) : "pending";
            result.put("healthReportStatus", healthStatus);

            boolean isConfirmed = relation != null && "true".equals(String.valueOf(relation.get("confirmed")));
            if (isConfirmed) {
                Long childUserId = relation.get("child_user_id") != null ? ((Number) relation.get("child_user_id")).longValue() : null;
                if (childUserId != null) {
                    Map<String, Object> child = userMapper.selectUserById(childUserId);
                    if (child != null) {
                        result.put("childName", displayName(child));
                        result.put("childRelation", str(child.get("relation")));
                    }
                }
            } else {
                result.put("childName", "");
                result.put("childRelation", "");
            }

            return result;
        }

        return result;
    }

    private Map<String, Object> toProfile(Map<String, Object> user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.get("id"));
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

    private String primaryName(Map<String, Object> user) {
        String realName = nullToEmpty(user.get("real_name"));
        String nickname = nullToEmpty(user.get("nickname"));
        return realName.isEmpty() ? nickname : realName;
    }

    private boolean isCodeExpired(Object codeCreatedAt) {
        if (codeCreatedAt == null) {
            return true;
        }
        try {
            java.sql.Timestamp created = (java.sql.Timestamp) codeCreatedAt;
            long minutes = java.time.Duration.between(created.toInstant(), java.time.Instant.now()).toMinutes();
            return minutes > CODE_EXPIRE_MINUTES;
        } catch (Exception e) {
            return true;
        }
    }

    private Map<String, Object> findCurrentWorkingOrderByElderlyId(Long elderlyId) {
        List<Map<String, Object>> orders = taskMapper.selectOrdersByElderlyId(elderlyId);
        return orders.stream()
                .filter(o -> TaskStatus.WORKING.equals(o.get("status")))
                .findFirst()
                .orElse(null);
    }

    private static final double PLATFORM_FEE_SKILL = 0.10;
    private static final double PLATFORM_FEE_OTHER = 0.05;

    private double calculatePlatformFee(Integer salary, String taskType) {
        if (salary == null || taskType == null) return 0;
        if (taskType != null && taskType.startsWith("skill:")) {
            return salary * PLATFORM_FEE_SKILL;
        }
        return salary * PLATFORM_FEE_OTHER;
    }

    private double calculateDisplaySalary(Integer salary, String taskType, String currentRole) {
        if (salary == null) return 0;
        if ("elderly".equals(currentRole)) {
            double platformFee = calculatePlatformFee(salary, taskType);
            return salary - platformFee;
        }
        return salary;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private UserProfileVO toUserProfileVO(Map<String, Object> source) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(toLong(source.get("id")));
        vo.setNickname(strOrNull(source.get("nickname")));
        vo.setMobile(strOrNull(source.get("mobile")));
        vo.setName(strOrNull(source.get("name")));
        vo.setRealName(strOrNull(source.get("realName")));
        vo.setAge(source.get("age"));
        vo.setGender(strOrNull(source.get("gender")));
        vo.setCity(strOrNull(source.get("city")));
        vo.setHealthDesc(strOrNull(source.get("healthDesc")));
        vo.setSkillTags(strOrNull(source.get("skillTags")));
        vo.setEmergencyContact(strOrNull(source.get("emergencyContact")));
        vo.setEmergencyMobile(strOrNull(source.get("emergencyMobile")));
        vo.setRelation(strOrNull(source.get("relation")));
        vo.setNote(strOrNull(source.get("note")));
        vo.setRemark(strOrNull(source.get("remark")));
        vo.setFontSize(strOrNull(source.get("fontSize")));
        return vo;
    }

    private CurrentOrderVO toCurrentOrderVO(Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        CurrentOrderVO vo = new CurrentOrderVO();
        vo.setId(toLong(source.get("id")));
        vo.setTitle(strOrNull(source.get("title")));
        vo.setAddress(strOrNull(source.get("address")));
        vo.setStartTime(source.get("start_time"));
        vo.setSalary(toInteger(source.get("salary")));
        vo.setStatus(strOrNull(source.get("status")));
        return vo;
    }

    private BindOrderItemVO toBindOrderItemVO(Map<String, Object> source) {
        BindOrderItemVO vo = new BindOrderItemVO();
        vo.setId(toLong(source.get("id")));
        vo.setCode(strOrNull(source.get("order_code")));
        vo.setTitle(strOrNull(source.get("title")));
        vo.setAddress(strOrNull(source.get("address")));
        vo.setTimeText(strOrNull(source.get("time_text")));
        vo.setElderlyName(strOrNull(source.get("elderly_name")));
        vo.setSalary(toInteger(source.get("salary")));
        vo.setPlatformFee(toDouble(source.get("platformFee")));
        vo.setDisplaySalary(toDouble(source.get("displaySalary")));
        vo.setSettleText(strOrNull(source.get("settle_text")));
        vo.setStatus(strOrNull(source.get("status")));
        return vo;
    }

    private BindListItemVO toBindListItemVO(Map<String, Object> source) {
        BindListItemVO vo = new BindListItemVO();
        vo.setId(toLong(source.get("id")));
        vo.setBindCode(strOrNull(source.get("bind_code")));
        vo.setConfirmed(toBoolean(source.get("confirmed")));
        vo.setCreatedAt(source.get("created_at"));
        vo.setElderlyNickname(strOrNull(source.get("elderly_nickname")));
        vo.setElderlyRealName(strOrNull(source.get("elderly_real_name")));
        vo.setElderlyMobile(strOrNull(source.get("elderly_mobile")));
        vo.setChildNickname(strOrNull(source.get("child_nickname")));
        vo.setChildRealName(strOrNull(source.get("child_real_name")));
        vo.setChildMobile(strOrNull(source.get("child_mobile")));
        vo.setRelation(strOrNull(source.get("relation")));
        return vo;
    }

    private BindStatusVO toBindStatusVO(Map<String, Object> source) {
        BindStatusVO vo = new BindStatusVO();
        vo.setCode(strOrNull(source.get("code")));
        vo.setConfirmed(toBoolean(source.get("confirmed")));
        vo.setElderlyName(strOrNull(source.get("elderlyName")));
        vo.setElderlyId(toLong(source.get("elderlyId")));
        vo.setHealthReportStatus(strOrNull(source.get("healthReportStatus")));
        vo.setChildName(strOrNull(source.get("childName")));
        vo.setChildRelation(strOrNull(source.get("childRelation")));
        return vo;
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

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

    private String strOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

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

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
