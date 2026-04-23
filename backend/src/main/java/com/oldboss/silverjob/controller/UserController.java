package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.Result;
import com.oldboss.silverjob.dto.ElderlyFontSizeRequestDTO;
import com.oldboss.silverjob.dto.IdRequestDTO;
import com.oldboss.silverjob.dto.UserSaveRequestDTO;
import com.oldboss.silverjob.service.UserService;
import com.oldboss.silverjob.vo.UserInfoVO;
import com.oldboss.silverjob.vo.UserSummaryVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户控制器
 * 处理用户资料查询、保存、启用禁用、字体设置等接口
 */
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 构造用户控制器。
     * @param userService 用户服务
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取所有用户列表（管理员专用）
     * @param authorization Authorization 请求头
     * @return 所有用户列表
     */
    @GetMapping("/list")
    public Result<?> list(@RequestHeader("Authorization") String authorization,
                          @RequestParam(value = "page", required = false) Integer page,
                          @RequestParam(value = "pageSize", required = false) Integer pageSize,
                          @RequestParam(value = "roleType", required = false) String roleType,
                          @RequestParam(value = "enabled", required = false) Boolean enabled,
                          @RequestParam(value = "healthStatus", required = false) String healthStatus) {
        log.info("获取所有用户列表");
        if (page == null && pageSize == null && roleType == null && enabled == null && healthStatus == null) {
            return Result.success(userService.listAllUsers(authorization));
        }
        return Result.success(userService.listAllUsers(authorization, page, pageSize, roleType, enabled, healthStatus));
    }

    /**
     * 获取当前登录用户的资料
     * @param authorization Authorization 请求头
     * @return 当前用户资料，包含角色和详细信息
     */
    @GetMapping("/info")
    public Result<UserInfoVO> info(@RequestHeader("Authorization") String authorization) {
        log.info("获取当前登录用户的资料");
        return Result.success(userService.userInfo(authorization));
    }

    /**
     * 保存当前登录用户的资料
     * @param authorization Authorization 请求头
     * @param payload 用户资料数据
     * @return 保存成功结果
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestHeader("Authorization") String authorization,
                                              @Valid @RequestBody UserSaveRequestDTO payload) {
        log.info("保存当前登录用户的资料");
        userService.saveUserInfo(authorization, payload);
        return Result.success(null, "资料已保存");
    }

    /**
     * 启用或禁用用户账号（管理员专用）
     * @param authorization Authorization 请求头
     * @param request 请求参数（用户ID和启用状态）
     * @return 操作结果
     */
    @PostMapping("/setEnabled")
    public Result<Void> setEnabled(@RequestHeader("Authorization") String authorization,
                                                   @Valid @RequestBody IdRequestDTO request) {
        log.info("启用或禁用用户账号");
        boolean enabled = request.getEnabled() != null && request.getEnabled();
        userService.setUserEnabled(authorization, request.getUserId(), enabled);
        return Result.success(null, enabled ? "已启用" : "已禁用");
    }

    /**
     * 设置老人字体大小（子女端专用）
     * @param authorization Authorization 请求头
     * @param payload 请求参数（老人ID和字体大小）
     * @return 设置成功结果
     */
    @PostMapping("/setElderlyFontSize")
    public Result<Void> setElderlyFontSize(@RequestHeader("Authorization") String authorization,
                                           @Valid @RequestBody ElderlyFontSizeRequestDTO payload) {
        log.info("设置老人字体大小");
        userService.setElderlyFontSize(authorization, payload.getElderlyId(), payload.getFontSize());
        return Result.success(null, "字体大小已更新");
    }
}
