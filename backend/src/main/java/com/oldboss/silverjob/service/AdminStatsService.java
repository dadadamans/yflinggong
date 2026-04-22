package com.oldboss.silverjob.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oldboss.silverjob.common.TaskStatus;
import com.oldboss.silverjob.entity.BindRelation;
import com.oldboss.silverjob.entity.Task;
import com.oldboss.silverjob.entity.User;
import com.oldboss.silverjob.mapper.BindRelationMapper;
import com.oldboss.silverjob.mapper.TaskMapper;
import com.oldboss.silverjob.mapper.UserMapper;
import com.oldboss.silverjob.vo.AdminStatsVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理员统计服务
 * 负责处理系统数据统计分析的核心逻辑
 *
 * 使用 MyBatis-Plus 的 LambdaQueryWrapper 实现：
 * - 类型安全：通过 User::getRoleType 等方法引用，不再使用字符串硬编码字段名
 * - SQL 预编译：自动防止 SQL 注入
 * - 一改全改：数据库字段名变更只需修改实体类 anotations
 */
@Service
public class AdminStatsService {

    /**
     * 用户Mapper：负责用户表的数据库操作
     * 继承自 BaseMapper<User>，拥有基本的增删改查能力
     */
    private final UserMapper userMapper;

    /**
     * 任务Mapper：负责任务表的数据库操作
     * 继承自 BaseMapper<Task>，拥有基本的增删改查能力
     * 同时包含自定义统计查询方法（如 selectStatusDistribution）
     */
    private final TaskMapper taskMapper;

    /**
     * 绑定关系Mapper：负责绑定关系表的数据库操作
     * 继承自 BaseMapper<BindRelation>
     */
    private final BindRelationMapper bindRelationMapper;

    /**
     * 构造方法注入
     * Spring 会自动注入三个 Mapper 实例
     *
     * @param userMapper 用户Mapper
     * @param taskMapper 任务Mapper
     * @param bindRelationMapper 绑定关系Mapper
     */
    public AdminStatsService(UserMapper userMapper, TaskMapper taskMapper, BindRelationMapper bindRelationMapper) {
        this.userMapper = userMapper;
        this.taskMapper = taskMapper;
        this.bindRelationMapper = bindRelationMapper;
    }

    /**
     * 获取管理员dashboard所需的所有统计数据
     * 包含用户统计、任务统计、绑定关系统计、趋势图表等
     *
     * @return 统计数据VO，包含以下字段：
     *   - elderlyCount: 老人用户数量
     *   - employerCount: 雇主用户数量
     *   - childCount: 子女用户数量
     *   - taskCount: 任务总数
     *   - waitingCount: 待接单任务数
     *   - workingCount: 进行中任务数
     *   - pendingPaymentCount: 待支付订单数
     *   - doneCount: 已完成任务数
     *   - bindCount: 绑定关系总数
     *   - confirmedBindCount: 已确认绑定数
     *   - orderStatusDist: 任务状态分布（饼图数据）
     *   - taskTypeDist: 任务类型分布
     *   - userTrend: 用户注册趋势（过去7天）
     *   - taskTrend: 任务发布趋势（过去7天）
     */
    public AdminStatsVO getDashboardStats() {
        AdminStatsVO statsVO = new AdminStatsVO();

        // ==================== 用户统计 ====================
        // 查询老人用户数量（role_type = 'elderly’）
        statsVO.setElderlyCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRoleType, "elderly")));

        // 查询雇主用户数量（role_type = 'employer’）
        statsVO.setEmployerCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRoleType, "employer")));

        // 查询子女用户数量（role_type = 'child’）
        statsVO.setChildCount(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRoleType, "child")));

        // ==================== 任务统计 ====================
        // 查询任务总数（selectCount(null) 表示不设置条件，相当于 count(*)）
        statsVO.setTaskCount(taskMapper.selectCount(null));

        // 查询状态为 "waiting" 的任务数（待接单）
        statsVO.setWaitingCount(taskMapper.selectCount(new LambdaQueryWrapper<Task>().eq(Task::getStatus, TaskStatus.WAITING)));

        // 查询状态为 "working" 的任务数（进行中）
        statsVO.setWorkingCount(taskMapper.selectCount(new LambdaQueryWrapper<Task>().eq(Task::getStatus, TaskStatus.WORKING)));

        // 查询状态为 "pending_payment" 的任务数（待支付）
        statsVO.setPendingPaymentCount(taskMapper.selectCount(new LambdaQueryWrapper<Task>().eq(Task::getStatus, TaskStatus.PENDING_PAYMENT)));

        // 查询状态为 "done" 的任务数（已完成）
        statsVO.setDoneCount(taskMapper.selectCount(new LambdaQueryWrapper<Task>().eq(Task::getStatus, TaskStatus.DONE)));

        // ==================== 绑定关系统计 ====================
        // 查询绑定关系总数
        statsVO.setBindCount(bindRelationMapper.selectCount(null));

        // 查询已确认的绑定关系数（confirmed = true）
        statsVO.setConfirmedBindCount(bindRelationMapper.selectCount(new LambdaQueryWrapper<BindRelation>().eq(BindRelation::getConfirmed, true)));

        // ==================== 分布图表数据 ====================
        // 任务状态分布：如 {status: "waiting", count: 10}, {status: "done", count: 5}
        // SQL: SELECT status, count(*) as count FROM task GROUP BY status
        statsVO.setOrderStatusDist(taskMapper.selectStatusDistribution());

        // 任务类型分布：如 {type: "陪伴", count: 8}, {type: "家务", count: 3}
        statsVO.setTaskTypeDist(taskMapper.selectTypeDistribution());

        // ==================== 趋势图表数据 ====================
        // 过去7天的用户注册趋势
        statsVO.setUserTrend(userMapper.selectUserTrend());

        // 过去7天的任务发布趋势
        statsVO.setTaskTrend(taskMapper.selectTaskTrend());

        return statsVO;
    }
}
