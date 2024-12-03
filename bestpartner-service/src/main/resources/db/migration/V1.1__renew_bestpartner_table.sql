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

DROP TABLE IF EXISTS `llm_doc`;
CREATE TABLE `llm_doc`
(
    `id`              VARCHAR(36) NOT NULL COMMENT '主鍵',
    `knowledge_id`    VARCHAR(36) NOT NULL COMMENT '知識庫ID',
    `vector_store_id` VARCHAR(36)  DEFAULT NULL COMMENT '向量資料庫ID',
    `name`            VARCHAR(255) DEFAULT NULL COMMENT '名稱',
    `type`            VARCHAR(50)  DEFAULT NULL COMMENT '類型',
    `url`             VARCHAR(255) DEFAULT NULL COMMENT '網址',
    `description`     VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `size`            INT          DEFAULT NULL COMMENT '文件大小',
    `created_at`      TIMESTAMP   NOT NULL COMMENT '創建時間',
    `updated_at`      TIMESTAMP    DEFAULT NULL COMMENT '更新時間',
    `created_by`      VARCHAR(50) COMMENT '創建者',
    `updated_by`      VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件表';

DROP TABLE IF EXISTS `llm_tool`;
CREATE TABLE `llm_tool`
(
    `id`         VARCHAR(36)  NOT NULL COMMENT '主鍵',
    `name`       VARCHAR(100) NOT NULL COMMENT '工具名稱',
    `class_path` VARCHAR(100) NOT NULL COMMENT 'Class path',
    `created_at` TIMESTAMP    NOT NULL COMMENT '創建時間',
    `updated_at` TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    `created_by` VARCHAR(50) COMMENT '創建者',
    `updated_by` VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具表';

DROP TABLE IF EXISTS `llm_user`;
CREATE TABLE `llm_user`
(
    `id`         VARCHAR(36)  NOT NULL COMMENT '使用者ID',
    `username`   VARCHAR(50)  NOT NULL COMMENT '使用者名稱',
    `password`   VARCHAR(100) NOT NULL COMMENT '密碼',
    `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '暱稱',
    `phone`      VARCHAR(20)  DEFAULT NULL COMMENT '手機',
    `email`      VARCHAR(50)  DEFAULT NULL COMMENT 'Email',
    `avatar`     VARCHAR(100) DEFAULT NULL COMMENT '頭像',
    `status`     TINYINT(1) DEFAULT '0' COMMENT '狀態 0 鎖定 1有效',
    `created_at` TIMESTAMP    NOT NULL COMMENT '創建時間',
    `updated_at` TIMESTAMP    DEFAULT NULL COMMENT '更新時間',
    `created_by` VARCHAR(50) COMMENT '創建者',
    `updated_by` VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用者列表';

-- 新增 LLM 模型資料
-- OPENAI
INSERT INTO bestpartner.llm_setting (id, account, platform, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('40605e2f-be98-47e1-9181-da9ca0d1df8b', 'SYSTEM', 'OPENAI', 'CHAT', 'openai_local_chat_test', '{"id": null, "url": null, "topK": null, "topP": 0.5, "apiKey": "sk-xxx", "timeout": 6000, "platform": "OPENAI", "maxTokens": 4096, "modelName": "gpt-4o-mini", "dimensions": null, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2024-10-22 15:07:10', '2024-10-22 15:07:10', 'admin', 'admin');
INSERT INTO bestpartner.llm_setting (id, account, platform, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('488e9ad6-b3c1-420e-92a4-c3ff116ddae1', 'SYSTEM', 'OPENAI', 'EMBEDDING', 'local_embedding_test', '{"id": null, "url": null, "topK": 40, "topP": 0.5, "apiKey": "sk-xxx", "timeout": 6000, "platform": "OPENAI", "maxTokens": 4096, "modelName": "text-embedding-3-small", "dimensions": 1536, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2024-10-21 09:51:52', '2024-10-21 09:51:52', 'admin', 'admin');
-- OLLAMA
INSERT INTO bestpartner.llm_setting (id, account, platform, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('3834bd26-14a5-40a5-81ee-ab2a2c91557a', 'USER', 'OLLAMA', 'CHAT', 'ollama_local_chat_test', '{"id": null, "url": "http://localhost:11434", "topK": 40, "topP": 0.5, "apiKey": null, "timeout": 60000, "platform": "OLLAMA", "maxTokens": 4096, "modelName": "llama3.1:latest", "dimensions": null, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2024-10-22 11:50:43', '2024-12-03 10:13:03', 'user', 'user');
INSERT INTO bestpartner.llm_setting (id, account, platform, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('b613df34-1c03-4657-8d08-2372d5f291ef', 'USER', 'OLLAMA', 'EMBEDDING', 'ollama_local_embedding_test', '{"id": null, "url": "http://localhost:11434", "topK": 40, "topP": 0.5, "apiKey": null, "timeout": 60000, "platform": "OLLAMA", "maxTokens": 4096, "modelName": "bge-m3:latest", "dimensions": 1024, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2024-10-22 15:06:40', '2024-10-22 15:06:40', 'user', 'user');

-- 新增系統設定
INSERT INTO bestpartner.system_setting (setting_key, setting_value, description, created_at, updated_at, created_by,
                                   updated_by)
VALUES ('default_llm', 'OPENAI', 'system default llm', '2024-10-19 20:08:21.833397', '2024-10-19 12:10:04.079429',
        'system', 'system');

-- 新增向量資料庫設定
INSERT INTO bestpartner.vector_store_setting (id, account, type, alias, vector_setting, created_at, updated_at, created_by, updated_by) VALUES ('8ef34f2f-5820-47ee-8743-894cacd0b711', 'SYSTEM', 'MILVUS', 'local-test', '{"url": "http://localhost:19530", "password": null, "username": null, "dimension": 1536, "collectionName": "milvus_local_collection"}', '2024-10-21 10:51:34', '2024-10-21 10:51:34', 'admin', 'admin');
INSERT INTO bestpartner.vector_store_setting (id, account, type, alias, vector_setting, created_at, updated_at, created_by, updated_by) VALUES ('d351ae41-b143-4a8b-8443-64f806a2602a', 'USER', 'MILVUS', 'local-test', '{"url": "http://localhost:19530", "password": null, "username": null, "dimension": 1024, "collectionName": "ollama_local_collection"}', '2024-10-22 16:00:41', '2024-12-03 10:15:53', 'user', 'user');
