package com.oldboss.silverjob.service;

import com.oldboss.silverjob.common.BizException;
import com.oldboss.silverjob.model.CurrentUser;
import com.oldboss.silverjob.payload.BindConfirmRequest;
import com.oldboss.silverjob.payload.LoginRequest;
import com.oldboss.silverjob.payload.MessageSendRequest;
import com.oldboss.silverjob.payload.TaskFormRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DemoStateService {

    private static final DateTimeFormatter TOKEN_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter MESSAGE_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public DemoStateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Map<String, Object> login(LoginRequest request) {
        String role = safe(request.getRoleType());
        if (role.isEmpty()) {
            throw new BizException("请选择身份");
        }

        UserRecord user = findUserByRole(role);
        if (user == null) {
            throw new BizException("请选择身份");
        }

        String oldDisplayName = displayName(user);
        String nickname = safe(request.getNickname());
        String mobile = safe(request.getMobile());
        if (!nickname.isEmpty() || !mobile.isEmpty()) {
            String newNickname = nickname.isEmpty() ? user.nickname : nickname;
            String newRealName = resolveRealName(role, user, nickname);
            String newMobile = mobile.isEmpty() ? user.mobile : mobile;
            jdbcTemplate.update(
                    "update app_user set nickname = ?, real_name = ?, mobile = ?, updated_at = current_timestamp where id = ?",
                    newNickname,
                    newRealName,
                    emptyToNull(newMobile),
                    user.id
            );
            user = findUserById(user.id);
            syncDisplayName(role, oldDisplayName, displayName(user));
        }

        String token = "demo-" + role + "-" + TOKEN_TIME.format(LocalDateTime.now()) + "-"
                + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update(
                "insert into user_session(token, user_id, role_type, expired_at) values (?, ?, ?, null)",
                token, user.id, role
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("currentRole", role);
        result.put("userInfo", toProfile(user));
        return result;
    }

    @Transactional
    public void logout(String authorization) {
        String token = extractToken(authorization);
        if (!token.isEmpty()) {
            jdbcTemplate.update("delete from user_session where token = ?", token);
        }
    }

    public Map<String, Object> currentUser(String authorization) {
        CurrentUser currentUser = requireUser(authorization);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentRole", currentUser.getRoleType());
        result.put("roleType", currentUser.getRoleType());
        result.put("userInfo", currentUser.getProfile());
        return result;
    }

    public Map<String, Object> userInfo(String authorization) {
        CurrentUser currentUser = requireUser(authorization);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", currentUser.getRoleType());
        result.put("roleText", roleText(currentUser.getRoleType()));
        result.put("profile", currentUser.getProfile());
        return result;
    }

    @Transactional
    public void saveUserInfo(String authorization, Map<String, Object> payload) {
        CurrentUser currentUser = requireUser(authorization);
        UserRecord existing = findUserByRole(currentUser.getRoleType());
        if (existing == null) {
            throw new BizException("用户不存在");
        }

        String oldDisplayName = displayName(existing);
        Map<String, Object> merged = new LinkedHashMap<>(toProfile(existing));
        payload.forEach((key, value) -> {
            if (value != null) {
                merged.put(key, value);
            }
        });

        String nickname = str(merged.get("nickname"));
        String realName = resolveProfileRealName(existing.roleType, merged, existing);
        jdbcTemplate.update(
                "update app_user set nickname = ?, real_name = ?, mobile = ?, gender = ?, age = ?, city = ?, "
                        + "health_desc = ?, skill_tags = ?, emergency_contact = ?, emergency_mobile = ?, relation = ?, "
                        + "remark = ?, note = ?, updated_at = current_timestamp where id = ?",
                emptyToNull(nickname),
                emptyToNull(realName),
                emptyToNull(str(merged.get("mobile"))),
                emptyToNull(str(merged.get("gender"))),
                parseShort(merged.get("age")),
                emptyToNull(str(merged.get("city"))),
                emptyToNull(str(merged.get("healthDesc"))),
                emptyToNull(str(merged.get("skillTags"))),
                emptyToNull(str(merged.get("emergencyContact"))),
                emptyToNull(str(merged.get("emergencyMobile"))),
                emptyToNull(str(merged.get("relation"))),
                emptyToNull(str(merged.get("remark"))),
                emptyToNull(str(merged.get("note"))),
                existing.id
        );

        syncDisplayName(existing.roleType, oldDisplayName, displayName(findUserById(existing.id)));
    }

    public List<Map<String, Object>> taskList(String authorization) {
        requireUser(authorization);
        return jdbcTemplate.query(
                "select id, task_type, title, address, time_text, salary, content, publisher_name, publisher_role, status, "
                        + "coalesce(elderly_name, '') as elderly_name "
                        + "from task order by id asc",
                (rs, rowNum) -> mapTask(rs)
        );
    }

    public Map<String, Object> taskDetail(String authorization, Long id) {
        requireUser(authorization);
        Map<String, Object> task = findTask(id);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        Map<String, Object> order = findOrderByTaskId(id);
        if (order != null) {
            task.put("order", order);
        }
        return task;
    }

    @Transactional
    public void addTask(String authorization, TaskFormRequest request) {
        CurrentUser currentUser = requireUser(authorization);
        if (!Arrays.asList("employer", "child").contains(currentUser.getRoleType())) {
            throw new BizException("当前身份不能发布任务");
        }
        if (safe(request.getTitle()).isEmpty() || safe(request.getType()).isEmpty() || safe(request.getAddress()).isEmpty()
                || request.getSalary() == null || safe(request.getTimeText()).isEmpty() || safe(request.getContent()).isEmpty()) {
            throw new BizException("请把任务信息填写完整");
        }

        long publisherId = findUserByRole(currentUser.getRoleType()).id;
        jdbcTemplate.update(
                "insert into task(task_type, title, address, time_text, salary, content, publisher_id, publisher_name, "
                        + "publisher_role, status, elderly_id, elderly_name) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, null, null)",
                request.getType().trim(),
                request.getTitle().trim(),
                request.getAddress().trim(),
                request.getTimeText().trim(),
                request.getSalary(),
                request.getContent().trim(),
                publisherId,
                displayName(findUserById(publisherId)),
                currentUser.getRoleType(),
                "waiting"
        );
    }

    @Transactional
    public void grabTask(String authorization, Long id) {
        CurrentUser currentUser = requireUser(authorization);
        if (!"elderly".equals(currentUser.getRoleType())) {
            throw new BizException("只有老人用户可以接单");
        }

        UserRecord elderly = findUserByRole("elderly");
        Map<String, Object> task = findTask(id);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        if (!"waiting".equals(task.get("status"))) {
            throw new BizException("当前任务不能接单");
        }

        String elderlyName = displayName(elderly);
        int updated = jdbcTemplate.update(
                "update task set status = 'working', elderly_id = ?, elderly_name = ?, updated_at = current_timestamp "
                        + "where id = ? and status = 'waiting'",
                elderly.id, elderlyName, id
        );
        if (updated == 0) {
            throw new BizException("当前任务不能接单");
        }

        jdbcTemplate.update(
                "insert into task_order(task_id, order_code, title, address, salary, employer_id, employer_name, elderly_id, "
                        + "elderly_name, start_time, finish_time, status, settle_text) "
                        + "select id, ?, title, address, salary, publisher_id, publisher_name, ?, ?, current_timestamp, null, "
                        + "'working', '待结算' from task where id = ?",
                buildOrderCode(),
                elderly.id,
                elderlyName,
                id
        );
    }

    public List<Map<String, Object>> orderList(String authorization) {
        CurrentUser currentUser = requireUser(authorization);
        String role = currentUser.getRoleType();
        UserRecord elderly = findUserByRole("elderly");
        UserRecord employer = findUserByRole("employer");

        String sql = "select id, task_id, order_code, title, address, salary, employer_name, elderly_name, start_time, "
                + "finish_time, status, settle_text from task_order ";
        Object[] args;
        if ("admin".equals(role)) {
            sql += "order by id desc";
            args = new Object[0];
        } else if ("employer".equals(role)) {
            sql += "where employer_id = ? order by id desc";
            args = new Object[]{employer == null ? -1L : employer.id};
        } else if ("elderly".equals(role) || "child".equals(role)) {
            sql += "where elderly_id = ? order by id desc";
            args = new Object[]{elderly == null ? -1L : elderly.id};
        } else {
            return List.of();
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapOrder(rs), args);
    }

    @Transactional
    public void finishOrder(String authorization, Long taskId) {
        CurrentUser currentUser = requireUser(authorization);
        if (!Arrays.asList("employer", "child").contains(currentUser.getRoleType())) {
            throw new BizException("当前身份不能完成任务");
        }
        if (findOrderByTaskId(taskId) == null) {
            throw new BizException("订单不存在");
        }
        jdbcTemplate.update("update task set status = 'done', updated_at = current_timestamp where id = ?", taskId);
        jdbcTemplate.update(
                "update task_order set status = 'done', finish_time = current_timestamp, settle_text = '模拟已结算', "
                        + "updated_at = current_timestamp where task_id = ?",
                taskId
        );
    }

    @Transactional
    public void cancelOrder(String authorization, Long taskId) {
        requireUser(authorization);
        if (findOrderByTaskId(taskId) == null) {
            throw new BizException("订单不存在");
        }
        jdbcTemplate.update("update task set status = 'cancelled', updated_at = current_timestamp where id = ?", taskId);
        jdbcTemplate.update(
                "update task_order set status = 'cancelled', finish_time = current_timestamp, settle_text = '已取消，无需结算', "
                        + "updated_at = current_timestamp where task_id = ?",
                taskId
        );
    }

    @Transactional
    public Map<String, Object> createBindCode(String authorization) {
        CurrentUser currentUser = requireUser(authorization);
        if (!"elderly".equals(currentUser.getRoleType())) {
            throw new BizException("只有老人用户可以生成绑定码");
        }

        UserRecord elderly = findUserByRole("elderly");
        UserRecord child = findUserByRole("child");
        if (elderly == null || child == null) {
            throw new BizException("绑定用户不存在");
        }

        String code = String.valueOf((int) (100000 + Math.random() * 900000));
        BindRelationRecord relation = findBindRelation();
        if (relation == null) {
            jdbcTemplate.update(
                    "insert into bind_relation(elderly_user_id, child_user_id, bind_code, confirmed, confirmed_at) "
                            + "values (?, ?, ?, false, null)",
                    elderly.id, child.id, code
            );
        } else {
            jdbcTemplate.update(
                    "update bind_relation set bind_code = ?, confirmed = false, confirmed_at = null where id = ?",
                    code, relation.id
            );
        }
        return bindInfoResult();
    }

    @Transactional
    public void confirmBind(String authorization, BindConfirmRequest request) {
        CurrentUser currentUser = requireUser(authorization);
        if (!"child".equals(currentUser.getRoleType())) {
            throw new BizException("只有子女用户可以确认绑定");
        }
        String code = safe(request.getCode());
        if (code.isEmpty()) {
            throw new BizException("请输入绑定码");
        }

        int updated = jdbcTemplate.update(
                "update bind_relation set confirmed = true, confirmed_at = current_timestamp where child_user_id = ? and bind_code = ?",
                findUserByRole("child").id,
                code
        );
        if (updated == 0) {
            throw new BizException("绑定码不正确");
        }
    }

    public Map<String, Object> bindInfo(String authorization) {
        CurrentUser currentUser = requireUser(authorization);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bindInfo", bindInfoResult());
        UserRecord elderly = findUserByRole("elderly");
        result.put("elderlyProfile", elderly == null ? null : toProfile(elderly));
        result.put("currentOrder", currentWorkingOrder());
        if ("admin".equals(currentUser.getRoleType())) {
            return result;
        }
        return result;
    }

    public List<Map<String, Object>> bindOrderList(String authorization) {
        CurrentUser currentUser = requireUser(authorization);
        if (!Arrays.asList("child", "admin").contains(currentUser.getRoleType())) {
            throw new BizException("当前身份不能查看绑定订单");
        }
        UserRecord elderly = findUserByRole("elderly");
        if (elderly == null) {
            return List.of();
        }
        return jdbcTemplate.query(
                "select id, task_id, order_code, title, address, salary, employer_name, elderly_name, start_time, "
                        + "finish_time, status, settle_text from task_order where elderly_id = ? order by id desc",
                (rs, rowNum) -> mapOrder(rs),
                elderly.id
        );
    }

    public List<Map<String, Object>> messageList(String authorization) {
        requireUser(authorization);
        BindRelationRecord relation = findBindRelation();
        if (relation == null) {
            return List.of();
        }
        return jdbcTemplate.query(
                "select id, sender_role, content, sent_at from message where bind_relation_id = ? order by id asc",
                (rs, rowNum) -> mapMessage(rs),
                relation.id
        );
    }

    @Transactional
    public void sendMessage(String authorization, MessageSendRequest request) {
        CurrentUser currentUser = requireUser(authorization);
        String content = safe(request.getContent());
        if (content.isEmpty()) {
            throw new BizException("请输入留言内容");
        }

        BindRelationRecord relation = findBindRelation();
        if (relation == null) {
            throw new BizException("绑定关系不存在");
        }

        String senderRole = safe(request.getSenderRole());
        if (senderRole.isEmpty()) {
            senderRole = currentUser.getRoleType();
        }

        UserRecord sender = findUserByRole(senderRole);
        if (sender == null) {
            throw new BizException("发送人不存在");
        }

        jdbcTemplate.update(
                "insert into message(bind_relation_id, sender_user_id, sender_role, content, sent_at) values (?, ?, ?, ?, current_timestamp)",
                relation.id, sender.id, senderRole, content
        );
    }

    public CurrentUser requireUser(String authorization) {
        String token = extractToken(authorization);
        if (token.isEmpty()) {
            throw new BizException(401, "未登录或登录已失效");
        }

        UserRecord user = queryOne(
                "select u.* from user_session s join app_user u on u.id = s.user_id where s.token = ? order by s.id desc limit 1",
                this::mapUser,
                token
        );
        if (user == null) {
            throw new BizException(401, "未登录或登录已失效");
        }
        return new CurrentUser(token, user.roleType, toProfile(user));
    }

    private Map<String, Object> currentWorkingOrder() {
        return queryOne(
                "select id, task_id, order_code, title, address, salary, employer_name, elderly_name, start_time, finish_time, "
                        + "status, settle_text from task_order where status = 'working' order by id desc limit 1",
                this::mapOrder
        );
    }

    private Map<String, Object> bindInfoResult() {
        BindRelationRecord relation = findBindRelation();
        UserRecord elderly = findUserByRole("elderly");
        UserRecord child = findUserByRole("child");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", relation == null ? "" : relation.bindCode);
        result.put("confirmed", relation != null && relation.confirmed);
        result.put("elderlyName", elderly == null ? "" : displayName(elderly));
        result.put("childName", child == null ? "" : displayName(child));
        return result;
    }

    private Map<String, Object> findTask(Long id) {
        return queryOne(
                "select id, task_type, title, address, time_text, salary, content, publisher_name, publisher_role, status, "
                        + "coalesce(elderly_name, '') as elderly_name from task where id = ?",
                this::mapTask,
                id
        );
    }

    private Map<String, Object> findOrderByTaskId(Long taskId) {
        return queryOne(
                "select id, task_id, order_code, title, address, salary, employer_name, elderly_name, start_time, finish_time, "
                        + "status, settle_text from task_order where task_id = ?",
                this::mapOrder,
                taskId
        );
    }

    private UserRecord findUserByRole(String role) {
        return queryOne("select * from app_user where role_type = ? order by id asc limit 1", this::mapUser, role);
    }

    private UserRecord findUserById(Long id) {
        return queryOne("select * from app_user where id = ?", this::mapUser, id);
    }

    private BindRelationRecord findBindRelation() {
        return queryOne("select * from bind_relation order by id asc limit 1", this::mapBindRelation);
    }

    private void syncDisplayName(String role, String oldName, String newName) {
        if (oldName.equals(newName)) {
            return;
        }
        if ("elderly".equals(role)) {
            jdbcTemplate.update("update task set elderly_name = ? where elderly_name = ?", newName, oldName);
            jdbcTemplate.update("update task_order set elderly_name = ? where elderly_name = ?", newName, oldName);
        }
        if ("employer".equals(role) || "child".equals(role)) {
            jdbcTemplate.update(
                    "update task set publisher_name = ? where publisher_name = ? and publisher_role = ?",
                    newName, oldName, role
            );
            jdbcTemplate.update("update task_order set employer_name = ? where employer_name = ?", newName, oldName);
        }
    }

    private Map<String, Object> mapTask(ResultSet rs) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", rs.getLong("id"));
        result.put("type", rs.getString("task_type"));
        result.put("title", rs.getString("title"));
        result.put("address", rs.getString("address"));
        result.put("timeText", rs.getString("time_text"));
        result.put("salary", rs.getInt("salary"));
        result.put("content", rs.getString("content"));
        result.put("publisherName", rs.getString("publisher_name"));
        result.put("publisherRole", rs.getString("publisher_role"));
        result.put("publisherRoleType", rs.getString("publisher_role"));
        result.put("status", rs.getString("status"));
        result.put("elderlyName", rs.getString("elderly_name"));
        result.put("taskType", rs.getString("task_type"));
        return result;
    }

    private Map<String, Object> mapOrder(ResultSet rs) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", rs.getLong("id"));
        result.put("taskId", rs.getLong("task_id"));
        result.put("code", rs.getString("order_code"));
        result.put("title", rs.getString("title"));
        result.put("address", rs.getString("address"));
        result.put("salary", rs.getInt("salary"));
        result.put("employerName", rs.getString("employer_name"));
        result.put("elderlyName", rs.getString("elderly_name"));
        result.put("startTime", formatTimestamp(rs.getTimestamp("start_time")));
        result.put("finishTime", formatTimestamp(rs.getTimestamp("finish_time")));
        result.put("status", rs.getString("status"));
        result.put("settleText", rs.getString("settle_text"));
        return result;
    }

    private Map<String, Object> mapMessage(ResultSet rs) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", rs.getLong("id"));
        result.put("senderRole", rs.getString("sender_role"));
        result.put("senderText", roleText(rs.getString("sender_role")));
        result.put("content", rs.getString("content"));
        result.put("time", formatMessageTime(rs.getTimestamp("sent_at")));
        return result;
    }

    private UserRecord mapUser(ResultSet rs) throws SQLException {
        return new UserRecord(
                rs.getLong("id"),
                rs.getString("role_type"),
                rs.getString("nickname"),
                rs.getString("real_name"),
                rs.getString("mobile"),
                rs.getString("gender"),
                rs.getObject("age") == null ? null : rs.getShort("age"),
                rs.getString("city"),
                rs.getString("health_desc"),
                rs.getString("skill_tags"),
                rs.getString("emergency_contact"),
                rs.getString("emergency_mobile"),
                rs.getString("relation"),
                rs.getString("remark"),
                rs.getString("note")
        );
    }

    private BindRelationRecord mapBindRelation(ResultSet rs) throws SQLException {
        return new BindRelationRecord(
                rs.getLong("id"),
                rs.getLong("elderly_user_id"),
                rs.getLong("child_user_id"),
                rs.getString("bind_code"),
                rs.getBoolean("confirmed")
        );
    }

    private <T> T queryOne(String sql, RowMapperWithSqlException<T> mapper, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapper.map(rs), args);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private Map<String, Object> toProfile(UserRecord user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nickname", nullToEmpty(user.nickname));
        result.put("mobile", nullToEmpty(user.mobile));

        if ("elderly".equals(user.roleType)) {
            result.put("realName", nullToEmpty(user.realName));
            result.put("age", user.age == null ? "" : String.valueOf(user.age));
            result.put("gender", nullToEmpty(user.gender));
            result.put("city", nullToEmpty(user.city));
            result.put("healthDesc", nullToEmpty(user.healthDesc));
            result.put("skillTags", nullToEmpty(user.skillTags));
            result.put("emergencyContact", nullToEmpty(user.emergencyContact));
            result.put("emergencyMobile", nullToEmpty(user.emergencyMobile));
        } else {
            result.put("name", nullToEmpty(primaryName(user)));
            if ("child".equals(user.roleType)) {
                result.put("relation", nullToEmpty(user.relation));
                result.put("note", nullToEmpty(user.note));
            }
            if ("employer".equals(user.roleType)) {
                result.put("remark", nullToEmpty(user.remark));
            }
        }
        return result;
    }

    private String roleText(String role) {
        if ("elderly".equals(role)) {
            return "老人";
        }
        if ("employer".equals(role)) {
            return "雇主";
        }
        if ("child".equals(role)) {
            return "子女";
        }
        if ("admin".equals(role)) {
            return "管理员";
        }
        return role;
    }

    private String displayName(UserRecord user) {
        if (user == null) {
            return "";
        }
        if ("elderly".equals(user.roleType)) {
            return nullToEmpty(user.realName).isEmpty() ? nullToEmpty(user.nickname) : nullToEmpty(user.realName);
        }
        return primaryName(user);
    }

    private String primaryName(UserRecord user) {
        return nullToEmpty(user.realName).isEmpty() ? nullToEmpty(user.nickname) : nullToEmpty(user.realName);
    }

    private String resolveRealName(String role, UserRecord user, String nickname) {
        if (nickname.isEmpty()) {
            return user.realName;
        }
        return nickname;
    }

    private String resolveProfileRealName(String role, Map<String, Object> merged, UserRecord existing) {
        if ("elderly".equals(role)) {
            String realName = str(merged.get("realName"));
            return realName.isEmpty() ? existing.realName : realName;
        }
        String name = str(merged.get("name"));
        if (!name.isEmpty()) {
            return name;
        }
        return existing.realName;
    }

    private String buildOrderCode() {
        return "#" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%02d", jdbcTemplate.queryForObject("select coalesce(max(id), 0) + 1 from task_order", Long.class));
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(ORDER_TIME);
    }

    private String formatMessageTime(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(MESSAGE_TIME);
    }

    private String extractToken(String authorization) {
        if (authorization == null) {
            return "";
        }
        return authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : authorization.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Short parseShort(Object value) {
        String text = str(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Short.valueOf(text);
        } catch (NumberFormatException ex) {
            throw new BizException("年龄格式不正确");
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    @FunctionalInterface
    private interface RowMapperWithSqlException<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private record UserRecord(
            Long id,
            String roleType,
            String nickname,
            String realName,
            String mobile,
            String gender,
            Short age,
            String city,
            String healthDesc,
            String skillTags,
            String emergencyContact,
            String emergencyMobile,
            String relation,
            String remark,
            String note
    ) {
    }

    private record BindRelationRecord(
            Long id,
            Long elderlyUserId,
            Long childUserId,
            String bindCode,
            boolean confirmed
    ) {
    }
}
