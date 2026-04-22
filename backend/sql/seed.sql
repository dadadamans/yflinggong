INSERT INTO app_user (
    id, username, password_hash, role_type, nickname, real_name, mobile, gender, age, city,
    health_desc, skill_tags, emergency_contact, emergency_mobile, relation, remark, note
) VALUES
    (1, 'elderly', NULL, 'elderly', '王阿姨', '王秀兰', '13800001111', '女', 63, '上海静安',
     '身体稳定，可完成半天以内的轻体力任务', '陪诊、做饭、买菜', '王女士', '13800002231', NULL, NULL, NULL),
    (2, 'employer', NULL, 'employer', '李先生', '李先生', '13900006618', NULL, NULL, NULL,
     NULL, NULL, NULL, NULL, NULL, '偏向发布社区附近的短时照护和陪诊任务', NULL),
    (3, 'child', NULL, 'child', '王女士', '王女士', '13700002231', NULL, NULL, NULL,
     NULL, NULL, NULL, NULL, '女儿', NULL, '希望随时看到老人接单状态和留言'),
    (4, 'admin', '123456', 'admin', '管理员', '管理员', '13600000000', NULL, NULL, NULL,
     NULL, NULL, NULL, NULL, NULL, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

SELECT setval('app_user_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM app_user), 1), true);

INSERT INTO task (
    id, task_type, title, address, time_text, salary, content,
    publisher_id, publisher_name, publisher_role, status, elderly_id, elderly_name,
    employer_id, employer_name, order_code, start_time, finish_time, settle_text
) VALUES
    (1, '陪诊', '社区医院陪同复查', '海棠社区卫生服务中心', '今天 14:00 - 17:00', 120,
     '帮助挂号、取药和陪同问诊，路程近，流程简单。', 2, '李先生', 'employer', 'working', 1, '王秀兰',
     2, '李先生', '#2026040301', '2026-04-03 14:00:00', NULL, '待结算'),
    (2, '家政', '午间做饭与厨房整理', '静安寺街道安福小区 3 栋 202', '明天 10:30 - 12:30', 90,
     '准备两人午餐，饭后简单整理厨房。', 2, '李先生', 'employer', 'waiting', NULL, NULL,
     NULL, NULL, NULL, NULL, NULL, NULL),
    (3, '陪护', '上门陪伴老人聊天', '和睦家园 8 号楼', '后天 09:00 - 11:00', 80,
     '陪老人散步、聊天，节奏轻松。', NULL, '赵阿姨', 'other', 'done', 1, '王秀兰',
     NULL, '赵阿姨', '#2026040102', '2026-04-01 09:00:00', '2026-04-01 11:10:00', '模拟已结算')
ON CONFLICT (id) DO NOTHING;

SELECT setval('task_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM task), 1), true);

INSERT INTO bind_relation (
    id, elderly_user_id, child_user_id, bind_code, confirmed, created_at, confirmed_at
) VALUES
    (1, 1, 3, '618204', FALSE, '2026-04-03 10:00:00', NULL)
ON CONFLICT (elderly_user_id, child_user_id) DO NOTHING;

SELECT setval('bind_relation_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM bind_relation), 1), true);

INSERT INTO message (
    id, bind_relation_id, sender_user_id, sender_role, content, sent_at
) VALUES
    (1, 1, 3, 'child', '妈，结束后记得休息一下，我晚上给您打电话。', '2026-04-03 17:05:00'),
    (2, 1, 1, 'elderly', '好的，今天任务不重，忙完我就回家。', '2026-04-03 17:08:00'),
    (3, 1, 3, 'child', '如果需要我帮忙联系雇主，随时告诉我。', '2026-04-03 17:10:00')
ON CONFLICT (id) DO NOTHING;

SELECT setval('message_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM message), 1), true);

INSERT INTO user_session (token, user_id, role_type, expired_at)
VALUES ('demo-elderly-seed-token', 1, 'elderly', NULL)
ON CONFLICT (token) DO NOTHING;
