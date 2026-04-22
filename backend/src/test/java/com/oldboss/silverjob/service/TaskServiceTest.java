package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.PageResult;
import com.oldboss.silverjob.mapper.BindRelationMapper;
import com.oldboss.silverjob.mapper.TaskMapper;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.vo.TaskItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private BindRelationMapper bindRelationMapper;
    @Mock
    private AuthService authService;
    @Mock
    private UserService userService;
    @Mock
    private CommentService commentService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void taskListUsesPublisherStatusPaginationForEmployer() {
        when(authService.requireUser("Bearer token")).thenReturn(currentUser(5L, "employer", "雇主A"));
        when(taskMapper.selectTasksByPublisherIdPageAndStatus(5L, "waiting", 0, 10))
                .thenReturn(List.of(taskRow(21L, "waiting")));
        when(taskMapper.countTasksByPublisherIdAndStatus(5L, "waiting")).thenReturn(1L);

        PageResult<TaskItemVO> page = taskService.taskList("Bearer token", 1, 10, "waiting");

        assertEquals(1, page.getList().size());
        assertEquals(1L, page.getTotal());
        verify(taskMapper).selectTasksByPublisherIdPageAndStatus(5L, "waiting", 0, 10);
        verify(taskMapper).countTasksByPublisherIdAndStatus(5L, "waiting");
        verify(taskMapper, never()).selectTasksByPublisherIdPage(5L, 0, 10);
    }

    @Test
    void applyTaskRequiresApprovedHealthReport() {
        when(authService.requireUser("Bearer token")).thenReturn(currentUser(9L, "elderly", "老人A"));
        when(userService.isHealthReportApproved(9L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.applyTask("Bearer token", 88L));

        assertEquals("您的体检报告还未通过审核，无法申请接单", ex.getMessage());
        verify(taskMapper, never()).selectTaskById(anyLong());
        verify(taskMapper, never()).updateTaskToApplying(anyLong(), anyLong(), anyString());
    }

    @Test
    void deleteOrderByAdminDeletesCommentsBeforeTask() {
        when(authService.requireUser("Bearer admin")).thenReturn(currentUser(1L, "admin", "管理员"));
        when(taskMapper.selectTaskById(66L)).thenReturn(taskRow(66L, "done"));

        taskService.deleteOrder("Bearer admin", 66L);

        verify(commentService).deleteCommentsByTaskId(66L);
        verify(taskMapper).deleteTaskById(66L);
    }

    @Test
    void finishOrderMovesWorkingTaskToPendingPayment() {
        when(authService.requireUser("Bearer elderly")).thenReturn(currentUser(9L, "elderly", "老人A"));
        when(taskMapper.selectTaskById(77L)).thenReturn(taskRowForOrder(77L, "working", 5L, 9L));

        taskService.finishOrder("Bearer elderly", 77L);

        verify(taskMapper).updateTaskToPendingPayment(77L);
    }

    @Test
    void payOrderMovesPendingPaymentTaskToDone() {
        when(authService.requireUser("Bearer employer")).thenReturn(currentUser(5L, "employer", "雇主A"));
        when(taskMapper.selectTaskById(78L)).thenReturn(taskRowForOrder(78L, "pending_payment", 5L, 9L));

        taskService.payOrder("Bearer employer", 78L);

        verify(taskMapper).updateTaskToDone(78L);
    }

    private CurrentUser currentUser(Long id, String role, String name) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", id);
        profile.put("nickname", name);
        profile.put("realName", name);
        return new CurrentUser("token", role, profile);
    }

    private Map<String, Object> taskRow(Long id, String status) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("title", "测试任务");
        row.put("task_type", "skill:陪诊");
        row.put("address", "测试地址");
        row.put("time_text", "2026-04-22 10:00-11:00");
        row.put("salary", 100);
        row.put("content", "测试内容");
        row.put("publisher_id", 5L);
        row.put("publisher_name", "雇主A");
        row.put("status", status);
        row.put("publisher_total_score", 0);
        row.put("publisher_comment_count", 0);
        row.put("elderly_total_score", 0);
        row.put("elderly_comment_count", 0);
        return row;
    }

    private Map<String, Object> taskRowForOrder(Long id, String status, Long publisherId, Long elderlyId) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("status", status);
        row.put("publisher_id", publisherId);
        row.put("elderly_id", elderlyId);
        return row;
    }
}
