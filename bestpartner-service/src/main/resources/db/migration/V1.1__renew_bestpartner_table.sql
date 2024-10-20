DROP TABLE IF EXISTS `llm_model`;
DROP TABLE IF EXISTS `llm_setting`;
-- LLM 模型
CREATE TABLE `llm_setting`
(
    `id`            VARCHAR(36) NOT NULL COMMENT '主鍵',
    `account`       VARCHAR(36) NOT NULL COMMENT '使用者',
    `platform`      VARCHAR(50) NOT NULL COMMENT 'LLM 平台',
    `type`          VARCHAR(15) NOT NULL COMMENT '模型類型: CHAT、EMBEDDING',
    `alias`         VARCHAR(100) DEFAULT NULL COMMENT '自定義別名',
    `model_setting` JSON         DEFAULT NULL COMMENT '模型設定',
    `created_at`    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    `updated_at`    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    `created_by`    VARCHAR(50) COMMENT '創建者',
    `updated_by`    VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表';

DROP TABLE IF EXISTS `system_setting`;
-- 系統設定
CREATE TABLE `system_setting`
(
    `id`            BIGINT AUTO_INCREMENT COMMENT '主鍵',
    `setting_key`   VARCHAR(100) NOT NULL COMMENT '設定名稱',
    `setting_value` TEXT COMMENT '設定值',
    `description`   TEXT COMMENT '設定說明',
    `created_at`    TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    `updated_at`    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    `created_by`    VARCHAR(50) COMMENT '創建者',
    `updated_by`    VARCHAR(50) COMMENT '最後更新者',
    UNIQUE KEY idx_system_setting_key (`setting_key`),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系統設定表';

DROP TABLE IF EXISTS `vector_store_setting`;
-- 向量資料庫設定
CREATE TABLE `vector_store_setting`
(
    `id`             VARCHAR(36) NOT NULL COMMENT '主鍵',
    `account`        VARCHAR(36) NOT NULL COMMENT '使用者',
    `type`           VARCHAR(50) NOT NULL COMMENT '向量資料庫類型',
    `alias`          VARCHAR(100) DEFAULT NULL COMMENT '自定義別名',
    `vector_setting` JSON         DEFAULT NULL COMMENT '向量資料庫設定',
    `created_at`     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    `updated_at`     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    `created_by`     VARCHAR(50) COMMENT '創建者',
    `updated_by`     VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='向量資料庫設定表';

DROP TABLE IF EXISTS `llm_tool`;
CREATE TABLE `llm_tool`
(
    `id`         VARCHAR(36)  NOT NULL COMMENT '主鍵',
    `name`       VARCHAR(100) NOT NULL COMMENT '工具名稱',
    `class_path` VARCHAR(100) NOT NULL COMMENT 'Class path',
    `created_at` TIMESTAMP    NOT NULL COMMENT '創建時間',
    `updated_at` TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具表';

INSERT INTO PUBLIC.LLM_SETTING (ID, ACCOUNT, PLATFORM, TYPE, ALIAS, MODEL_SETTING, CREATED_AT, UPDATED_AT, CREATED_BY,
                                UPDATED_BY)
VALUES ('45d5c19f-819a-4a3a-83ad-d76812f97db0', 'SYSTEM', 'OLLAMA', 'EMBEDDING', 'example',
        JSON '{"id":null,"platform":null,"apiKey":null,"url":"http://localhost:11434","modelName":"llama3.1:latest","temperature":0.7,"topP":0.5,"topK":40,"dimensions":null,"maxTokens":4096,"timeout":60000,"logRequests":true,"logResponses":true}',
        '2024-10-18 10:09:02.365262', '2024-10-19 06:34:07.475463', 'admin', 'admin');

INSERT INTO PUBLIC.SYSTEM_SETTING (SETTING_KEY, SETTING_VALUE, DESCRIPTION, CREATED_AT, UPDATED_AT, CREATED_BY,
                                   UPDATED_BY)
VALUES ('default_llm', 'OPENAI', 'system default llm', '2024-10-19 20:08:21.833397', '2024-10-19 12:10:04.079429',
        'system', 'system');

INSERT INTO PUBLIC.VECTOR_STORE_SETTING (ID, ACCOUNT, TYPE, ALIAS, VECTOR_SETTING, CREATED_AT, UPDATED_AT, CREATED_BY,
                                         UPDATED_BY)
VALUES ('424c5fcf-a3c4-457c-96d4-6457bec1b432', 'SYSTEM', 'MILVUS', 'system',
        '''7B2275726C223A22687474703A2F2F6C6F63616C686F73743A3139353330222C22757365726E616D65223A6E756C6C2C2270617373776F7264223A6E756C6C2C22636F6C6C656374696F6E4E616D65223A2273797374656D2D636F6C6C656374696F6E222C2264696D656E73696F6E223A313032347D''',
        '2024-10-20 12:42:57.992171', '2024-10-20 04:46:25.389282', 'admin', 'admin');
