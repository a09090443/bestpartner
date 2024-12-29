-- 系統權限表
DROP TABLE IF EXISTS `llm_permission`;
CREATE TABLE `llm_permission`
(
    `id`          VARCHAR(36) NOT NULL COMMENT '識別碼',
    `name`        VARCHAR(50) NOT NULL COMMENT '權限名稱',
    `num`         INTEGER     NOT NULL COMMENT '權限代號',
    `description` VARCHAR(100) DEFAULT NULL COMMENT '描述',
    `created_at`  TIMESTAMP   NOT NULL COMMENT '創建時間',
    `updated_at`  TIMESTAMP    DEFAULT NULL COMMENT '更新時間',
    `created_by`  VARCHAR(50) COMMENT '創建者',
    `updated_by`  VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`name`, `num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='權限列表';

-- LLM 模型平台
DROP TABLE IF EXISTS `llm_platform`;
CREATE TABLE `llm_platform`
(
    `id`         VARCHAR(36)  NOT NULL COMMENT '主鍵',
    `name`       VARCHAR(100) NOT NULL COMMENT 'LLM 平台名稱',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    `created_by` VARCHAR(50) COMMENT '創建者',
    `updated_by` VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型平台清單';

-- LLM 模型設定資訊表
DROP TABLE IF EXISTS `llm_setting`;
CREATE TABLE `llm_setting`
(
    `id`            VARCHAR(36) NOT NULL COMMENT '主鍵',
    `user_id`       VARCHAR(36) NOT NULL COMMENT '使用者ID',
    `platform_id`   VARCHAR(36) NOT NULL COMMENT 'LLM 平台ID',
    `type`          VARCHAR(15) NOT NULL COMMENT '模型類型: CHAT、EMBEDDING',
    `alias`         VARCHAR(100) DEFAULT NULL COMMENT '自定義別名',
    `model_setting` JSON         DEFAULT NULL COMMENT '模型設定',
    `created_at`    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    `updated_at`    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    `created_by`    VARCHAR(50) COMMENT '創建者',
    `updated_by`    VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表';

alter table llm_user
    modify status CHAR default '0' null comment '狀態 0 無效 1有效';

-- 新增使用者
INSERT INTO bestpartner.llm_user (id, username, password, nickname, phone, email, avatar, status, created_at,
                                  updated_at, created_by, updated_by)
VALUES ('5554f255-08d5-4069-99d2-928372d4a87b', 'user',
        'b14361404c078ffd549c03db443c3fede2f3e534d73f78f77301ed97d4a436a9fd9db05ee8b325c0ad36438b43fec8510c204fc1c1edb21d0941c00e9e2c1ce2',
        '', '1234567890', 'user@bestpartner.com.tw', '', '1', '2024-12-23 14:37:11', null, '', '');
INSERT INTO bestpartner.llm_user_role (user_id, role_num)
VALUES ('5554f255-08d5-4069-99d2-928372d4a87b', 1);
