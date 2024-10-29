DROP TABLE IF EXISTS `llm_user`;
CREATE TABLE `llm_user`
(
    `id`         VARCHAR(36)  NOT NULL COMMENT '使用者ID',
    `username`   VARCHAR(50)  NOT NULL COMMENT '使用者名稱',
    `password`   VARCHAR(128) NOT NULL COMMENT '密碼',
    `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '暱稱',
    `phone`      VARCHAR(20)  DEFAULT NULL COMMENT '手機',
    `email`      VARCHAR(50)  DEFAULT NULL COMMENT 'Email',
    `avatar`     VARCHAR(100) DEFAULT NULL COMMENT '頭像',
    `status`     TINYINT(1) DEFAULT '0' COMMENT '狀態 0 無效 1有效',
    `created_at` TIMESTAMP    NOT NULL COMMENT '創建時間',
    `updated_at` TIMESTAMP    DEFAULT NULL COMMENT '更新時間',
    `created_by` VARCHAR(50) COMMENT '創建者',
    `updated_by` VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`username`)
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
    `user_id`  VARCHAR(50) NOT NULL COMMENT '使用者ID',
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
