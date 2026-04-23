package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.common.Result;
import com.oldboss.silverjob.dto.IdRequestDTO;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.service.AuthService;
import com.oldboss.silverjob.service.UserService;
import com.oldboss.silverjob.vo.HealthReportVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 体检报告控制器
 * 处理老人体检报告上传、审核、状态查询等接口
 */
@RestController
@RequestMapping("/api/health")
@Slf4j
public class HealthReportController {

    @Value("${app.upload.health-report-dir:#{systemProperties['user.dir'] + '/uploads/healthReports/'}}")
    private String uploadDir;
    
    private final AuthService authService;
    private final UserService userService;

    /**
     * 构造体检报告控制器。
     * @param authService 认证服务
     * @param userService 用户服务
     */
    public HealthReportController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * 初始化上传目录
     */
    @PostConstruct
    private void initUploadDir() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new IllegalStateException("无法初始化体检报告目录", e);
        }
    }

    /**
     * 上传体检报告（老人端专用）
     * @param authorization Authorization 请求头
     * @param file 体检报告文件
     * @return 上传成功结果
     */
    @PostMapping("/upload")
    public Result<Void> upload(@RequestHeader("Authorization") String authorization,
                                                @RequestParam("file") MultipartFile file) {
        log.info("上传体检报告");
        CurrentUser currentUser = authService.requireUser(authorization);
        if (!"elderly".equals(currentUser.getRoleType())) {
            log.info("只有老人才能上传体检报告");
            throw new BizException("只有老人才能上传体检报告");
        }

        if (file == null || file.isEmpty()) {
            log.info("请选择文件");
            throw new BizException("请选择文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            log.info("文件名无效");
            throw new BizException("文件名无效");
        }
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex <= 0 || dotIndex == originalFilename.length() - 1) {
            log.info("文件名无效");
            throw new BizException("文件名无效");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (!".pdf".equalsIgnoreCase(ext)) {
            log.info("仅支持PDF文件");
            throw new BizException("仅支持 PDF 文件");
        }
        String newFilename = UUID.randomUUID().toString() + ext;
        Path path = Paths.get(uploadDir, newFilename);

        try {
            // 先完成文件落盘，再保存访问路径，避免数据库记录指向不存在的文件。
            Files.write(path, file.getBytes());

            String fileUrl = "/uploads/healthReports/" + newFilename;
            userService.saveHealthReportUrl(authorization, fileUrl);

            return Result.success(null, "体检报告已上传");
        } catch (IOException e) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // 清理失败时忽略，优先保留原始上传异常信息。
            }
            throw new BizException("文件上传失败，请稍后重试");
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // 业务异常发生后回滚已落盘文件，避免产生孤立文件。
            }
            throw e;
        }
    }

    /**
     * 审核体检报告（管理员专用）
     * @param authorization Authorization 请求头
     * @param request 审核请求（用户ID和审核状态）
     * @return 审核结果
     */
    @PostMapping("/review")
    public Result<Void> review(@RequestHeader("Authorization") String authorization,
                                                @Valid @RequestBody IdRequestDTO request) {
        log.info("审核体检报告");
        Long userId = request.getUserId();
        Boolean approved = request.getEnabled();
        String healthCondition = request.getHealthCondition();
        userService.reviewHealthReport(authorization, userId, approved, healthCondition);
        return Result.success(null, approved ? "已通过审核" : "已拒绝");
    }

    /**
     * 获取体检报告状态
     * @param authorization Authorization 请求头
     * @return 体检报告状态信息
     */
    @GetMapping("/status")
    public Result<HealthReportVO> status(@RequestHeader("Authorization") String authorization) {
        log.info("获取体检报告状态");
        return Result.success(userService.getHealthReportStatus(authorization));
    }
}
