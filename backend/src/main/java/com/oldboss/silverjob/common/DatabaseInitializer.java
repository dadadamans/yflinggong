package com.oldboss.silverjob.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("ALTER TABLE app_user ADD COLUMN IF NOT EXISTS health_report_url VARCHAR(512)");
        } catch (Exception ignored) {}
        
        try {
            jdbcTemplate.execute("ALTER TABLE app_user ADD COLUMN IF NOT EXISTS health_report_status VARCHAR(32)");
        } catch (Exception ignored) {}
        
        try {
            jdbcTemplate.execute("ALTER TABLE app_user ADD COLUMN IF NOT EXISTS health_report_time TIMESTAMP");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE app_user ADD COLUMN IF NOT EXISTS total_score INTEGER DEFAULT 0");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("ALTER TABLE app_user ADD COLUMN IF NOT EXISTS comment_count INTEGER DEFAULT 0");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS comment (" +
                    "id BIGSERIAL PRIMARY KEY," +
                    "task_id BIGINT NOT NULL," +
                    "order_id BIGINT NOT NULL," +
                    "reviewer_id BIGINT NOT NULL," +
                    "reviewer_name VARCHAR(64)," +
                    "reviewer_role VARCHAR(32)," +
                    "reviewee_id BIGINT NOT NULL," +
                    "reviewee_name VARCHAR(64)," +
                    "rating INTEGER NOT NULL," +
                    "content TEXT," +
                    "comment_type VARCHAR(32)," +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_comment_reviewee_id ON comment(reviewee_id)");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_comment_task_reviewer ON comment(task_id, reviewer_id)");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_bind_relation_child_confirmed ON bind_relation(child_user_id) WHERE confirmed = true AND child_user_id IS NOT NULL");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("UPDATE task SET settle_text = '已结算' WHERE settle_text = '模拟已支付'");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS feedback (" +
                    "id BIGSERIAL PRIMARY KEY," +
                    "user_id BIGINT NOT NULL," +
                    "role VARCHAR(20) NOT NULL," +
                    "content TEXT NOT NULL," +
                    "related_order_id BIGINT," +
                    "status VARCHAR(20) DEFAULT 'pending'," +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_feedback_user_id ON feedback(user_id)");
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_feedback_status ON feedback(status)");
        } catch (Exception ignored) {}
    }
}
