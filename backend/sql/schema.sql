CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role_type VARCHAR(32) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    real_name VARCHAR(64),
    mobile VARCHAR(20),
    gender VARCHAR(16),
    age SMALLINT,
    city VARCHAR(128),
    health_desc TEXT,
    skill_tags TEXT,
    emergency_contact VARCHAR(64),
    emergency_mobile VARCHAR(20),
    relation VARCHAR(32),
    remark TEXT,
    note TEXT,
    avatar_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_task_status ON task(status);
CREATE INDEX IF NOT EXISTS idx_task_publisher_role ON task(publisher_role);
CREATE INDEX IF NOT EXISTS idx_task_elderly_id ON task(elderly_id);

CREATE TABLE IF NOT EXISTS task_order (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE REFERENCES task(id),
    order_code VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(128) NOT NULL,
    address VARCHAR(255) NOT NULL,
    salary INTEGER NOT NULL,
    employer_id BIGINT REFERENCES app_user(id),
    employer_name VARCHAR(64) NOT NULL,
    elderly_id BIGINT REFERENCES app_user(id),
    elderly_name VARCHAR(64) NOT NULL,
    start_time TIMESTAMP,
    finish_time TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    settle_text VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_task_order_status ON task_order(status);
CREATE INDEX IF NOT EXISTS idx_task_order_employer_id ON task_order(employer_id);
CREATE INDEX IF NOT EXISTS idx_task_order_elderly_id ON task_order(elderly_id);

CREATE TABLE IF NOT EXISTS bind_relation (
    id BIGSERIAL PRIMARY KEY,
    elderly_user_id BIGINT NOT NULL REFERENCES app_user(id),
    child_user_id BIGINT NOT NULL REFERENCES app_user(id),
    bind_code VARCHAR(16) NOT NULL,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    UNIQUE (elderly_user_id, child_user_id)
);

CREATE INDEX IF NOT EXISTS idx_bind_relation_code ON bind_relation(bind_code);

CREATE TABLE IF NOT EXISTS message (
    id BIGSERIAL PRIMARY KEY,
    bind_relation_id BIGINT REFERENCES bind_relation(id),
    sender_user_id BIGINT REFERENCES app_user(id),
    sender_role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
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
