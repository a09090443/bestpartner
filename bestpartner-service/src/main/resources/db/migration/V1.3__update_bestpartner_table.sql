DROP TABLE IF EXISTS `llm_tool`;
CREATE TABLE `llm_tool`
(
    `id`             VARCHAR(36)  NOT NULL COMMENT '主鍵',
    `name`           VARCHAR(100) NOT NULL COMMENT '工具名稱',
    `class_path`     VARCHAR(100) NOT NULL COMMENT 'Class path',
    `category`          VARCHAR(50)  NOT NULL COMMENT '工具分類',
    `type`           VARCHAR(10)  NOT NULL COMMENT '工具類型',
    `setting_fields` TEXT      DEFAULT NULL COMMENT '設定值欄位名稱, 以逗號分隔',
    `description`    TEXT      DEFAULT NULL COMMENT '工具描述',
    `created_at`     TIMESTAMP    NOT NULL COMMENT '創建時間',
    `updated_at`     TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    `created_by`     VARCHAR(50) COMMENT '創建者',
    `updated_by`     VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具表';

DROP TABLE IF EXISTS `llm_tool_category`;
CREATE TABLE `llm_tool_category`
(
    `id`          VARCHAR(36)         NOT NULL COMMENT '主鍵',
    `name`        VARCHAR(30) UNIQUE NOT NULL COMMENT '群組名稱',
    `description` TEXT      DEFAULT NULL COMMENT '群組描述',
    `created_at`  TIMESTAMP           NOT NULL COMMENT '創建時間',
    `updated_at`  TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    `created_by`  VARCHAR(50) COMMENT '創建者',
    `updated_by`  VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具群組表';

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

-- 註冊工具
INSERT INTO bestpartner.llm_tool (id, name, class_path, category, type, setting_fields, description, created_at, updated_at, created_by, updated_by) VALUES ('2dee0287-7606-4964-979f-381282873ed7', 'DateTool', 'tw.zipe.basepartner.tool.DateTool', 'DATE', 'BUILT_IN', null, null, '2024-12-03 17:44:43', '2024-12-03 17:44:43', 'admin', 'admin');
