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
    `id`           VARCHAR(36) NOT NULL COMMENT '主鍵',
    `knowledge_id` VARCHAR(36) NOT NULL COMMENT '知識庫ID',
    `doc_id`       VARCHAR(36) DEFAULT NULL COMMENT '文件ID',
    `content`      TEXT        NULL COMMENT '切片内容',
    `created_at`   TIMESTAMP   NOT NULL COMMENT '創建時間',
    `updated_at`   TIMESTAMP   DEFAULT NULL COMMENT '更新時間',
    `created_by`   VARCHAR(50) COMMENT '創建者',
    `updated_by`   VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文件切片表';

DROP TABLE IF EXISTS `vector_store_setting`;
CREATE TABLE vector_store_setting
(
    id             varchar(36)                         not null comment '主鍵'
        primary key,
    user_id        varchar(36)                         not null comment '使用者',
    type           varchar(50)                         not null comment '向量資料庫類型',
    alias          varchar(100)                        null comment '自定義別名',
    vector_setting json                                null comment '向量資料庫設定',
    created_at     timestamp default CURRENT_TIMESTAMP null comment '創建時間',
    updated_at     timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新時間',
    created_by     varchar(50)                         null comment '創建者',
    updated_by     varchar(50)                         null comment '最後更新者'
)ENGINE = InnoDB
 DEFAULT CHARSET = utf8mb4
 COLLATE = utf8mb4_unicode_ci COMMENT '向量資料庫設定表';

DROP TABLE IF EXISTS `llm_doc`;
CREATE TABLE llm_doc
(
    id              varchar(36)  not null comment '主鍵'
        primary key,
    knowledge_id    varchar(36)  not null comment '知識庫ID',
    name            varchar(255) null comment '名稱',
    type            varchar(50)  null comment '類型',
    url             varchar(255) null comment '網址',
    description     varchar(255) null comment '描述',
    size            bigint       null comment '文件大小',
    created_at      timestamp    not null comment '創建時間',
    updated_at      timestamp    null comment '更新時間',
    created_by      varchar(50)  null comment '創建者',
    updated_by      varchar(50)  null comment '最後更新者'
)ENGINE = InnoDB
 DEFAULT CHARSET = utf8mb4
 COLLATE = utf8mb4_unicode_ci COMMENT '文件表';

DROP TABLE IF EXISTS `llm_knowledge`;
CREATE TABLE llm_knowledge
(
    id               varchar(36)  not null comment '主鍵'
        primary key,
    vector_store_id  varchar(36)  not null comment '向量資料庫ID',
    llm_embedding_id varchar(36)  not null comment '向量模型ID',
    name             varchar(255) not null comment '名稱',
    description      varchar(255) null comment '描述',
    user_id          varchar(36)  not null comment '使用者ID',
    created_at       timestamp    not null comment '創建時間',
    updated_at       timestamp    null comment '更新時間',
    created_by       varchar(50)  null comment '創建者',
    updated_by       varchar(50)  null comment '最後更新者'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '知識庫表';

-- 使用者自訂工具表
INSERT INTO bestpartner.llm_tool_user_setting (id, alias, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('d946c66f-74c2-4b71-a140-7bf8d6eb08d5', 'openai_test', '670017b4-23d0-4339-a9c0-22b6d9446461', 'eaa798ce-e57e-4782-ac13-bc9fbcd826bf', '{"llmUrl": "https://api.openai.com/v1", "platform": "OPENAI", "llmApiKey": "xxxx", "llmModelName": "gpt-4o-mini", "datasourceUrl": "jdbc:mysql://localhost:3306/sale?allowPublicKeyRetrieval=true&useSSL=false", "datasourcePassword": "sale", "datasourceUsername": "sale", "datasourceDatabaseType": "MYSQL"}', '2025-02-22 20:28:44', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_tool_user_setting (id, alias, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('c544a5f9-0d1f-48f2-9d98-afd4b75a22e8', 'test', '670017b4-23d0-4339-a9c0-22b6d9446461', 'c14e82ca-511f-424c-b20c-a96d93cae920', '{"csi": "63ac3e98103e24d92", "apiKey": "xxxx", "timeout": 100000, "maxRetries": 10, "logRequests": true, "logResponses": true, "siteRestrict": false, "includeImages": true}', '2025-02-21 16:10:56', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_tool_user_setting (id, alias, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('2d9003be-1526-4eca-83de-7d0c65db6961', 'test', '670017b4-23d0-4339-a9c0-22b6d9446461', 'eaa798ce-e57e-4782-ac13-bc9fbcd82611', '{"llmUrl": "http://localhost:11434", "platform": "OLLAMA", "llmApiKey": null, "llmModelName": "llama3.1:latest", "datasourceUrl": "jdbc:mysql://localhost:3306/sale?allowPublicKeyRetrieval=true&useSSL=false", "datasourcePassword": "sale", "datasourceUsername": "sale", "datasourceDatabaseType": "MYSQL"}', '2025-02-22 15:10:59', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');

-- 工具分類表
INSERT INTO bestpartner.llm_tool_category (id, name, description, created_at, updated_at, created_by, updated_by) VALUES ('d09e84b9-17f2-4c07-bfa6-4dcf9201bd17', 'TEXT2SQL', 'Text2SQL群組', '2025-02-18 17:41:00', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');

-- 內建工具表
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, function_name, function_description, function_params, description, created_at, updated_at, created_by, updated_by) VALUES ('3737ec4a-2c88-490e-8301-ebc611f2433f', 'DateTool', 'tw.zipe.bastpartner.tool.DateTool', 'ea8a08e2-342d-4ade-9579-127d2d1443c5', 'CUSTOMIZE', null, 'getCurrentTime', '以台灣時間為基準，會根據不同時區取得當地日期時間', '{"zoneId": ["輸入格式為國家/城市，如:Australia/Darwin, Asia/Taipei, Africa/Harare", "String"]}', '日期工具', '2025-02-20 16:07:44', null, '', '');
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, function_name, function_description, function_params, description, created_at, updated_at, created_by, updated_by) VALUES ('c14e82ca-511f-424c-b20c-a96d93cae920', 'GoogleSearch', 'dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine', '90caee3f-2c87-48b9-8912-3dd810f62377', 'BUILT_IN', 'tw.zipe.bastpartner.tool.config.Google', '', '', null, '內建 Google 搜尋工具', '2025-02-20 16:08:47', null, '', '');
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, function_name, function_description, function_params, description, created_at, updated_at, created_by, updated_by) VALUES ('f95fda5f-4632-4a1a-9a21-2d4facbd4279', 'TavilySearch', 'dev.langchain4j.web.search.tavily.TavilyWebSearchEngine', '90caee3f-2c87-48b9-8912-3dd810f62377', 'BUILT_IN', 'tw.zipe.bastpartner.tool.config.Tavily', '', '', null, '內建 Tavily 搜尋工具', '2025-02-21 22:32:01', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');

-- 新增使用者 LLM 模型資料
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('91a777cf-9b1f-47d5-8690-73542c74d45e', '670017b4-23d0-4339-a9c0-22b6d9446461', '340580a6-74cf-4126-9c95-55741ec3996c', 'STREAMING_CHAT', 'openai_local_chat_test', '{"url": null, "topK": null, "topP": 0.5, "apiKey": "", "timeout": 6000, "platform": "OPENAI", "maxTokens": 4096, "modelName": "gpt-4o-mini", "dimensions": null, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-02-22 22:43:37', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');

-- 新增向量資料庫設定
INSERT INTO bestpartner.vector_store_setting (id, user_id, type, alias, vector_setting, created_at, updated_at, created_by, updated_by) VALUES ('b6776d0e-406b-454f-85b0-08e18aaf9248', '670017b4-23d0-4339-a9c0-22b6d9446461', 'MILVUS', 'rag-test', '{"url": "http://127.0.0.1:19530", "password": null, "username": null, "dimension": 1024, "requestLog": false, "responseLog": false, "collectionName": "test"}', '2025-03-08 14:21:47', '2025-03-08 13:52:28', '670017b4-23d0-4339-a9c0-22b6d9446461', '670017b4-23d0-4339-a9c0-22b6d9446461');
INSERT INTO bestpartner.vector_store_setting (id, user_id, type, alias, vector_setting, created_at, updated_at, created_by, updated_by) VALUES ('d3c6465c-c14e-4968-9a2a-34a05e90189b', '670017b4-23d0-4339-a9c0-22b6d9446461', 'CHROMA', 'chroma-local-test', '{"url": "http://localhost:8000", "password": null, "username": null, "dimension": 1024, "requestLog": false, "responseLog": false, "collectionName": "chroma_local_collection"}', '2025-03-08 14:23:00', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
