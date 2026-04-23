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
import org.springframework.dao.DataIntegrityViolationException;

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
    private static final int MAX_BIND_CODE_RETRIES = 5;

    private final BindRelationMapper bindRelationMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final UserService userService;

    /**
     * 构造绑定服务并注入所需依赖。
     * @param bindRelationMapper 绑定关系数据访问对象
     * @param taskMapper 任务数据访问对象
     * @param userMapper 用户数据访问对象
     * @param authService 认证服务
     * @param userService 用户服务
     */
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
     * @return 绑定码状态信息
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

        boolean created = false;
        for (int i = 0; i < MAX_BIND_CODE_RETRIES; i++) {
            String code = String.format("%06d", RANDOM.nextInt(900000) + 100000);
            try {
                if (relation == null) {
                    bindRelationMapper.insertBindRelation(elderlyId, code);
                } else {
                    bindRelationMapper.updateBindCode(elderlyId, code);
                }
                created = true;
                break;
            } catch (DataIntegrityViolationException ignored) {
                // Bind code is unique. Retry with a new code instead of surfacing a 500.
            }
        }

        if (!created) {
            throw new BizException("绑定码生成失败，请稍后重试");
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

        try {
            int updated = bindRelationMapper.confirmBind(childId, code);
            if (updated == 0) {
                throw new BizException("绑定失败，请重试");
            }
        } catch (DataIntegrityViolationException ex) {
            throw new BizException("您已绑定其他老人，无法重复绑定");
        }
    }

    /**
     * 解除绑定关系。
     * @param authorization Authorization 请求头
     */
    @Transactional
    public void unbind(String authorization) {
        authService.requireUser(authorization);
        throw new BizException("当前版本暂不支持解绑功能");
    }

    /**
     * 获取当前用户的绑定信息、老人资料和当前进行中订单
     * @param authorization Authorization 请求头
     * @return 绑定详情
     */
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

    /**
     * 获取绑定老人相关的全部订单（子女端专用）
     * @param authorization Authorization 请求头
     * @return 订单列表
     */
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

    /**
     * 分页获取绑定老人相关订单
     * @param authorization Authorization 请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页订单结果
     */
    public PageResult<BindOrderItemVO> bindOrderList(String authorization, Integer page, Integer pageSize) {
        return bindOrderList(authorization, page, pageSize, null);
    }

    /**
     * 按状态分页获取绑定老人相关订单
     * @param authorization Authorization 请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 订单状态筛选
     * @return 分页订单结果
     */
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

    /**
     * 获取全部绑定关系（管理员专用）
     * @param authorization Authorization 请求头
     * @return 绑定关系列表
     */
    public List<BindListItemVO> listAllBinds(String authorization) {
        authService.requireAdmin(authorization);
        return bindRelationMapper.selectAllBinds().stream()
                .map(this::toBindListItemVO)
                .toList();
    }

    /**
     * 分页获取全部绑定关系（管理员专用）
     * @param authorization Authorization 请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页绑定关系列表
     */
    public PageResult<BindListItemVO> listAllBinds(String authorization, Integer page, Integer pageSize) {
        authService.requireAdmin(authorization);
        PageQuery pageQuery = PageUtils.normalize(page, pageSize);
        List<BindListItemVO> list = bindRelationMapper.selectAllBindsPage(pageQuery.getOffset(), pageQuery.getPageSize()).stream()
                .map(this::toBindListItemVO)
                .toList();
        long total = bindRelationMapper.countAllBinds();
        return PageUtils.buildPageResult(list, total, pageQuery);
    }

    /**
     * 统一组装绑定状态基础信息，分别兼容老人端与子女端。
     * @param userId 当前用户ID
     * @param role 当前角色
     * @return 绑定状态数据
     */
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

    /**
     * 将数据库用户记录转换为统一资料结构。
     * @param user 用户记录
     * @return 资料 Map
     */
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
     * 判断绑定码是否已超过有效时间。
     * @param codeCreatedAt 绑定码创建时间
     * @return 是否过期
     */
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

    /**
     * 查找老人当前正在执行的订单，用于绑定信息页展示。
     * @param elderlyId 老人ID
     * @return 当前进行中的订单，没有则返回 null
     */
    private Map<String, Object> findCurrentWorkingOrderByElderlyId(Long elderlyId) {
        return taskMapper.selectCurrentWorkingOrderByElderlyId(elderlyId);
    }

    private static final double PLATFORM_FEE_SKILL = 0.10;
    private static final double PLATFORM_FEE_OTHER = 0.05;

    /**
     * 根据任务类型计算平台服务费。
     * @param salary 任务金额
     * @param taskType 任务类型
     * @return 平台服务费
     */
    private double calculatePlatformFee(Integer salary, String taskType) {
        if (salary == null || taskType == null) return 0;
        if (taskType != null && taskType.startsWith("skill:")) {
            return salary * PLATFORM_FEE_SKILL;
        }
        return salary * PLATFORM_FEE_OTHER;
    }

    /**
     * 根据当前查看角色计算展示金额。
     * @param salary 原始金额
     * @param taskType 任务类型
     * @param currentRole 当前角色
     * @return 展示金额
     */
    private double calculateDisplaySalary(Integer salary, String taskType, String currentRole) {
        if (salary == null) return 0;
        if ("elderly".equals(currentRole)) {
            double platformFee = calculatePlatformFee(salary, taskType);
            return salary - platformFee;
        }
        return salary;
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
     * 将空值归一化为空字符串。
     * @param value 原始值
     * @return 非空字符串
     */
    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 将统一资料结构转换为用户资料对象。
     * @param source 资料 Map
     * @return 用户资料对象
     */
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

    /**
     * 将订单记录转换为当前订单展示对象。
     * @param source 订单记录
     * @return 当前订单对象
     */
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

    /**
     * 将订单记录转换为绑定订单列表项。
     * @param source 订单记录
     * @return 绑定订单列表项
     */
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

    /**
     * 将绑定关系记录转换为绑定列表项。
     * @param source 绑定关系记录
     * @return 绑定列表项
     */
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

    /**
     * 将绑定状态数据转换为绑定状态对象。
     * @param source 绑定状态数据
     * @return 绑定状态对象
     */
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

    /**
     * 安全地将任意对象转换为 Boolean。
     * @param value 原始值
     * @return Boolean 值或 null
     */
    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
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
     * 将任意对象转换为字符串，空值返回 null。
     * @param value 原始值
     * @return 字符串或 null
     */
    private String strOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
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
     * 安全地将任意对象转换为 Double。
     * @param value 原始值
     * @return Double 值或 null
     */
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
