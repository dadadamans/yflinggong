package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.common.PageQuery;
import com.oldboss.silverjob.common.PageResult;
import com.oldboss.silverjob.common.PageUtils;
import com.oldboss.silverjob.common.TaskStatus;
import com.oldboss.silverjob.dto.CommentRequestDTO;
import com.oldboss.silverjob.entity.Comment;
import com.oldboss.silverjob.mapper.CommentMapper;
import com.oldboss.silverjob.mapper.TaskMapper;
import com.oldboss.silverjob.mapper.UserMapper;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.vo.CommentItemVO;
import com.oldboss.silverjob.vo.UserCommentStatsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 评价服务
 * 处理任务评价、评价查询、评分统计和评分回写等业务逻辑
 */
@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final AuthService authService;

    /**
     * 构造评价服务并注入所需依赖。
     * @param commentMapper 评价数据访问对象
     * @param taskMapper 任务数据访问对象
     * @param userMapper 用户数据访问对象
     * @param authService 认证服务
     */
    public CommentService(CommentMapper commentMapper, TaskMapper taskMapper, UserMapper userMapper, AuthService authService) {
        this.commentMapper = commentMapper;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.authService = authService;
    }

    /**
     * 提交任务评价
     * @param authorization Authorization 请求头
     * @param request 评价请求
     */
    @Transactional
    public void addComment(String authorization, CommentRequestDTO request) {
        CurrentUser currentUser = authService.requireUser(authorization);

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BizException("评分必须是1-5星");
        }

        if (request.getRevieweeId() == null) {
            throw new BizException("被评价人不能为空");
        }

        Long taskId = request.getTaskId();
        if (taskId == null) {
            throw new BizException("任务ID不能为空");
        }

        if (commentMapper.hasCommented(taskId, currentUser.getUserId())) {
            throw new BizException("您已评价过该任务");
        }

        Map<String, Object> task = taskMapper.selectTaskById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }

        String taskStatus = (String) task.get("status");
        if (!TaskStatus.DONE.equals(taskStatus)) {
            throw new BizException("任务完成后才能评价");
        }

        Long publisherId = toLong(task.get("publisher_id"));
        Long elderlyId = toLong(task.get("elderly_id"));
        Long reviewerId = currentUser.getUserId();
        Long revieweeId = request.getRevieweeId();

        if (reviewerId.equals(revieweeId)) {
            throw new BizException("不能评价自己");
        }
        if (publisherId == null || elderlyId == null) {
            throw new BizException("任务参与方信息不完整，无法评价");
        }
        boolean reviewerIsParticipant = reviewerId.equals(publisherId) || reviewerId.equals(elderlyId);
        if (!reviewerIsParticipant) {
            throw new BizException("只有任务参与方可以评价");
        }
        Long expectedRevieweeId = reviewerId.equals(publisherId) ? elderlyId : publisherId;
        if (!revieweeId.equals(expectedRevieweeId)) {
            throw new BizException("被评价人必须是该任务的另一方");
        }

        Map<String, Object> reviewee = userMapper.selectUserById(expectedRevieweeId);
        if (reviewee == null) {
            throw new BizException("被评价人不存在");
        }

        Comment comment = new Comment();
        comment.setTaskId(taskId);
        comment.setOrderId(taskId);
        comment.setReviewerId(currentUser.getUserId());
        comment.setReviewerName(getDisplayName(currentUser));
        comment.setReviewerRole(currentUser.getRoleType());
        comment.setRevieweeId(expectedRevieweeId);
        comment.setRevieweeName(getDisplayName(reviewee));
        comment.setRating(request.getRating());
        comment.setContent(request.getContent());
        comment.setCommentType(request.getCommentType());

        commentMapper.insert(comment);

        updateUserRating(expectedRevieweeId);
    }

    public boolean hasCommented(Long taskId, Long reviewerId) {
        if (taskId == null || reviewerId == null) {
            return false;
        }
        return commentMapper.hasCommented(taskId, reviewerId);
    }

    public Set<Long> findCommentedTaskIds(Set<Long> taskIds, Long reviewerId) {
        if (taskIds == null || taskIds.isEmpty() || reviewerId == null) {
            return Set.of();
        }
        return commentMapper.findCommentedTaskIds(taskIds, reviewerId);
    }

    /**
     * 获取用户收到的全部评价
     * @param userId 用户ID
     * @return 评价列表
     */
    public List<CommentItemVO> getUserComments(Long userId) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }
        return commentMapper.findByRevieweeId(userId);
    }

    /**
     * 分页获取用户收到的评价
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页评价结果
     */
    public PageResult<CommentItemVO> getUserComments(Long userId, Integer page, Integer pageSize) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }
        PageQuery pageQuery = PageUtils.normalize(page, pageSize);
        List<CommentItemVO> list = commentMapper.findByRevieweeIdPage(userId, pageQuery.getOffset(), pageQuery.getPageSize());
        long total = commentMapper.countByRevieweeId(userId);
        return PageUtils.buildPageResult(list, total, pageQuery);
    }

    /**
     * 获取指定任务下的全部评价
     * @param taskId 任务ID
     * @return 评价列表
     */
    public List<CommentItemVO> getTaskComments(Long taskId) {
        if (taskId == null) {
            throw new BizException("任务ID不能为空");
        }
        return commentMapper.findByTaskId(taskId);
    }

    /**
     * 分页获取指定任务下的评价
     * @param taskId 任务ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页评价结果
     */
    public PageResult<CommentItemVO> getTaskComments(Long taskId, Integer page, Integer pageSize) {
        if (taskId == null) {
            throw new BizException("任务ID不能为空");
        }
        PageQuery pageQuery = PageUtils.normalize(page, pageSize);
        List<CommentItemVO> list = commentMapper.findByTaskIdPage(taskId, pageQuery.getOffset(), pageQuery.getPageSize());
        long total = commentMapper.countByTaskId(taskId);
        return PageUtils.buildPageResult(list, total, pageQuery);
    }

    /**
     * 获取用户评价统计
     * @param userId 用户ID
     * @return 用户总分、评价条数和平均分
     */
    public UserCommentStatsVO getUserCommentStats(Long userId) {
        if (userId == null) {
            throw new BizException("用户ID��能为空");
        }

        Integer totalScore = commentMapper.sumRatingByUserId(userId);
        Integer commentCount = commentMapper.countByUserId(userId);

        UserCommentStatsVO stats = new UserCommentStatsVO();
        stats.setUserId(userId);
        stats.setTotalScore(totalScore != null ? totalScore : 0);
        stats.setCommentCount(commentCount != null ? commentCount : 0);

        if (commentCount != null && commentCount > 0 && totalScore != null) {
            stats.setAvgRating((double) totalScore / commentCount);
        } else {
            stats.setAvgRating(0.0);
        }

        return stats;
    }

    /**
     * 删除指定任务下的评价并重算相关用户评分
     * @param taskId 任务ID
     */
    @Transactional
    public void deleteCommentsByTaskId(Long taskId) {
        if (taskId == null) {
            throw new BizException("任务ID不能为空");
        }

        Set<Long> affectedUserIds = new LinkedHashSet<>(commentMapper.findRevieweeIdsByTaskId(taskId));
        commentMapper.deleteByTaskId(taskId);
        affectedUserIds.forEach(this::updateUserRating);
    }

    /**
     * 根据评论表中的累计数据回写用户评分字段。
     * @param userId 用户ID
     */
    private void updateUserRating(Long userId) {
        Integer totalScore = commentMapper.sumRatingByUserId(userId);
        Integer commentCount = commentMapper.countByUserId(userId);

        userMapper.updateUserRating(userId, totalScore != null ? totalScore : 0, commentCount != null ? commentCount : 0);
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
     * 从当前登录用户资料中提取显示名称。
     * @param user 当前登录用户
     * @return 显示名称
     */
    private String getDisplayName(CurrentUser user) {
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
     * 从数据库用户记录中提取显示名称。
     * @param user 用户记录
     * @return 显示名称
     */
    private String getDisplayName(Map<String, Object> user) {
        if (user == null) {
            return "";
        }
        String roleType = user.get("role_type") == null ? "" : String.valueOf(user.get("role_type"));
        String nickname = user.get("nickname") == null ? "" : String.valueOf(user.get("nickname")).trim();
        String realName = user.get("real_name") == null ? "" : String.valueOf(user.get("real_name")).trim();
        if ("elderly".equals(roleType)) {
            return realName.isEmpty() ? nickname : realName;
        }
        return realName.isEmpty() ? nickname : realName;
    }
}
