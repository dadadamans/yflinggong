package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.common.PageQuery;
import com.oldboss.silverjob.common.PageResult;
import com.oldboss.silverjob.common.PageUtils;
import com.oldboss.silverjob.common.TaskStatus;
import com.oldboss.silverjob.common.TaskCategory;
import com.oldboss.silverjob.dto.TaskFormRequestDTO;
import com.oldboss.silverjob.mapper.BindRelationMapper;
import com.oldboss.silverjob.mapper.TaskMapper;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.vo.TaskDetailVO;
import com.oldboss.silverjob.vo.TaskItemVO;
import com.oldboss.silverjob.vo.OrderItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 任务服务
 * 处理任务大厅、任务发布、接单、订单管理等业务逻辑
 */
@Service
@Slf4j
public class TaskService {

    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TaskMapper taskMapper;
    private final BindRelationMapper bindRelationMapper;
    private final AuthService authService;
    private final UserService userService;
    private final CommentService commentService;

    /**
     * 构造任务服务并注入所需依赖。
     * @param taskMapper 任务数据访问对象
     * @param bindRelationMapper 绑定关系数据访问对象
     * @param authService 认证服务
     * @param userService 用户服务
     * @param commentService 评价服务
     */
    public TaskService(TaskMapper taskMapper, BindRelationMapper bindRelationMapper, AuthService authService, UserService userService, CommentService commentService) {
        this.taskMapper = taskMapper;
        this.bindRelationMapper = bindRelationMapper;
        this.authService = authService;
        this.userService = userService;
        this.commentService = commentService;
    }

    /**
     * 获取任务大厅列表
     * @param authorization Authorization 请求头
     * @return 当前用户可见的任务列表
     */
    public List<TaskItemVO> taskList(String authorization) {
        CurrentUser currentUser = authService.requireUser(authorization);
        String role = currentUser.getRoleType();
        Long userId = currentUser.getUserId();

        if ("elderly".equals(role)) {
            List<Long> childIds = bindRelationMapper.selectChildIdsByElderlyId(userId);
            return findWaitingTasksExcludePublishers(childIds, role);
        } else if ("employer".equals(role)) {
            return mapTaskRows(taskMapper.selectTasksByPublisherId(userId), role, userId);
        } else if ("child".equals(role)) {
            return mapTaskRows(taskMapper.selectTasksByPublisherId(userId), role, userId);
        }
        return mapTaskRows(taskMapper.selectTaskList(), role, userId);
    }

    /**
     * 分页获取任务大厅列表
     * @param authorization Authorization 请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页任务列表
     */
    public PageResult<TaskItemVO> taskList(String authorization, Integer page, Integer pageSize) {
        return taskList(authorization, page, pageSize, null);
    }

    /**
     * 按状态分页获取任务大厅列表
     * @param authorization Authorization 请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 任务状态筛选
     * @return 分页任务列表
     */
    public PageResult<TaskItemVO> taskList(String authorization, Integer page, Integer pageSize, String status) {
        CurrentUser currentUser = authService.requireUser(authorization);
        String role = currentUser.getRoleType();
        Long userId = currentUser.getUserId();
        PageQuery pageQuery = PageUtils.normalize(page, pageSize);
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim();

        List<TaskItemVO> list;
        long total;
        if ("elderly".equals(role)) {
            List<Long> childIds = bindRelationMapper.selectChildIdsByElderlyId(userId);
            if (childIds == null || childIds.isEmpty()) {
                list = mapTaskRows(taskMapper.selectWaitingTasksPage(pageQuery.getOffset(), pageQuery.getPageSize()), role, userId);
                total = taskMapper.countWaitingTasks();
            } else {
                list = mapTaskRows(taskMapper.selectWaitingTasksExcludePublishersPage(childIds, pageQuery.getOffset(), pageQuery.getPageSize()), role, userId);
                total = taskMapper.countWaitingTasksExcludePublishers(childIds);
            }
        } else if ("employer".equals(role) || "child".equals(role)) {
            list = mapTaskRows((normalizedStatus == null
                    ? taskMapper.selectTasksByPublisherIdPage(userId, pageQuery.getOffset(), pageQuery.getPageSize())
                    : taskMapper.selectTasksByPublisherIdPageAndStatus(userId, normalizedStatus, pageQuery.getOffset(), pageQuery.getPageSize()))
                    , role, userId);
            total = normalizedStatus == null
                    ? taskMapper.countTasksByPublisherId(userId)
                    : taskMapper.countTasksByPublisherIdAndStatus(userId, normalizedStatus);
        } else {
            list = mapTaskRows(taskMapper.selectTaskListPage(pageQuery.getOffset(), pageQuery.getPageSize()), role, userId);
            total = taskMapper.countTaskList();
        }
        return PageUtils.buildPageResult(list, total, pageQuery);
    }

    /**
     * 老人端查询待接单任务时，排除已绑定子女发布的任务。
     * @param excludePublisherIds 需要排除的发布者ID列表
     * @param role 当前角色
     * @return 过滤后的任务列表
     */
    private List<TaskItemVO> findWaitingTasksExcludePublishers(List<Long> excludePublisherIds, String role) {
        if (excludePublisherIds == null || excludePublisherIds.isEmpty()) {
            return mapTaskRows(taskMapper.selectWaitingTasks(), role, null);
        }
        return mapTaskRows(taskMapper.selectWaitingTasksExcludePublishers(excludePublisherIds), role, null);
    }

    /**
     * 获取任务详情
     * @param authorization Authorization 请求头
     * @param id 任务ID
     * @return 任务详情信息
     */
    public TaskDetailVO taskDetail(String authorization, Long id) {
        CurrentUser currentUser = authService.requireUser(authorization);
        String role = currentUser.getRoleType();
        Map<String, Object> task = taskMapper.selectTaskById(id);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        return toTaskDetailVO(mapRowWithSalary(task, role));
    }

    /**
     * 获取待接单任务列表（管理员专用）
     * @param authorization Authorization 请求头
     * @return 所有待接单的任务列表
     */
    public List<TaskItemVO> adminWaitingTasks(String authorization) {
        authService.requireAdmin(authorization);
        return mapTaskRows(taskMapper.selectTasksByStatus(TaskStatus.WAITING), "admin", null);
    }

    /**
     * 分页获取待接单任务列表（管理员专用）
     * @param authorization Authorization 请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页任务列表
     */
    public PageResult<TaskItemVO> adminWaitingTasks(String authorization, Integer page, Integer pageSize) {
        authService.requireAdmin(authorization);
        PageQuery pageQuery = PageUtils.normalize(page, pageSize);
        List<TaskItemVO> list = mapTaskRows(taskMapper.selectTasksByStatusPage(TaskStatus.WAITING, pageQuery.getOffset(), pageQuery.getPageSize()), "admin", null);
        long total = taskMapper.countTasksByStatus(TaskStatus.WAITING);
        return PageUtils.buildPageResult(list, total, pageQuery);
    }

    /**
     * 发布新任务
     * @param authorization Authorization 请求头
     * @param request 任务表单数据
     */
    /**
     * 发布新任务
     * @param authorization Authorization 请求头
     * @param request 任务表单数据
     */
    @Transactional
    public void addTask(String authorization, TaskFormRequestDTO request) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!Arrays.asList("employer", "child").contains(currentUser.getRoleType())) {
            throw new BizException("当前身份不能发布任务");
        }
        if (safe(request.getTitle()).isEmpty() || safe(request.getType()).isEmpty() || safe(request.getAddress()).isEmpty()
                || request.getSalary() == null || safe(request.getTimeText()).isEmpty() || safe(request.getContent()).isEmpty()) {
            throw new BizException("请把任务信息填写完整");
        }

        validateTaskTime(request.getTimeText());

        String category = safe(request.getCategory());
        String subType = safe(request.getType());
        String taskType = buildTaskType(category, subType);

        long publisherId = currentUser.getUserId();
        String publisherName = str(currentUser.getProfile().get("nickname"));
        if (publisherName == null || publisherName.isEmpty()) {
            publisherName = str(currentUser.getProfile().get("realName"));
        }
        if (publisherName == null || publisherName.isEmpty()) {
            publisherName = "用户" + publisherId;
        }

        taskMapper.insertTask(taskType, request.getTitle().trim(), request.getAddress().trim(),
                request.getTimeText().trim(), request.getSalary(), request.getContent().trim(),
                publisherId, publisherName, currentUser.getRoleType());
    }

    /**
     * 老人申请接单
     * @param authorization Authorization 请求头
     * @param id 任务ID
     */
    @Transactional
    public void applyTask(String authorization, Long id) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!"elderly".equals(currentUser.getRoleType())) {
            throw new BizException("只有老人用户可以申请接单");
        }

        if (!userService.isHealthReportApproved(currentUser.getUserId())) {
            throw new BizException("您的体检报告还未通过审核，无法申请接单");
        }

        Map<String, Object> task = taskMapper.selectTaskById(id);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        if (!TaskStatus.WAITING.equals(task.get("status"))) {
            throw new BizException("当前任务不能申请接单");
        }

        long elderlyId = currentUser.getUserId();
        String elderlyName = getUserDisplayName(currentUser);

        int updated = taskMapper.updateTaskToApplying(id, elderlyId, elderlyName);
        if (updated == 0) {
            throw new BizException("当前任务不能申请接单");
        }
    }

    /**
     * 雇主或子女批准接单申请。
     * @param authorization Authorization 请求头
     * @param taskId 任务ID
     */
    @Transactional
    public void approveTask(String authorization, Long taskId) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!Arrays.asList("employer", "child").contains(currentUser.getRoleType())) {
            throw new BizException("只有雇主可以批准接单");
        }

        Map<String, Object> task = taskMapper.selectTaskById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        if (!TaskStatus.APPLYING.equals(task.get("status"))) {
            throw new BizException("没有待审核的申请");
        }
        Long publisherId = toLong(task.get("publisher_id"));
        if (!publisherId.equals(currentUser.getUserId())) {
            throw new BizException("只能审核自己发布的任务");
        }

        String orderCode = buildOrderCode();
        int updated = taskMapper.updateTaskApprove(taskId, orderCode);
        if (updated == 0) {
            throw new BizException("审核失败");
        }
    }

    /**
     * 雇主或子女拒绝接单申请。
     * @param authorization Authorization 请求头
     * @param taskId 任务ID
     */
    @Transactional
    public void rejectTask(String authorization, Long taskId) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!Arrays.asList("employer", "child").contains(currentUser.getRoleType())) {
            throw new BizException("只有雇主可以拒绝接单");
        }

        Map<String, Object> task = taskMapper.selectTaskById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        if (!TaskStatus.APPLYING.equals(task.get("status"))) {
            throw new BizException("没有待审核的申请");
        }
        Long publisherId = toLong(task.get("publisher_id"));
        if (!publisherId.equals(currentUser.getUserId())) {
            throw new BizException("只能审核自己发布的任务");
        }

        int updated = taskMapper.updateTaskReject(taskId);
        if (updated == 0) {
            throw new BizException("拒绝失败");
        }
    }

    /**
     * 从当前登录用户资料中提取任务展示名称。
     * @param user 当前登录用户
     * @return 展示名称
     */
    private String getUserDisplayName(CurrentUser user) {
        String nickname = user.getProfile().get("nickname") != null
            ? String.valueOf(user.getProfile().get("nickname"))
            : null;
        if (nickname != null && !nickname.isEmpty()) {
            return nickname;
        }
        String realName = user.getProfile().get("realName") != null
            ? String.valueOf(user.getProfile().get("realName"))
            : null;
        if (realName != null && !realName.isEmpty()) {
            return realName;
        }
        return "用户" + user.getUserId();
    }

    /**
     * 获取我的任务列表
     * @param authorization Authorization 请求头
     * @return 当前用户发布的任务列表
     */
    public List<Map<String, Object>> myTaskList(String authorization) {
        CurrentUser currentUser = authService.requireUser(authorization);
        Long userId = currentUser.getUserId();
        String role = currentUser.getRoleType();
        return taskMapper.selectTasksByPublisherId(userId).stream().map(row -> mapRowWithSalary(row, role)).toList();
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
        String realName = nullToEmpty(user.get("real_name"));
        String nickname = nullToEmpty(user.get("nickname"));
        return realName.isEmpty() ? nickname : realName;
    }

    /**
     * 生成订单编号。
     * @return 订单编号
     */
    private String buildOrderCode() {
        return "#" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%02d", taskMapper.getNextOrderId());
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
     * 校验任务时间格式与发布时间约束。
     * @param timeText 时间描述文本
     */
    private void validateTaskTime(String timeText) {
        if (timeText == null || timeText.isEmpty()) {
            return;
        }
        String[] parts = timeText.trim().split("\\s+", 2);
        if (parts.length != 2 || !parts[1].contains("-")) {
            throw new BizException("任务时间格式不正确");
        }

        String datePart = parts[0];
        String[] timeRange = parts[1].split("-", 2);
        if (timeRange.length != 2) {
            throw new BizException("任务时间格式不正确");
        }

        LocalDate taskDate;
        LocalTime startTime;
        LocalTime endTime;
        try {
            taskDate = LocalDate.parse(datePart);
            startTime = LocalTime.parse(timeRange[0].trim());
            endTime = LocalTime.parse(timeRange[1].trim());
        } catch (DateTimeParseException e) {
            throw new BizException("任务时间格式不正确");
        }

        if (!endTime.isAfter(startTime)) {
            throw new BizException("任务时间格式不正确");
        }

        LocalDate today = LocalDate.now();
        if (taskDate.isBefore(today)) {
            throw new BizException("任务时间不能早于当前日期");
        }

        if (taskDate.isEqual(today)) {
            LocalTime now = LocalTime.now();
            LocalTime minTime = now.plusHours(2);
            if (startTime.isBefore(minTime)) {
                throw new BizException("今天需提前2小时发布");
            }
        }
    }

    /**
     * 组合任务大类和子类型得到最终任务类型编码。
     * @param category 任务大类
     * @param subType 子类型
     * @return 任务类型编码
     */
    private String buildTaskType(String category, String subType) {
        if (category.isEmpty() && subType.isEmpty()) {
            return "";
        }
        if (!category.isEmpty() && !subType.isEmpty()) {
            return category + ":" + subType;
        }
        if (!subType.isEmpty()) {
            TaskCategory cat = TaskCategory.getBySubType(subType);
            if (cat != null) {
                return cat.getCode() + ":" + subType;
            }
            return subType;
        }
        return category + ":";
    }

    /**
     * 获取订单列表（根据用户角色返回不同范围的订单）
     * @param authorization Authorization 请求头
     * @return 订单列表
     */
    public List<OrderItemVO> orderList(String authorization) {
        CurrentUser currentUser = authService.requireUser(authorization);
        String role = currentUser.getRoleType();
        Long userId = currentUser.getUserId();

        if ("admin".equals(role)) {
            return mapOrderRows(taskMapper.selectAllOrders(), role, userId);
        } else if ("employer".equals(role) || "child".equals(role)) {
            return mapOrderRows(taskMapper.selectOrdersByPublisherId(userId), role, userId);
        } else if ("elderly".equals(role)) {
            return mapOrderRows(taskMapper.selectOrdersByElderlyId(userId), role, userId);
        }
        return List.of();
    }

    /**
     * 分页获取订单列表
     * @param authorization Authorization 请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页订单结果
     */
    public PageResult<OrderItemVO> orderList(String authorization, Integer page, Integer pageSize) {
        return orderList(authorization, page, pageSize, null);
    }

    /**
     * 按状态分页获取订单列表
     * @param authorization Authorization 请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 订单状态筛选
     * @return 分页订单结果
     */
    public PageResult<OrderItemVO> orderList(String authorization, Integer page, Integer pageSize, String status) {
        CurrentUser currentUser = authService.requireUser(authorization);
        String role = currentUser.getRoleType();
        Long userId = currentUser.getUserId();
        PageQuery pageQuery = PageUtils.normalize(page, pageSize);
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim();

        List<OrderItemVO> list;
        long total;
        if ("admin".equals(role)) {
            list = mapOrderRows((normalizedStatus == null
                    ? taskMapper.selectAllOrdersPage(pageQuery.getOffset(), pageQuery.getPageSize())
                    : taskMapper.selectAllOrdersPageByStatus(normalizedStatus, pageQuery.getOffset(), pageQuery.getPageSize()))
                    , role, userId);
            total = normalizedStatus == null ? taskMapper.countAllOrders() : taskMapper.countAllOrdersByStatus(normalizedStatus);
        } else if ("employer".equals(role) || "child".equals(role)) {
            list = mapOrderRows((normalizedStatus == null
                    ? taskMapper.selectOrdersByPublisherIdPage(userId, pageQuery.getOffset(), pageQuery.getPageSize())
                    : taskMapper.selectOrdersByPublisherIdPageAndStatus(userId, normalizedStatus, pageQuery.getOffset(), pageQuery.getPageSize()))
                    , role, userId);
            total = normalizedStatus == null
                    ? taskMapper.countOrdersByPublisherId(userId)
                    : taskMapper.countOrdersByPublisherIdAndStatus(userId, normalizedStatus);
        } else if ("elderly".equals(role)) {
            list = mapOrderRows((normalizedStatus == null
                    ? taskMapper.selectOrdersByElderlyIdPage(userId, pageQuery.getOffset(), pageQuery.getPageSize())
                    : taskMapper.selectOrdersByElderlyIdPageAndStatus(userId, normalizedStatus, pageQuery.getOffset(), pageQuery.getPageSize()))
                    , role, userId);
            total = normalizedStatus == null
                    ? taskMapper.countOrdersByElderlyId(userId)
                    : taskMapper.countOrdersByElderlyIdAndStatus(userId, normalizedStatus);
        } else {
            list = List.of();
            total = 0;
        }
        return PageUtils.buildPageResult(list, total, pageQuery);
    }

    /**
     * 老人完成订单。
     * @param authorization Authorization 请求头
     * @param taskId 任务ID
     */
    @Transactional
    public void finishOrder(String authorization, Long taskId) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!"elderly".equals(currentUser.getRoleType())) {
            throw new BizException("当前身份不能完成任务");
        }
        Map<String, Object> task = taskMapper.selectTaskById(taskId);
        if (task == null) {
            throw new BizException("订单不存在");
        }
        Long elderlyId = toLong(task.get("elderly_id"));
        if (!elderlyId.equals(currentUser.getUserId())) {
            throw new BizException("只能完成自己接的订单");
        }
        String status = (String) task.get("status");
        if (!TaskStatus.WORKING.equals(status)) {
            throw new BizException("只有进行中的订单才能完成");
        }
        taskMapper.updateTaskToPendingPayment(taskId);
    }

    /**
     * 雇主或子女支付订单。
     * @param authorization Authorization 请求头
     * @param taskId 任务ID
     */
    @Transactional
    public void payOrder(String authorization, Long taskId) {
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!Arrays.asList("employer", "child").contains(currentUser.getRoleType())) {
            throw new BizException("当前身份不能支付");
        }
        Map<String, Object> task = taskMapper.selectTaskById(taskId);
        if (task == null) {
            throw new BizException("订单不存在");
        }
        Long publisherId = toLong(task.get("publisher_id"));
        if (!publisherId.equals(currentUser.getUserId())) {
            throw new BizException("只能支付自己发布的订单");
        }
        String status = (String) task.get("status");
        if (!TaskStatus.PENDING_PAYMENT.equals(status)) {
            throw new BizException("只有待支付的订单才能支付");
        }
        taskMapper.updateTaskToDone(taskId);
    }

    /**
     * 雇主或子女取消待接单订单。
     * @param authorization Authorization 请求头
     * @param taskId 任务ID
     */
    @Transactional
    public void cancelOrder(String authorization, Long taskId) {
        CurrentUser currentUser = authService.requireUser(authorization);
        String role = currentUser.getRoleType();
        if (!Arrays.asList("employer", "child").contains(role)) {
            throw new BizException("只有雇主或子女可以取消订单");
        }
        Map<String, Object> task = taskMapper.selectTaskById(taskId);
        if (task == null) {
            throw new BizException("订单不存在");
        }
        String status = (String) task.get("status");
        if (!TaskStatus.WAITING.equals(status)) {
            throw new BizException("只有待接单的订单才能取消");
        }
        Long publisherId = toLong(task.get("publisher_id"));
        if (!publisherId.equals(currentUser.getUserId())) {
            throw new BizException("只能取消自己发布的订单");
        }
        taskMapper.updateTaskToCancelled(taskId);
    }

    /**
     * 删除订单，管理员可直接删除，发布方仅能删除待接单订单。
     * @param authorization Authorization 请求头
     * @param taskId 任务ID
     */
    @Transactional
    public void deleteOrder(String authorization, Long taskId) {
        CurrentUser currentUser = authService.requireUser(authorization);
        String role = currentUser.getRoleType();
        Map<String, Object> task = taskMapper.selectTaskById(taskId);
        if (task == null) {
            throw new BizException("订单不存在");
        }
        String status = (String) task.get("status");
        if ("admin".equals(role)) {
            commentService.deleteCommentsByTaskId(taskId);
            taskMapper.deleteTaskById(taskId);
        } else if (Arrays.asList("employer", "child").contains(role)) {
            if (!TaskStatus.WAITING.equals(status)) {
                throw new BizException("只能删除待接单的订单");
            }
            Long publisherId = toLong(task.get("publisher_id"));
            if (!publisherId.equals(currentUser.getUserId())) {
                throw new BizException("只能删除自己发布的订单");
            }
            taskMapper.deleteTaskById(taskId);
        } else {
            throw new BizException("当前身份无法删除订单");
        }
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
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final double PLATFORM_FEE_SKILL = 0.10;
    private static final double PLATFORM_FEE_OTHER = 0.05;

    /**
     * 根据任务类型计算平台服务费。
     * @param salary 原始金额
     * @param taskType 任务类型
     * @return 平台服务费
     */
    private double calculatePlatformFee(Integer salary, String taskType) {
        if (salary == null || taskType == null) return 0;
        if (taskType.startsWith("skill:")) {
            return salary * PLATFORM_FEE_SKILL;
        }
        return salary * PLATFORM_FEE_OTHER;
    }

    /**
     * 按当前角色计算前端展示金额。
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
     * 保留原始行数据的占位方法，便于后续扩展字段映射。
     * @param row 原始记录
     * @return 原始记录
     */
    private Map<String, Object> mapRow(Map<String, Object> row) {
        return row;
    }

    /**
     * 将数据库任务记录补充为前端需要的金额、类型和时间格式字段。
     * @param row 原始任务记录
     * @param currentRole 当前查看角色
     * @return 处理后的任务记录
     */
    private Map<String, Object> mapRowWithSalary(Map<String, Object> row, String currentRole) {
        Integer salary = (Integer) row.get("salary");
        String taskType = (String) row.get("task_type");

        double platformFee = calculatePlatformFee(salary, taskType);
        double displaySalary = calculateDisplaySalary(salary, taskType, currentRole);

        row.put("platformFee", platformFee);
        row.put("displaySalary", displaySalary);

        String chineseType = TaskCategory.getChineseLabel(taskType);
        row.put("taskType", chineseType);

        Object startTime = row.get("start_time");
        Object finishTime = row.get("finish_time");
        if (startTime != null) {
            String startStr = startTime.toString().replace("T", " ");
            if (startStr.length() >= 16) {
                row.put("formattedStartTime", startStr.substring(0, 16));
            }
        }
        if (finishTime != null) {
            String finishStr = finishTime.toString().replace("T", " ");
            if (finishStr.length() >= 16) {
                row.put("formattedFinishTime", finishStr.substring(0, 16));
            }
        }

        return row;
    }

    /**
     * 将任务记录转换为任务详情对象。
     * @param row 任务记录
     * @return 任务详情对象
     */
    private TaskDetailVO toTaskDetailVO(Map<String, Object> row) {
        TaskDetailVO vo = new TaskDetailVO();
        vo.setId(toLong(row.get("id")));
        vo.setTitle(strOrNull(row.get("title")));
        vo.setTaskType(strOrNull(row.get("taskType")));
        vo.setAddress(strOrNull(row.get("address")));
        vo.setTimeText(strOrNull(row.get("time_text")));
        vo.setSalary(toInteger(row.get("salary")));
        vo.setPlatformFee(toDouble(row.get("platformFee")));
        vo.setDisplaySalary(toDouble(row.get("displaySalary")));
        vo.setContent(strOrNull(row.get("content")));
        vo.setPublisherId(toLong(row.get("publisher_id")));
        vo.setPublisherName(strOrNull(row.get("publisher_name")));
        vo.setPublisherRole(strOrNull(row.get("publisher_role")));
        vo.setElderlyId(toLong(row.get("elderly_id")));
        vo.setElderlyName(strOrNull(row.get("elderly_name")));
        vo.setStatus(strOrNull(row.get("status")));
        vo.setSettleText(strOrNull(row.get("settle_text")));
        vo.setFormattedStartTime(strOrNull(row.get("formattedStartTime")));
        vo.setFormattedFinishTime(strOrNull(row.get("formattedFinishTime")));
        return vo;
    }

    /**
     * 将任务记录转换为任务列表项对象。
     * @param row 任务记录
     * @return 任务列表项
     */
    private List<TaskItemVO> mapTaskRows(List<Map<String, Object>> rows, String role, Long reviewerId) {
        Set<Long> commentedTaskIds = findCommentedTaskIds(rows, reviewerId);
        return rows.stream()
                .map(row -> mapRowWithSalary(row, role))
                .map(row -> toTaskItemVO(row, commentedTaskIds))
                .toList();
    }

    private List<OrderItemVO> mapOrderRows(List<Map<String, Object>> rows, String role, Long reviewerId) {
        Set<Long> commentedTaskIds = findCommentedTaskIds(rows, reviewerId);
        return rows.stream()
                .map(row -> mapRowWithSalary(row, role))
                .map(row -> toOrderItemVO(row, commentedTaskIds))
                .toList();
    }

    private Set<Long> findCommentedTaskIds(List<Map<String, Object>> rows, Long reviewerId) {
        if (rows == null || rows.isEmpty() || reviewerId == null) {
            return Set.of();
        }

        Set<Long> taskIds = rows.stream()
                .map(row -> toLong(row.get("id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return commentService.findCommentedTaskIds(taskIds, reviewerId);
    }

    private TaskItemVO toTaskItemVO(Map<String, Object> row, Set<Long> commentedTaskIds) {
        TaskItemVO vo = new TaskItemVO();
        vo.setId(toLong(row.get("id")));
        vo.setTitle(strOrNull(row.get("title")));
        vo.setTaskType(strOrNull(row.get("taskType")));
        vo.setAddress(strOrNull(row.get("address")));
        vo.setTimeText(strOrNull(row.get("time_text")));
        vo.setSalary(toInteger(row.get("salary")));
        vo.setPlatformFee(toDouble(row.get("platformFee")));
        vo.setDisplaySalary(toDouble(row.get("displaySalary")));
        vo.setContent(strOrNull(row.get("content")));
        vo.setPublisherId(toLong(row.get("publisher_id")));
        vo.setPublisherName(strOrNull(row.get("publisher_name")));

        Integer publisherTotal = toInteger(row.get("publisher_total_score"));
        Integer publisherCount = toInteger(row.get("publisher_comment_count"));
        vo.setPublisherAvgRating(calculateAvgRating(publisherTotal, publisherCount));
        vo.setPublisherCommentCount(publisherCount);

        vo.setElderlyId(toLong(row.get("elderly_id")));
        vo.setElderlyName(strOrNull(row.get("elderly_name")));
        vo.setElderlyMobile(strOrNull(row.get("elderly_mobile")));
        vo.setElderlyHealthCondition(strOrNull(row.get("elderly_health_condition")));

        Integer elderlyTotal = toInteger(row.get("elderly_total_score"));
        Integer elderlyCount = toInteger(row.get("elderly_comment_count"));
        vo.setElderlyAvgRating(calculateAvgRating(elderlyTotal, elderlyCount));
        vo.setElderlyCommentCount(elderlyCount);

        vo.setSettleText(strOrNull(row.get("settle_text")));
        vo.setStatus(strOrNull(row.get("status")));
        vo.setFormattedStartTime(strOrNull(row.get("formattedStartTime")));
        vo.setFormattedFinishTime(strOrNull(row.get("formattedFinishTime")));
        vo.setCurrentUserCommented(commentedTaskIds.contains(vo.getId()));
        return vo;
    }

    /**
     * 根据总分和评价数计算平均分。
     * @param totalScore 总分
     * @param commentCount 评价数
     * @return 平均分
     */
    private Double calculateAvgRating(Integer totalScore, Integer commentCount) {
        if (totalScore == null || commentCount == null || commentCount == 0) {
            return 0.0;
        }
        return (double) totalScore / commentCount;
    }

    /**
     * 将订单记录转换为订单列表对象。
     * @param row 订单记录
     * @return 订单列表对象
     */
    private OrderItemVO toOrderItemVO(Map<String, Object> row, Set<Long> commentedTaskIds) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(toLong(row.get("id")));
        String orderCode = strOrNull(row.get("order_code"));
        vo.setOrderCode(orderCode);
        vo.setCode(orderCode);
        vo.setTitle(strOrNull(row.get("title")));
        vo.setAddress(strOrNull(row.get("address")));
        vo.setTimeText(strOrNull(row.get("time_text")));
        vo.setSalary(toInteger(row.get("salary")));
        vo.setPlatformFee(toDouble(row.get("platformFee")));
        vo.setDisplaySalary(toDouble(row.get("displaySalary")));
        vo.setElderlyName(strOrNull(row.get("elderly_name")));
        vo.setPublisherId(toLong(row.get("publisher_id")));
        vo.setPublisherName(strOrNull(row.get("publisher_name")));
        vo.setPublisherMobile(strOrNull(row.get("publisher_mobile")));
        vo.setSettleText(strOrNull(row.get("settle_text")));
        vo.setStatus(strOrNull(row.get("status")));
        vo.setFormattedStartTime(strOrNull(row.get("formattedStartTime")));
        vo.setFormattedFinishTime(strOrNull(row.get("formattedFinishTime")));
        vo.setCurrentUserCommented(commentedTaskIds.contains(vo.getId()));
        return vo;
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
