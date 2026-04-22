package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.common.TaskStatus;
import com.oldboss.silverjob.dto.CommentRequestDTO;
import com.oldboss.silverjob.entity.Comment;
import com.oldboss.silverjob.mapper.CommentMapper;
import com.oldboss.silverjob.mapper.TaskMapper;
import com.oldboss.silverjob.mapper.UserMapper;
import com.oldboss.silverjob.model.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthService authService;

    @InjectMocks
    private CommentService commentService;

    @Test
    void addCommentRejectsNonDoneTask() {
        when(authService.requireUser("Bearer token")).thenReturn(currentUser(1L, "employer", "雇主A"));
        when(commentMapper.hasCommented(11L, 1L)).thenReturn(false);
        when(taskMapper.selectTaskById(11L)).thenReturn(taskRow(11L, TaskStatus.WORKING, 1L, 2L));

        CommentRequestDTO request = new CommentRequestDTO();
        request.setTaskId(11L);
        request.setRevieweeId(2L);
        request.setRating(5);

        BizException ex = assertThrows(BizException.class, () -> commentService.addComment("Bearer token", request));
        assertEquals("任务完成后才能评价", ex.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void addCommentUsesServerResolvedRevieweeNameAndUpdatesRating() {
        when(authService.requireUser("Bearer token")).thenReturn(currentUser(1L, "employer", "雇主A"));
        when(commentMapper.hasCommented(12L, 1L)).thenReturn(false);
        when(taskMapper.selectTaskById(12L)).thenReturn(taskRow(12L, TaskStatus.DONE, 1L, 2L));
        when(userMapper.selectUserById(2L)).thenReturn(revieweeRow(2L, "elderly", "老人昵称", "张三"));
        when(commentMapper.sumRatingByUserId(2L)).thenReturn(5);
        when(commentMapper.countByUserId(2L)).thenReturn(1);

        CommentRequestDTO request = new CommentRequestDTO();
        request.setTaskId(12L);
        request.setRevieweeId(2L);
        request.setRating(5);
        request.setContent("服务很好");
        request.setCommentType("employer_rate_elderly");

        commentService.addComment("Bearer token", request);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(commentCaptor.capture());
        Comment saved = commentCaptor.getValue();
        assertEquals(2L, saved.getRevieweeId());
        assertEquals("张三", saved.getRevieweeName());
        assertEquals("雇主A", saved.getReviewerName());
        verify(userMapper).updateUserRating(2L, 5, 1);
    }

    private CurrentUser currentUser(Long id, String role, String name) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", id);
        profile.put("nickname", name);
        profile.put("realName", name);
        return new CurrentUser("token", role, profile);
    }

    private Map<String, Object> taskRow(Long id, String status, Long publisherId, Long elderlyId) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("status", status);
        row.put("publisher_id", publisherId);
        row.put("elderly_id", elderlyId);
        return row;
    }

    private Map<String, Object> revieweeRow(Long id, String role, String nickname, String realName) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("role_type", role);
        row.put("nickname", nickname);
        row.put("real_name", realName);
        return row;
    }
}
