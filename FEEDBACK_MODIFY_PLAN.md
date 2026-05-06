# Feedback 功能完整修改方案

## 一、SQL 文件修改

### 1. sql/schema.sql - 新增 feedback 表
在文件末尾（第140行后）添加：

```sql
-- 反馈表
CREATE TABLE IF NOT EXISTS feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    feedback_type VARCHAR(50),
    related_order_id BIGINT,
    status VARCHAR(20) DEFAULT 'pending',
    admin_reply TEXT,
    handled_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feedback_user_id ON feedback(user_id);
CREATE INDEX IF NOT EXISTS idx_feedback_status ON feedback(status);
```

### 2. sql/seed.sql - 补全序列重置
在文件末尾（第55行后）添加：

```sql
SELECT setval('feedback_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM feedback), 1), true);
```

---

## 二、Java 文件修改

### 1. Feedback.java - 完整替换
路径：`src/main/java/com/oldboss/silverjob/entity/Feedback.java`

需要添加的字段：
- `feedbackType` (String, @TableField("feedback_type"))
- `adminReply` (String, @TableField("admin_reply"))
- `handledBy` (Long, @TableField("handled_by"))
- `updatedAt` (LocalDateTime, @TableField(fill = FieldFill.INSERT_UPDATE))

### 2. FeedbackItemVO.java - 添加字段
路径：`src/main/java/com/oldboss/silverjob/vo/FeedbackItemVO.java`

需要添加的字段：
- `adminReply` (String)
- `handledBy` (Long)
- `updatedAt` (LocalDateTime)

### 3. FeedbackMapper.java - 完整替换
路径：`src/main/java/com/oldboss/silverjob/mapper/FeedbackMapper.java`

替换为：
```java
package com.oldboss.silverjob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oldboss.silverjob.entity.Feedback;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    @Select("""
        SELECT f.*, 
               COALESCE(u.real_name, u.nickname) as user_name, 
               t.title as order_title 
        FROM feedback f 
        LEFT JOIN app_user u ON f.user_id = u.id 
        LEFT JOIN task t ON f.related_order_id = t.id 
        ORDER BY f.id DESC
    """)
    List<Map<String, Object>> selectAllWithDetail();

    @Select("""
        SELECT f.*, t.title as order_title 
        FROM feedback f 
        LEFT JOIN task t ON f.related_order_id = t.id 
        WHERE f.user_id = #{userId} 
        ORDER BY f.id DESC
    """)
    List<Map<String, Object>> selectByUserIdWithDetail(@Param("userId") Long userId);
}
```

### 4. FeedbackService.java - 完整替换
路径：`src/main/java/com/oldboss/silverjob/service/FeedbackService.java`

需要修改：
- 删除旧的 `insertFeedback` 调用，改用 `feedbackMapper.insert(feedback)`
- 删除 `updateStatus` 方法，改为使用 MyBatis Plus 的 `updateById`
- 新增 `handleFeedback` 方法处理管理员回信
- 修改 `convertToVO` 方法，增加对 `adminReply`, `feedbackType`, `handledBy` 的处理
- 方法名改为：`getAdminList`, `getMyList`

核心新增方法：
```java
@Transactional
public void handleFeedback(String token, Long id, String status, String reply) {
    CurrentUser admin = authService.requireUser(token);
    if (!"admin".equals(admin.getRoleType())) throw new BizException("无权处理反馈");

    Feedback f = feedbackMapper.selectById(id);
    if (f == null) throw new BizException("反馈记录不存在");

    f.setStatus(status);
    f.setAdminReply(reply);
    f.setHandledBy(admin.getUserId());
    f.setUpdatedAt(LocalDateTime.now());
    
    feedbackMapper.updateById(f);
}
```

### 5. FeedbackController.java - 添加接口
路径：`src/main/java/com/oldboss/silverjob/controller/FeedbackController.java`

新增方法：
```java
@PutMapping("/{id}")
public Result<Void> handle(@RequestHeader("Authorization") String token,
                           @PathVariable Long id,
                           @RequestBody Map<String, String> body) {
    feedbackService.handleFeedback(token, id, body.get("status"), body.get("reply"));
    return Result.success(null, "已成功回复反馈");
}
```

---

## 三、需确认事项

1. CurrentUser 类包路径是 `com.oldboss.silverjob.model.CurrentUser` 还是 `com.oldboss.silverjob.dto.CurrentUser`？
2. 是否删除 DatabaseInitializer.java？（该文件不存在，可能已被清理）
3. 数据库需要重建才能应用新表：执行 `docker-compose down -v` 然后重新启动

---

## 四、执行顺序

1. 修改 SQL 文件（schema.sql, seed.sql）
2. 修改 Java 实体类（Feedback.java）
3. 修改 VO 类（FeedbackItemVO.java）
4. 修改 Mapper（FeedbackMapper.java）
5. 修改 Service（FeedbackService.java）
6. 修改 Controller（FeedbackController.java）
7. 重建数据库：`docker-compose down -v && docker-compose up -d`