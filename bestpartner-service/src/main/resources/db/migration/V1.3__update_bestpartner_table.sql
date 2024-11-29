DROP TABLE IF EXISTS `llm_tool`;
CREATE TABLE `llm_tool`
(
    `id`             VARCHAR(36)  NOT NULL COMMENT '主鍵',
    `name`           VARCHAR(100) NOT NULL COMMENT '工具名稱',
    `class_path`     VARCHAR(100) NOT NULL COMMENT 'Class path',
    `setting_fields` TEXT      DEFAULT NULL COMMENT '設定值欄位名稱, 以逗號分隔',
    `created_at`     TIMESTAMP    NOT NULL COMMENT '創建時間',
    `updated_at`     TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    `created_by`     VARCHAR(50) COMMENT '創建者',
    `updated_by`     VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具表';

DROP TABLE IF EXISTS `llm_tool_user_setting`;
CREATE TABLE `llm_tool_user_setting`
(
    `id`              VARCHAR(36) NOT NULL COMMENT '主鍵',
    `user_id`         VARCHAR(36) NOT NULL COMMENT '使用者ID',
    `tool_id`         VARCHAR(36) NOT NULL COMMENT '工具ID',
    `setting_content` JSON      DEFAULT NULL COMMENT '設定內容',
    `created_at`      TIMESTAMP   NOT NULL COMMENT '創建時間',
    `updated_at`      TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    `created_by`      VARCHAR(50) COMMENT '創建者',
    `updated_by`      VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`user_id`, `tool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用者自訂工具表';
