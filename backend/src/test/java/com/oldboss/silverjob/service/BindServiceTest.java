package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.PageResult;
import com.oldboss.silverjob.mapper.BindRelationMapper;
import com.oldboss.silverjob.mapper.TaskMapper;
import com.oldboss.silverjob.mapper.UserMapper;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.vo.BindOrderItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BindServiceTest {

    @Mock
    private BindRelationMapper bindRelationMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthService authService;
    @Mock
    private UserService userService;

    @InjectMocks
    private BindService bindService;

    @Test
    void bindOrderListUsesStatusPaginationForChild() {
        when(authService.requireUser("Bearer token")).thenReturn(currentUser(8L, "child", "子女A"));
        when(bindRelationMapper.selectByChildId(8L)).thenReturn(relationRow(15L));
        when(taskMapper.selectOrdersByElderlyIdPageAndStatus(15L, "pending_payment", 0, 10))
                .thenReturn(List.of(orderRow(31L, "pending_payment")));
        when(taskMapper.countOrdersByElderlyIdAndStatus(15L, "pending_payment")).thenReturn(1L);

        PageResult<BindOrderItemVO> page = bindService.bindOrderList("Bearer token", 1, 10, "pending_payment");

        assertEquals(1, page.getList().size());
        assertEquals(1L, page.getTotal());
        verify(taskMapper).selectOrdersByElderlyIdPageAndStatus(15L, "pending_payment", 0, 10);
        verify(taskMapper).countOrdersByElderlyIdAndStatus(15L, "pending_payment");
        verify(taskMapper, never()).selectOrdersByElderlyIdPage(15L, 0, 10);
    }

    private CurrentUser currentUser(Long id, String role, String name) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", id);
        profile.put("nickname", name);
        profile.put("realName", name);
        return new CurrentUser("token", role, profile);
    }

    private Map<String, Object> relationRow(Long elderlyId) {
        Map<String, Object> row = new HashMap<>();
        row.put("elderly_user_id", elderlyId);
        return row;
    }

    private Map<String, Object> orderRow(Long id, String status) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("title", "绑定订单");
        row.put("task_type", "help:陪伴");
        row.put("address", "测试地址");
        row.put("time_text", "2026-04-22 10:00-11:00");
        row.put("salary", 120);
        row.put("content", "测试内容");
        row.put("publisher_id", 99L);
        row.put("publisher_name", "雇主A");
        row.put("elderly_id", 15L);
        row.put("elderly_name", "老人A");
        row.put("status", status);
        row.put("publisher_mobile", "13800000000");
        row.put("settle_text", "-");
        return row;
    }
}
