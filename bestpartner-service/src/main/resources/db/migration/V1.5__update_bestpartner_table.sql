-- 使用者自訂工具表
DROP TABLE IF EXISTS llm_tool_user_setting;
CREATE TABLE llm_tool_user_setting
(
    id              VARCHAR(36) NOT NULL COMMENT '主鍵',
    alias           VARCHAR(50) NOT NULL COMMENT '別名',
    user_id         VARCHAR(36) NOT NULL COMMENT '使用者ID',
    tool_id         VARCHAR(36) NOT NULL COMMENT '工具ID',
    setting_content JSON        NULL COMMENT '設定內容',
    created_at      TIMESTAMP   NOT NULL COMMENT '創建時間',
    updated_at      TIMESTAMP   NULL COMMENT '更新時間',
    created_by      VARCHAR(50) NULL COMMENT '創建者',
    updated_by      VARCHAR(50) NULL COMMENT '最後更新者',
    PRIMARY KEY (alias, user_id, tool_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用者自訂工具表';

DROP TABLE IF EXISTS llm_tool;
CREATE TABLE llm_tool
(
    id                   VARCHAR(36)  NOT NULL COMMENT '主鍵'
        PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL COMMENT '工具名稱',
    class_path           VARCHAR(100) NOT NULL COMMENT 'Class path',
    category_id          VARCHAR(36)  NOT NULL COMMENT '工具群組表 ID',
    type                 VARCHAR(10)  NOT NULL COMMENT '工具類型',
    config_object_path   VARCHAR(500) NULL COMMENT 'Tool設定物件Path',
    function_name        VARCHAR(100) NULL COMMENT '功能名稱',
    function_description TEXT NULL COMMENT '功能描述',
    function_params      JSON NULL COMMENT '功能參數',
    description          TEXT NULL COMMENT '工具描述',
    created_at           TIMESTAMP    NOT NULL COMMENT '創建時間',
    updated_at           TIMESTAMP NULL COMMENT '更新時間',
    created_by           VARCHAR(50) NULL COMMENT '創建者',
    updated_by           VARCHAR(50) NULL COMMENT '最後更新者',
    CONSTRAINT NAME
        UNIQUE (name)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具表';

DROP TABLE IF EXISTS `llm_doc_slice`;
CREATE TABLE `llm_doc_slice`
(
    `id`              VARCHAR(36) NOT NULL COMMENT '主鍵',
    `knowledge_id`    VARCHAR(36) NOT NULL COMMENT '知識庫ID',
    `doc_id`            VARCHAR(36) DEFAULT NULL COMMENT '文件ID',
    `content`            VARCHAR(36) DEFAULT NULL COMMENT '切片内容',
    `created_at`      TIMESTAMP   NOT NULL COMMENT '創建時間',
    `updated_at`      TIMESTAMP    DEFAULT NULL COMMENT '更新時間',
    `created_by`      VARCHAR(50) COMMENT '創建者',
    `updated_by`      VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件切片表';

alter table vector_store_setting
    change account user_id varchar(36) not null comment '使用者';

-- 使用者自訂工具表
INSERT INTO bestpartner.llm_tool_user_setting (id, alias, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('2e8d3421-9ebb-44fa-a468-4fc2d8196d5a', 'test', '670017b4-23d0-4339-a9c0-22b6d9446461', '0c743a37-1f98-444f-bba8-b063604e5bfb', '{"csi": "xxxxxx", "apiKey": "AIxxxxx", "timeout": 100000, "maxRetries": 5, "logRequests": true, "logResponses": true, "includeImages": false}', '2025-01-13 10:45:56', '2025-01-14 11:44:45', '670017b4-23d0-4339-a9c0-22b6d9446461', '670017b4-23d0-4339-a9c0-22b6d9446461');
INSERT INTO bestpartner.llm_tool_user_setting (id, alias, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('5eac36a5-e780-4992-a1b6-494829f782a3', 'test', '670017b4-23d0-4339-a9c0-22b6d9446461', '528e6798-8e4e-4233-97c0-3c6fce76ba0d', '{"apiKey": "tvly-uxxxxxx", "timeout": 100000, "includeAnswer": true, "excludeDomains": [], "includeDomains": [], "includeRawContent": false}', '2025-01-13 10:45:19', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_tool_user_setting (id, alias, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('922940ba-4d4c-4755-a271-6a08635f7773', 'test', '670017b4-23d0-4339-a9c0-22b6d9446461', 'f95fda5f-4632-4a1a-9a21-2d4facbd4279', '{"apiKey": "tvly-dev-xxxxxxxxxxxx", "timeout": 100000, "includeAnswer": true, "excludeDomains": [], "includeDomains": [], "includeRawContent": false}', '2025-02-21 22:33:25', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');

-- 工具分類表
INSERT INTO bestpartner.llm_tool_category (id, name, description, created_at, updated_at, created_by, updated_by) VALUES ('d09e84b9-17f2-4c07-bfa6-4dcf9201bd17', 'TEXT2SQL', 'Text2SQL群組', '2025-02-18 17:41:00', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');

-- 內建工具表
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, function_name, function_description, function_params, description, created_at, updated_at, created_by, updated_by) VALUES ('3737ec4a-2c88-490e-8301-ebc611f2433f', 'DateTool', 'tw.zipe.bastpartner.tool.DateTool', 'ea8a08e2-342d-4ade-9579-127d2d1443c5', 'CUSTOMIZE', null, 'getCurrentTime', '以台灣時間為基準，會根據不同時區取得當地日期時間', '{"zoneId": ["輸入格式為國家/城市，如:Australia/Darwin, Asia/Taipei, Africa/Harare", "String"]}', '日期工具', '2025-02-20 16:07:44', null, '', '');
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, function_name, function_description, function_params, description, created_at, updated_at, created_by, updated_by) VALUES ('c14e82ca-511f-424c-b20c-a96d93cae920', 'GoogleSearch', 'dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine', '90caee3f-2c87-48b9-8912-3dd810f62377', 'BUILT_IN', 'tw.zipe.bastpartner.tool.config.Google', '', '', null, '內建 Google 搜尋工具', '2025-02-20 16:08:47', null, '', '');
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, function_name, function_description, function_params, description, created_at, updated_at, created_by, updated_by) VALUES ('f95fda5f-4632-4a1a-9a21-2d4facbd4279', 'TavilySearch', 'dev.langchain4j.web.search.tavily.TavilyWebSearchEngine', '90caee3f-2c87-48b9-8912-3dd810f62377', 'BUILT_IN', 'tw.zipe.bastpartner.tool.config.Tavily', '', '', null, '內建 Tavily 搜尋工具', '2025-02-21 22:32:01', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');

-- 新增使用者 LLM 模型資料
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('91a777cf-9b1f-47d5-8690-73542c74d45e', '670017b4-23d0-4339-a9c0-22b6d9446461', '340580a6-74cf-4126-9c95-55741ec3996c', 'STREAMING_CHAT', 'openai_local_chat_test', '{"url": null, "topK": null, "topP": 0.5, "apiKey": "sk-proj-xxxxx", "timeout": 6000, "platform": "OPENAI", "maxTokens": 4096, "modelName": "gpt-4o-mini", "dimensions": null, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-02-22 22:43:37', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');

-- 新增向量資料庫設定
INSERT INTO bestpartner.vector_store_setting (id, user_id, type, alias, vector_setting, created_at, updated_at, created_by, updated_by) VALUES ('b6776d0e-406b-454f-85b0-08e18aaf9248', '670017b4-23d0-4339-a9c0-22b6d9446461', 'MILVUS', 'local-test', '{"url": "http://localhost:19530", "password": null, "username": null, "dimension": 1024, "requestLog": false, "responseLog": false, "collectionName": "ollama_local_collection"}', '2025-03-08 14:21:47', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.vector_store_setting (id, user_id, type, alias, vector_setting, created_at, updated_at, created_by, updated_by) VALUES ('d3c6465c-c14e-4968-9a2a-34a05e90189b', '670017b4-23d0-4339-a9c0-22b6d9446461', 'CHROMA', 'chroma-local-test', '{"url": "http://localhost:8000", "password": null, "username": null, "dimension": 1024, "requestLog": false, "responseLog": false, "collectionName": "chroma_local_collection"}', '2025-03-08 14:23:00', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
