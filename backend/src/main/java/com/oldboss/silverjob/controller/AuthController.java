package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.Result;
import com.oldboss.silverjob.dto.LoginRequestDTO;
import com.oldboss.silverjob.dto.RegisterRequestDTO;
import com.oldboss.silverjob.service.AuthService;
import com.oldboss.silverjob.vo.CurrentUserVO;
import com.oldboss.silverjob.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证控制器
 * 处理用户注册、登录、退出、获取当前用户信息等接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册
     * @param request 注册请求参数（用户名、密码、角色等）
     * @return 注册结果，包含 token 和用户信息
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return Result.success(authService.register(request), "注册成功");
    }

    /**
     * 用户登录
     * @param request 登录请求参数（用户名、密码）
     * @return 登录结果，包含 token 和用户信息
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequestDTO request) {
        return Result.success(authService.login(request), "登录成功");
    }

    /**
     * 用户退出登录
     * @param authorization Authorization 请求头
     * @return 退出成功结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return Result.success(null, "退出成功");
    }

    /**
     * 获取当前登录用户信息
     * @param authorization Authorization 请求头
     * @return 当前用户信息
     */
    @GetMapping("/currentUser")
    public Result<CurrentUserVO> currentUser(@RequestHeader("Authorization") String authorization) {
        return Result.success(authService.currentUser(authorization));
    }
}
