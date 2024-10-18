DROP TABLE IF EXISTS `llm_model`;
CREATE TABLE `llm_setting`
(
    `id`            VARCHAR(36) NOT NULL COMMENT '主鍵',
    `account`       VARCHAR(36) NOT NULL COMMENT '使用者',
    `platform`      VARCHAR(50) NOT NULL COMMENT 'LLM 平台',
    `type`          VARCHAR(15) NOT NULL COMMENT '模型類型: CHAT、EMBEDDING',
    `alias`         VARCHAR(100) DEFAULT NULL COMMENT '自定義別名',
    `model_setting` JSON         DEFAULT NULL COMMENT '模型設定',
    `created_at`    TIMESTAMP   NOT NULL COMMENT '創建時間',
    `updated_at`    TIMESTAMP    DEFAULT NULL COMMENT '更新時間',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型配置表';

INSERT INTO PUBLIC.LLM_SETTING (ID, ACCOUNT, PLATFORM, TYPE, ALIAS, MODEL_SETTING, CREATED_AT, UPDATED_AT)
VALUES ('45d5c19f-819a-4a3a-83ad-d76812f97db0', 'SYSTEM', 'OLLAMA', 'EMBEDDING', 'example',
        JSON '{"id":null,"platform":null,"apiKey":null,"url":"http://localhost:11434","modelName":"llama3.1:latest","temperature":0.7,"topP":0.5,"topK":40,"dimensions":null,"maxTokens":4096,"timeout":60000,"logRequests":true,"logResponses":true}',
        '2024-10-18 10:09:02.365262', '2024-10-18 10:09:02.741789');


DROP TABLE IF EXISTS `llm_tool`;
CREATE TABLE `llm_tool`
(
    `id`         VARCHAR(36)  NOT NULL COMMENT '主鍵',
    `name`       VARCHAR(100) NOT NULL COMMENT '工具名稱',
    `class_path` VARCHAR(100) NOT NULL COMMENT 'Class path',
    `created_at` TIMESTAMP    NOT NULL COMMENT '創建時間',
    `updated_at` TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具表';
