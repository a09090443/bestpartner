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

INSERT INTO PUBLIC.LLM_USER (ID, USERNAME, PASSWORD, NICKNAME, PHONE, EMAIL, AVATAR, STATUS, CREATED_AT, UPDATED_AT,
                             CREATED_BY, UPDATED_BY)
VALUES ('c88f57c8-ad26-4ea0-9f71-a65995b49357', 'admin',
        'c7ad44cbad762a5da0a452f9e854fdc1e0e7a52a38015f23f3eab1d80b931dd472634dfac71cd34ebc35d16ab7fb8a90c81f975113d6c7538dc69dd8de9077ec',
        '', '1234567890', 'admin@bestpartner.com.tw', '', 0, '2024-10-29 22:27:47.527988', '2024-10-29 22:27:47.527988',
        '', '');
