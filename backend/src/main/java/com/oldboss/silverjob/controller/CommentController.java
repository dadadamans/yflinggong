package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.Result;
import com.oldboss.silverjob.dto.CommentRequestDTO;
import com.oldboss.silverjob.service.CommentService;
import com.oldboss.silverjob.vo.CommentItemVO;
import com.oldboss.silverjob.vo.UserCommentStatsVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestHeader("Authorization") String authorization,
                          @Valid @RequestBody CommentRequestDTO request) {
        commentService.addComment(authorization, request);
        return Result.success(null, "评价已提交");
    }

    @GetMapping("/user/{userId}")
    public Result<?> userComments(@PathVariable("userId") Long userId,
                                  @RequestParam(value = "page", required = false) Integer page,
                                  @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        if (page == null && pageSize == null) {
            return Result.success(commentService.getUserComments(userId));
        }
        return Result.success(commentService.getUserComments(userId, page, pageSize));
    }

    @GetMapping("/user/{userId}/stats")
    public Result<UserCommentStatsVO> userCommentStats(@PathVariable("userId") Long userId) {
        return Result.success(commentService.getUserCommentStats(userId));
    }

    @GetMapping("/task/{taskId}")
    public Result<?> taskComments(@PathVariable("taskId") Long taskId,
                                  @RequestParam(value = "page", required = false) Integer page,
                                  @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        if (page == null && pageSize == null) {
            return Result.success(commentService.getTaskComments(taskId));
        }
        return Result.success(commentService.getTaskComments(taskId, page, pageSize));
    }
}
