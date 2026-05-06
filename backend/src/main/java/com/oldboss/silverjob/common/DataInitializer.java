package com.oldboss.silverjob.common;

import com.oldboss.silverjob.entity.User;
import com.oldboss.silverjob.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.nickname}")
    private String adminNickname;

    @Value("${app.admin.real-name}")
    private String adminRealName;

    @Value("${app.admin.mobile}")
    private String adminMobile;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminUsername)) {
            log.warn("未检测到管理员配置，跳过初始化");
            return;
        }

        Map existing = userMapper.selectByUsername(adminUsername);

        if (existing == null) {
            log.info(">>>> [数据初始化] 管理员账号不存在，准备自动创建...");

            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRoleType("admin");
            admin.setNickname(adminNickname);
            admin.setRealName(adminRealName);
            admin.setMobile(adminMobile);
            admin.setEnabled(true);

            try {
                userMapper.insertUser(admin);
                log.info(">>>> [数据初始化] 管理员账号 [{}] 创建成功！", adminUsername);
            } catch (Exception e) {
                log.error(">>>> [数据初始化] 创建管理员异常: {}", e.getMessage());
            }
        } else {
            String role = (String) existing.get("role_type");
            if ("admin".equals(role)) {
                log.info(">>>> [数据初始化] 管理员账号 [{}] 已存在，跳过", adminUsername);
            } else {
                log.warn(">>>> [数据初始化] 用户名 [{}] 已被其他角色占用，跳过", adminUsername);
            }
        }
    }
}