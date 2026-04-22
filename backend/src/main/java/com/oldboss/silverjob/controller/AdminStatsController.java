package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.Result;
import com.oldboss.silverjob.service.AdminStatsService;
import com.oldboss.silverjob.service.AuthService;
import com.oldboss.silverjob.vo.AdminStatsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员统计控制器
 * 处理系统数据统计、报表等接口
 *
 * 设计原则：
 * - 控制器只负责接收请求、调用Service、返回响应
 * - 业务逻辑全部下沉到Service层，保持Controller简洁
 * - 这样更符合分层架构设计，便于测试和维护
 */
@RestController
@RequestMapping("/api/admin")
public class AdminStatsController {

    /**
     * 管理员统计服务
     * 通过构造方法注入，由Spring管理生命周期
     */
    private final AdminStatsService adminStatsService;
    private final AuthService authService;

    /**
     * 构造方法注入
     * 推荐使用构造方法注入而非字段注入，便于单元测试和保证不可变性
     *
     * @param adminStatsService 管理员统计服务实例
     */
    public AdminStatsController(AdminStatsService adminStatsService, AuthService authService) {
        this.adminStatsService = adminStatsService;
        this.authService = authService;
    }

    /**
     * 获取系统统计数据概览
     * 这是管理员dashboard的核心接口，返回所有统计数据
     *
     * 接口路径：GET /api/admin/stats
     * 请求头：Authorization: Bearer <token>
     *
     * @param authorization Authorization请求头，用于验证管理员身份
     * @return 统计数据VO，包含用户数量、订单数量、绑定关系、趋势图表等
     *         返回格式：Result.success(data)
     */
    @GetMapping("/stats")
    public Result<AdminStatsVO> stats(@RequestHeader("Authorization") String authorization) {
        authService.requireAdmin(authorization);
        return Result.success(adminStatsService.getDashboardStats());
    }
}
