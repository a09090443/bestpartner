DROP TABLE IF EXISTS `llm_model`;
CREATE TABLE `llm_setting`
(
    `id` VARCHAR(36) NOT NULL COMMENT '主鍵',
    `platform` VARCHAR(50) DEFAULT NULL COMMENT 'LLM 平台',
    `type` VARCHAR(15) DEFAULT NULL COMMENT '模型類型: CHAT、EMBEDDING',
    `alias` VARCHAR(100) DEFAULT NULL COMMENT '自定義別名',
    `model_setting` JSON DEFAULT NULL COMMENT '模型設定',
    `created_at` TIMESTAMP NOT NULL COMMENT '創建時間',
    `updated_at` TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型配置表';

DROP TABLE IF EXISTS `llm_tool`;
CREATE TABLE `llm_tool`
(
    `id` VARCHAR(36) NOT NULL COMMENT '主鍵',
    `name` VARCHAR(100) NOT NULL COMMENT '工具名稱',
    `class_path` VARCHAR(100) NOT NULL COMMENT 'Class path',
    `created_at` TIMESTAMP NOT NULL COMMENT '創建時間',
    `updated_at` TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具表';
