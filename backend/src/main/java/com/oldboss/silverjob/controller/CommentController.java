package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.Result;
import com.oldboss.silverjob.dto.CommentRequestDTO;
import com.oldboss.silverjob.service.CommentService;
import com.oldboss.silverjob.vo.CommentItemVO;
import com.oldboss.silverjob.vo.UserCommentStatsVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评价控制器
 * 处理评价提交、任务评价查询和用户评价统计等接口
 */
@RestController
@RequestMapping("/api/comment")
@Slf4j
public class CommentController {

    private final CommentService commentService;

    /**
     * 构造评价控制器。
     * @param commentService 评价服务
     */
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 提交任务评价
     * @param authorization Authorization 请求头
     * @param request 评价请求，包含任务、被评价人和评分信息
     * @return 提交成功结果
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestHeader("Authorization") String authorization,
                          @Valid @RequestBody CommentRequestDTO request) {
        log.info("提交任务评价: {}", request);
        commentService.addComment(authorization, request);
        return Result.success(null, "评价已提交");
    }

    /**
     * 获取指定用户收到的评价列表
     * @param userId 用户ID
     * @return 评价列表或分页结果
     */
    @GetMapping("/user/{userId}")
    public Result<?> userComments(@PathVariable("userId") Long userId,
                                  @RequestParam(value = "page", required = false) Integer page,
                                  @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        log.info("获取指定用户收到的评价列表: {}" , userId , page, pageSize);
        if (page == null && pageSize == null) {
            return Result.success(commentService.getUserComments(userId));
        }
        return Result.success(commentService.getUserComments(userId, page, pageSize));
    }

    /**
     * 获取指定用户的评价统计
     * @param userId 用户ID
     * @return 用户总分、评价条数和平均分
     */
    @GetMapping("/user/{userId}/stats")
    public Result<UserCommentStatsVO> userCommentStats(@PathVariable("userId") Long userId) {
        log.info("获取指定用户的评价统计: {}" , userId);
        return Result.success(commentService.getUserCommentStats(userId));
    }

    /**
     * 获取指定任务下的评价列表
     * @param taskId 任务ID
     * @return 任务评价列表或分页结果
     */
    @GetMapping("/task/{taskId}")
    public Result<?> taskComments(@PathVariable("taskId") Long taskId,
                                  @RequestParam(value = "page", required = false) Integer page,
                                  @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        log.info("获取指定任务下的评价列表:{}" , taskId);
        if (page == null && pageSize == null) {
            return Result.success(commentService.getTaskComments(taskId));
        }
        return Result.success(commentService.getTaskComments(taskId, page, pageSize));
    }
}
