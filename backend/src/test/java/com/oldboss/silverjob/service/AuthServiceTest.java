package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.dto.LoginRequestDTO;
import com.oldboss.silverjob.dto.RegisterRequestDTO;
import com.oldboss.silverjob.entity.User;
import com.oldboss.silverjob.mapper.BindRelationMapper;
import com.oldboss.silverjob.mapper.UserMapper;
import com.oldboss.silverjob.mapper.UserSessionMapper;
import com.oldboss.silverjob.vo.LoginVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Mock
    private BindRelationMapper bindRelationMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserSessionMapper userSessionMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginRejectsUsingStoredBcryptHashAsPassword() {
        String storedHash = ENCODER.encode("correct-password");
        when(userMapper.selectByUsername("alice")).thenReturn(userRow(1L, "alice", "employer", storedHash, true));

        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("alice");
        request.setPassword(storedHash);

        BizException ex = assertThrows(BizException.class, () -> authService.login(request));
        assertEquals("账号或密码错误", ex.getMessage());
        verify(userSessionMapper, never()).insertSession(anyString(), anyLong(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void loginUpgradesLegacyPlaintextPasswordAfterSuccessfulLogin() {
        when(userMapper.selectByUsername("legacy")).thenReturn(userRow(2L, "legacy", "employer", "123456", true));

        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("legacy");
        request.setPassword("123456");

        LoginVO result = authService.login(request);

        assertEquals("employer", result.getCurrentRole());
        verify(userSessionMapper).insertSession(anyString(), eq(2L), eq("employer"), any(LocalDateTime.class));
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(userMapper).updatePassword(eq(2L), passwordCaptor.capture());
        assertNotEquals("123456", passwordCaptor.getValue());
        assertTrue(ENCODER.matches("123456", passwordCaptor.getValue()));
    }

    @Test
    void registerChildRequiresBindCode() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("child01");
        request.setPassword("123456");
        request.setRoleType("child");

        BizException ex = assertThrows(BizException.class, () -> authService.register(request));
        assertEquals("请输入老人绑定码", ex.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerChildWithValidBindCodeCompletesBindingAndLogin() {
        when(userMapper.selectByUsername("child01"))
                .thenReturn(null)
                .thenReturn(userRow(100L, "child01", "child", ENCODER.encode("123456"), true));
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        });
        when(bindRelationMapper.selectByBindCode("654321")).thenReturn(bindRelationRow(10L, false));
        when(bindRelationMapper.selectByChildId(100L)).thenReturn(null);
        when(bindRelationMapper.selectByElderlyId(10L)).thenReturn(bindRelationRow(10L, false));
        when(bindRelationMapper.confirmBind(100L, "654321")).thenReturn(1);

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername("child01");
        request.setPassword("123456");
        request.setRoleType("child");
        request.setBindCode("654321");
        request.setNickname("小王");

        LoginVO result = authService.register(request);

        assertEquals("child", result.getCurrentRole());
        verify(bindRelationMapper).confirmBind(100L, "654321");
        verify(userSessionMapper).insertSession(anyString(), eq(100L), eq("child"), any(LocalDateTime.class));
    }

    @Test
    void requireUserRejectsDisabledAccountAndDeletesOldToken() {
        Map<String, Object> disabledUser = userRow(3L, "disabled", "employer", ENCODER.encode("123456"), false);
        when(userSessionMapper.selectUserByToken("old-token")).thenReturn(disabledUser);

        BizException ex = assertThrows(BizException.class, () -> authService.requireUser("Bearer old-token"));

        assertEquals(403, ex.getCode());
        assertEquals("账号已被禁用，请联系管理员", ex.getMessage());
        verify(userSessionMapper).deleteSession("old-token");
    }

    private Map<String, Object> userRow(Long id, String username, String role, String passwordHash, boolean enabled) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("username", username);
        row.put("role_type", role);
        row.put("password_hash", passwordHash);
        row.put("enabled", enabled);
        row.put("nickname", username);
        row.put("real_name", username);
        return row;
    }

    private Map<String, Object> bindRelationRow(Long elderlyId, boolean confirmed) {
        Map<String, Object> row = new HashMap<>();
        row.put("elderly_user_id", elderlyId);
        row.put("confirmed", confirmed);
        row.put("code_created_at", Timestamp.from(Instant.now()));
        return row;
    }
}
