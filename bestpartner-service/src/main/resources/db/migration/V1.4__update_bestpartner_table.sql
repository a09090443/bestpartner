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

-- LLM 模型工具表
DROP TABLE IF EXISTS `llm_tool`;
create table llm_tool
(
    id                 varchar(36)  not null comment '主鍵'
        primary key,
    name               varchar(100) not null comment '工具名稱',
    class_path         varchar(100) not null comment 'Class path',
    category_id        varchar(36)  not null comment '工具群組表 ID',
    type               varchar(10)  not null comment '工具類型',
    config_object_path varchar(500) null comment 'Tool設定物件Path',
    description        text         null comment '工具描述',
    created_at         timestamp    not null comment '創建時間',
    updated_at         timestamp    null comment '更新時間',
    created_by         varchar(50)  null comment '創建者',
    updated_by         varchar(50)  null comment '最後更新者',
    constraint name
        unique (name)
)
    comment '工具表' collate = utf8mb4_unicode_ci;

alter table llm_user
    modify status CHAR default '0' null comment '狀態 0 無效 1有效';

-- 刪除工具
DELETE FROM bestpartner.llm_tool WHERE name = 'GoogleSearch';
DELETE FROM bestpartner.llm_tool WHERE name = 'DateTool';
DELETE FROM bestpartner.llm_tool WHERE name = 'TavilySearch';
-- 新增工具
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, description, created_at, updated_at, updated_by, created_by) VALUES ('0c743a37-1f98-444f-bba8-b063604e5bfb', 'GoogleSearch', 'dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine', '90caee3f-2c87-48b9-8912-3dd810f62377', 'BUILT_IN', 'tw.zipe.bastpartner.tool.config.Google', '內建 Google 搜尋工具', '2025-01-12 21:26:15', null, '', 'c88f57c8-ad26-4ea0-9f71-a65995b49357');
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, description, created_at, updated_at, updated_by, created_by) VALUES ('3e836297-a91b-4edd-afbb-84954794da44', 'DateUtil', 'tw.zipe.bastpartner.tool.DateTool', 'ea8a08e2-342d-4ade-9579-127d2d1443c5', 'BUILT_IN', null, '內建日期工具', '2025-01-13 10:43:51', null, '', 'c88f57c8-ad26-4ea0-9f71-a65995b49357');
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, description, created_at, updated_at, updated_by, created_by) VALUES ('528e6798-8e4e-4233-97c0-3c6fce76ba0d', 'TavilySearch', 'dev.langchain4j.web.search.tavily.TavilyWebSearchEngine', '90caee3f-2c87-48b9-8912-3dd810f62377', 'BUILT_IN', 'tw.zipe.bastpartner.tool.config.Tavily', '內建 Tavily 搜尋工具', '2025-01-12 21:26:00', null, '', 'c88f57c8-ad26-4ea0-9f71-a65995b49357');

-- 新增使用者
INSERT INTO bestpartner.llm_user (id, username, password, nickname, phone, email, avatar, status, created_at,
                                  updated_at, created_by, updated_by)
VALUES ('5554f255-08d5-4069-99d2-928372d4a87b', 'user',
        'b14361404c078ffd549c03db443c3fede2f3e534d73f78f77301ed97d4a436a9fd9db05ee8b325c0ad36438b43fec8510c204fc1c1edb21d0941c00e9e2c1ce2',
        '', '1234567890', 'user@bestpartner.com.tw', '', '1', '2024-12-23 14:37:11', null, '', '');
INSERT INTO bestpartner.llm_user_role (user_id, role_num) VALUES ('c88f57c8-ad26-4ea0-9f71-a65995b49357', 0);
INSERT INTO bestpartner.llm_user_role (user_id, role_num) VALUES ('5554f255-08d5-4069-99d2-928372d4a87b', 1);

-- 新增權限
INSERT INTO bestpartner.llm_permission (id, name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('720ad595-712e-43b0-a4f5-8d35794ddbf6', 'admin', 0, '最高權限', '2024-12-31 20:54:10', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_permission (id, name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('f8508ea8-3c0d-479c-a56d-1c604d99b4f6', 'all', 2, '一般權限', '2024-12-31 20:54:44', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_permission (id, name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('93ded4b1-c93d-424c-9aa4-330de9d8bbf7', 'user-read', 1, '一般使用者讀取', '2024-12-31 20:52:51', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');

-- 新增 LLM 模型平台
INSERT INTO bestpartner.llm_platform (id, name, created_at, updated_at, created_by, updated_by) VALUES ('166a5745-b89c-410e-ada4-7ae3da426af5', 'OLLAMA', '2024-12-31 21:42:28', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');
INSERT INTO bestpartner.llm_platform (id, name, created_at, updated_at, created_by, updated_by) VALUES ('340580a6-74cf-4126-9c95-55741ec3996c', 'OPENAI', '2024-12-31 21:42:38', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');

-- 新增 LLM 模型設定資訊
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('0b471493-e45a-47da-b73b-58d88eba6194', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '166a5745-b89c-410e-ada4-7ae3da426af5', 'EMBEDDING', 'ollama_local_embedding_test', '{"url": "http://localhost:11434", "topK": 40, "topP": 0.5, "apiKey": null, "timeout": 60000, "platform": "OLLAMA", "maxTokens": 4096, "modelName": "bge-m3:latest", "dimensions": 1024, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-01 20:41:16', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('4069f3c2-ba50-4eed-b3d3-0cc53478dd9a', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '166a5745-b89c-410e-ada4-7ae3da426af5', 'CHAT', 'ollama_local_chat_test', '{"url": "http://localhost:11434", "topK": 40, "topP": 0.5, "apiKey": null, "timeout": 60000, "platform": "OLLAMA", "maxTokens": 4096, "modelName": "llama3.1:latest", "dimensions": null, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-01 20:41:05', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('42758e05-0c8f-4acd-9f72-3b9f6f38c5df', '670017b4-23d0-4339-a9c0-22b6d9446461', '166a5745-b89c-410e-ada4-7ae3da426af5', 'CHAT', 'ollama_user_chat_test', '{"url": "http://localhost:11434", "topK": 40, "topP": 0.5, "apiKey": null, "timeout": 60000, "platform": "OLLAMA", "maxTokens": 4096, "modelName": "llama3.1:latest", "dimensions": null, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-03 10:50:04', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('9c6838a3-7d20-4aca-9730-f198a8eb192c', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '340580a6-74cf-4126-9c95-55741ec3996c', 'CHAT', 'openai_local_chat_test', '{"url": null, "topK": null, "topP": 0.5, "apiKey": "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", "timeout": 6000, "platform": null, "maxTokens": 4096, "modelName": "gpt-4o-mini", "dimensions": null, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-01 20:39:24', '2025-01-01 21:35:24', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', 'c88f57c8-ad26-4ea0-9f71-a65995b49357');
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('e9b7f145-23a2-43cc-8bc0-3866ea265c50', '670017b4-23d0-4339-a9c0-22b6d9446461', '166a5745-b89c-410e-ada4-7ae3da426af5', 'EMBEDDING', 'ollama_user_embedding_test', '{"url": "http://localhost:11434", "topK": 40, "topP": 0.5, "apiKey": null, "timeout": 60000, "platform": "OLLAMA", "maxTokens": 4096, "modelName": "bge-m3:latest", "dimensions": 1024, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-03 10:50:30', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('fbeafc72-eda0-4a2b-b79e-ed88f53db29f', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '340580a6-74cf-4126-9c95-55741ec3996c', 'EMBEDDING', 'local_embedding_test', '{"url": null, "topK": null, "topP": 0.5, "apiKey": "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", "timeout": 6000, "platform": null, "maxTokens": 4096, "modelName": "text-embedding-3-small", "dimensions": 1536, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-01 20:40:44', '2025-01-01 21:39:03', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', 'c88f57c8-ad26-4ea0-9f71-a65995b49357');

-- 新增使用者工具
INSERT INTO bestpartner.llm_tool_user_setting (id, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('2e8d3421-9ebb-44fa-a468-4fc2d8196d5a', '670017b4-23d0-4339-a9c0-22b6d9446461', '0c743a37-1f98-444f-bba8-b063604e5bfb', '{"csi": "CSI", "apiKey": "API_KEY", "timeout": 100000, "maxRetries": 5, "logRequests": true, "logResponses": true, "includeImages": false}', '2025-01-13 10:45:56', '2025-01-14 11:44:45', '670017b4-23d0-4339-a9c0-22b6d9446461', '670017b4-23d0-4339-a9c0-22b6d9446461');
INSERT INTO bestpartner.llm_tool_user_setting (id, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('5eac36a5-e780-4992-a1b6-494829f782a3', '670017b4-23d0-4339-a9c0-22b6d9446461', '528e6798-8e4e-4233-97c0-3c6fce76ba0d', '{"apiKey": "API_KEY", "timeout": 100000, "includeAnswer": true, "excludeDomains": [], "includeDomains": [], "includeRawContent": false}', '2025-01-13 10:45:19', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');

-- 更新系統設定
UPDATE bestpartner.system_setting t
SET t.setting_key = 'default_llm_platform',
    t.description = 'system default llm_platform'
WHERE t.id = 1;
