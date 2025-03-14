alter table llm_doc modify size bigint null comment '文件大小';

DROP TABLE IF EXISTS `llm_user`;
CREATE TABLE `llm_user`
(
    `id`         VARCHAR(36)  NOT NULL COMMENT '使用者ID',
    `username`   VARCHAR(50)  NOT NULL COMMENT '使用者名稱',
    `password`   VARCHAR(128) NOT NULL COMMENT '密碼',
    `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '暱稱',
    `phone`      VARCHAR(20)  DEFAULT NULL COMMENT '手機',
    `email`      VARCHAR(50)  NOT NULL COMMENT 'Email',
    `avatar`     VARCHAR(100) DEFAULT NULL COMMENT '頭像',
    `status`     TINYINT(1) DEFAULT '0' COMMENT '狀態 0 無效 1有效',
    `created_at` TIMESTAMP    NOT NULL COMMENT '創建時間',
    `updated_at` TIMESTAMP    DEFAULT NULL COMMENT '更新時間',
    `created_by` VARCHAR(50) COMMENT '創建者',
    `updated_by` VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用者列表';

DROP TABLE IF EXISTS `llm_role`;
CREATE TABLE `llm_role`
(
    `name`        VARCHAR(50)    NOT NULL COMMENT '角色名稱',
    `num`         INTEGER UNIQUE NOT NULL COMMENT '角色代號',
    `description` VARCHAR(100)   NOT NULL COMMENT '描述',
    `created_at`  TIMESTAMP      NOT NULL COMMENT '創建時間',
    `updated_at`  TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    `created_by`  VARCHAR(50) COMMENT '創建者',
    `updated_by`  VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色列表';

DROP TABLE IF EXISTS `llm_permission`;
CREATE TABLE `llm_permission`
(
    `name`        VARCHAR(50)    NOT NULL COMMENT '權限名稱',
    `num`         INTEGER UNIQUE NOT NULL COMMENT '權限代號',
    `description` VARCHAR(100) DEFAULT NULL COMMENT '描述',
    `created_at`  TIMESTAMP      NOT NULL COMMENT '創建時間',
    `updated_at`  TIMESTAMP    DEFAULT NULL COMMENT '更新時間',
    `created_by`  VARCHAR(50) COMMENT '創建者',
    `updated_by`  VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='權限列表';

DROP TABLE IF EXISTS `llm_user_role`;
CREATE TABLE `llm_user_role`
(
    `user_id`  VARCHAR(36) NOT NULL COMMENT '使用者ID',
    `role_num` INTEGER     NOT NULL COMMENT '角色代號',
    PRIMARY KEY (`user_id`, `role_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用者角色對應表';

DROP TABLE IF EXISTS `llm_role_permission`;
CREATE TABLE `llm_role_permission`
(
    `role_num`       INTEGER NOT NULL COMMENT '角色代號',
    `permission_num` INTEGER NOT NULL COMMENT '權限代號',
    PRIMARY KEY (`role_num`, `permission_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色權限對應表';

-- 新增使用者
INSERT INTO bestpartner.llm_user (id, username, password, nickname, phone, email, avatar, status, created_at, updated_at, created_by, updated_by)
VALUES ('c88f57c8-ad26-4ea0-9f71-a65995b49357', 'admin',
        'c7ad44cbad762a5da0a452f9e854fdc1e0e7a52a38015f23f3eab1d80b931dd472634dfac71cd34ebc35d16ab7fb8a90c81f975113d6c7538dc69dd8de9077ec',
        '', '1234567890', 'admin@bestpartner.com.tw', '', 1, '2024-10-29 22:27:47.527988', '2024-10-29 22:27:47.527988',
        '', '');
INSERT INTO bestpartner.llm_user (id, username, password, nickname, phone, email, avatar, status, created_at, updated_at,
                             created_by, updated_by)
VALUES ('670017b4-23d0-4339-a9c0-22b6d9446461', 'test_user',
        'b14361404c078ffd549c03db443c3fede2f3e534d73f78f77301ed97d4a436a9fd9db05ee8b325c0ad36438b43fec8510c204fc1c1edb21d0941c00e9e2c1ce2',
        'test_user', '0987654321', 'test@partmer.com.tw', 'test.jpg', 1, '2024-10-30 22:50:07.325141',
        '2024-10-30 22:51:14.934868', '', '');

-- 新增角色
INSERT INTO bestpartner.llm_role (name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('ADMIN', 0, '管理員', '2024-10-30 09:58:26.000000', '2024-10-30 09:58:28.000000', 'SYSTEM', 'SYSTEM');
INSERT INTO bestpartner.llm_role (name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('USER', 1, '一般使用者', '2024-10-30 09:58:26.000000', '2024-10-30 09:58:28.000000', 'SYSTEM', 'SYSTEM');
INSERT INTO bestpartner.llm_role (name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('PRO_USER', 2, '進階使用者', '2024-10-30 17:52:59.000000', '2024-10-30 17:53:02.000000', 'SYSTEM', 'SYSTEM');

-- 角色對應權限
INSERT INTO bestpartner.llm_role_permission (role_num, permission_num) VALUES (0, 0);
INSERT INTO bestpartner.llm_role_permission (role_num, permission_num) VALUES (0, 1);
INSERT INTO bestpartner.llm_role_permission (role_num, permission_num) VALUES (1, 1);
