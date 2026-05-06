CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    role_type VARCHAR(32) NOT NULL,
    nickname VARCHAR(64),
    mobile VARCHAR(32),
    real_name VARCHAR(64),
    relation VARCHAR(32),
    age VARCHAR(8),
    gender VARCHAR(8),
    city VARCHAR(64),
    health_desc VARCHAR(256),
    skill_tags VARCHAR(256),
    emergency_contact VARCHAR(64),
    emergency_mobile VARCHAR(32),
    remark VARCHAR(256),
    note VARCHAR(256),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE (username, role_type)
);

CREATE INDEX IF NOT EXISTS idx_app_user_role_type ON app_user(role_type);
CREATE INDEX IF NOT EXISTS idx_app_user_mobile ON app_user(mobile);

CREATE TABLE IF NOT EXISTS task (
    id BIGSERIAL PRIMARY KEY,
    task_type VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    address VARCHAR(255) NOT NULL,
    time_text VARCHAR(128) NOT NULL,
    salary INTEGER NOT NULL,
    content TEXT NOT NULL,
    publisher_id BIGINT REFERENCES app_user(id),
    publisher_name VARCHAR(64) NOT NULL,
    publisher_role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    elderly_id BIGINT REFERENCES app_user(id),
    elderly_name VARCHAR(64),
    employer_id BIGINT REFERENCES app_user(id),
    employer_name VARCHAR(64),
    order_code VARCHAR(32),
    start_time TIMESTAMP,
    finish_time TIMESTAMP,
    settle_text VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_task_status ON task(status);
CREATE INDEX IF NOT EXISTS idx_task_publisher_role ON task(publisher_role);
CREATE INDEX IF NOT EXISTS idx_task_elderly_id ON task(elderly_id);
CREATE INDEX IF NOT EXISTS idx_task_publisher_id ON task(publisher_id);

DROP TABLE IF EXISTS task_order;

CREATE TABLE IF NOT EXISTS bind_relation (
    id BIGSERIAL PRIMARY KEY,
    elderly_user_id BIGINT REFERENCES app_user(id),
    child_user_id BIGINT REFERENCES app_user(id),
    bind_code VARCHAR(16) NOT NULL,
    code_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    UNIQUE (bind_code)
);

CREATE INDEX IF NOT EXISTS idx_bind_relation_code ON bind_relation(bind_code);

ALTER TABLE bind_relation ADD COLUMN IF NOT EXISTS code_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS message (
    id BIGSERIAL PRIMARY KEY,
    bind_relation_id BIGINT REFERENCES bind_relation(id),
    sender_user_id BIGINT REFERENCES app_user(id),
    sender_role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_message_bind_relation_id ON message(bind_relation_id);
CREATE INDEX IF NOT EXISTS idx_message_sent_at ON message(sent_at);

CREATE TABLE IF NOT EXISTS user_session (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(128) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    role_type VARCHAR(32) NOT NULL,
    expired_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_session_user_id ON user_session(user_id);

-- 体检报告字段
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS health_report_url VARCHAR(512);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS health_report_status VARCHAR(32);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS health_report_time TIMESTAMP;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS health_condition VARCHAR(32);

-- 老人字体大小设置
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS font_size VARCHAR(16) DEFAULT 'medium';

-- 用户评价累计字段
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS total_score INTEGER DEFAULT 0;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS comment_count INTEGER DEFAULT 0;

-- 评价表
CREATE TABLE IF NOT EXISTS comment (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task(id),
    order_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL REFERENCES app_user(id),
    reviewer_name VARCHAR(64),
    reviewer_role VARCHAR(32),
    reviewee_id BIGINT NOT NULL REFERENCES app_user(id),
    reviewee_name VARCHAR(64),
    rating INTEGER NOT NULL,
    content TEXT,
    comment_type VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_comment_reviewee_id ON comment(reviewee_id);
CREATE INDEX IF NOT EXISTS idx_comment_task_id ON comment(task_id);
CREATE INDEX IF NOT EXISTS idx_comment_reviewer_id ON comment(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_comment_task_reviewer ON comment(task_id, reviewer_id);

-- 绑定关系1对1约束（同一个老人只能绑定一个子女）
CREATE UNIQUE INDEX IF NOT EXISTS idx_bind_relation_elderly_confirmed 
ON bind_relation(elderly_user_id) WHERE confirmed = true;

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

-- 子女侧也保持1对1约束（同一个子女只能绑定一个老人）
CREATE UNIQUE INDEX IF NOT EXISTS idx_bind_relation_child_confirmed
ON bind_relation(child_user_id) WHERE confirmed = true AND child_user_id IS NOT NULL;
