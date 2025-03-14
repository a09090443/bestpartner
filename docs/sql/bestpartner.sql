create table if not exists flyway_schema_history
(
    installed_rank int                                 not null
    primary key,
    version        varchar(50)                         null,
    description    varchar(200)                        not null,
    type           varchar(20)                         not null,
    script         varchar(1000)                       not null,
    checksum       int                                 null,
    installed_by   varchar(100)                        not null,
    installed_on   timestamp default CURRENT_TIMESTAMP not null,
    execution_time int                                 not null,
    success        tinyint(1)                          not null
    );

create index flyway_schema_history_s_idx
    on flyway_schema_history (success);

create table if not exists llm_doc
(
    id           varchar(36)  not null comment '主鍵'
    primary key,
    knowledge_id varchar(36)  not null comment '知識庫ID',
    name         varchar(255) null comment '名稱',
    type         varchar(50)  null comment '類型',
    url          varchar(255) null comment '網址',
    description  varchar(255) null comment '描述',
    size         bigint       null comment '文件大小',
    created_at   timestamp    not null comment '創建時間',
    updated_at   timestamp    null comment '更新時間',
    created_by   varchar(50)  null comment '創建者',
    updated_by   varchar(50)  null comment '最後更新者'
    )
    comment '文件表' collate = utf8mb4_unicode_ci;

create table if not exists llm_doc_slice
(
    id           varchar(36) not null comment '主鍵'
    primary key,
    knowledge_id varchar(36) not null comment '知識庫ID',
    doc_id       varchar(36) null comment '文件ID',
    content      text        null comment '切片内容',
    created_at   timestamp   not null comment '創建時間',
    updated_at   timestamp   null comment '更新時間',
    created_by   varchar(50) null comment '創建者',
    updated_by   varchar(50) null comment '最後更新者'
    )
    comment '文件切片表' collate = utf8mb4_unicode_ci;

create table if not exists llm_knowledge
(
    id               varchar(36)  not null comment '主鍵'
    primary key,
    user_id          varchar(36)  not null comment '使用者ID',
    vector_store_id  varchar(36)  not null comment '向量資料庫ID',
    llm_embedding_id varchar(36)  not null comment '向量模型ID',
    name             varchar(255) not null comment '名稱',
    description      varchar(255) null comment '描述',
    created_at       timestamp    not null comment '創建時間',
    updated_at       timestamp    null comment '更新時間',
    created_by       varchar(50)  null comment '創建者',
    updated_by       varchar(50)  null comment '最後更新者'
    )
    comment '知識庫表' collate = utf8mb4_unicode_ci;

create table if not exists llm_permission
(
    id          varchar(36)  not null comment '識別碼',
    name        varchar(50)  not null comment '權限名稱',
    num         int          not null comment '權限代號',
    description varchar(100) null comment '描述',
    created_at  timestamp    not null comment '創建時間',
    updated_at  timestamp    null comment '更新時間',
    created_by  varchar(50)  null comment '創建者',
    updated_by  varchar(50)  null comment '最後更新者',
    primary key (name, num)
    )
    comment '權限列表';

create table if not exists llm_platform
(
    id         varchar(36)                         not null comment '主鍵'
    primary key,
    name       varchar(100)                        not null comment 'LLM 平台名稱',
    created_at timestamp default CURRENT_TIMESTAMP null comment '創建時間',
    updated_at timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新時間',
    created_by varchar(50)                         null comment '創建者',
    updated_by varchar(50)                         null comment '最後更新者'
    )
    comment '模型平台清單' collate = utf8mb4_unicode_ci;

create table if not exists llm_role
(
    name        varchar(50)  not null comment '角色名稱'
    primary key,
    num         int          not null comment '角色代號',
    description varchar(100) not null comment '描述',
    created_at  timestamp    not null comment '創建時間',
    updated_at  timestamp    null comment '更新時間',
    created_by  varchar(50)  null comment '創建者',
    updated_by  varchar(50)  null comment '最後更新者',
    constraint num
    unique (num)
    )
    comment '角色列表';

create table if not exists llm_role_permission
(
    role_num       int not null comment '角色代號',
    permission_num int not null comment '權限代號',
    primary key (role_num, permission_num)
    )
    comment '角色權限對應表';

create table if not exists llm_setting
(
    id            varchar(36)                         not null comment '主鍵'
    primary key,
    user_id       varchar(36)                         not null comment '使用者ID',
    platform_id   varchar(36)                         not null comment 'LLM 平台ID',
    type          varchar(15)                         not null comment '模型類型: CHAT、EMBEDDING',
    alias         varchar(100)                        null comment '自定義別名',
    model_setting json                                null comment '模型設定',
    created_at    timestamp default CURRENT_TIMESTAMP null comment '創建時間',
    updated_at    timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新時間',
    created_by    varchar(50)                         null comment '創建者',
    updated_by    varchar(50)                         null comment '最後更新者'
    )
    comment '模型配置表' collate = utf8mb4_unicode_ci;

create table if not exists llm_tool
(
    id                   varchar(36)  not null comment '主鍵'
    primary key,
    name                 varchar(100) not null comment '工具名稱',
    class_path           varchar(100) not null comment 'Class path',
    category_id          varchar(36)  not null comment '工具群組表 ID',
    type                 varchar(10)  not null comment '工具類型',
    config_object_path   varchar(500) null comment 'Tool設定物件Path',
    function_name        varchar(100) null comment '功能名稱',
    function_description text         null comment '功能描述',
    function_params      json         null comment '功能參數',
    description          text         null comment '工具描述',
    created_at           timestamp    not null comment '創建時間',
    updated_at           timestamp    null comment '更新時間',
    created_by           varchar(50)  null comment '創建者',
    updated_by           varchar(50)  null comment '最後更新者',
    constraint NAME
    unique (name)
    )
    comment '工具表' collate = utf8mb4_unicode_ci;

create table if not exists llm_tool_category
(
    id          varchar(36) not null comment '主鍵'
    primary key,
    name        varchar(30) not null comment '群組名稱',
    description text        null comment '群組描述',
    created_at  timestamp   not null comment '創建時間',
    updated_at  timestamp   null comment '更新時間',
    created_by  varchar(50) null comment '創建者',
    updated_by  varchar(50) null comment '最後更新者',
    constraint name
    unique (name)
    )
    comment '工具群組表' collate = utf8mb4_unicode_ci;

create table if not exists llm_tool_user_setting
(
    id              varchar(36) not null comment '主鍵',
    user_id         varchar(36) not null comment '使用者ID',
    tool_id         varchar(36) not null comment '工具ID',
    setting_content json        null comment '設定內容',
    created_at      timestamp   not null comment '創建時間',
    updated_at      timestamp   null comment '更新時間',
    created_by      varchar(50) null comment '創建者',
    updated_by      varchar(50) null comment '最後更新者',
    primary key (user_id, tool_id)
    )
    comment '使用者自訂工具表' collate = utf8mb4_unicode_ci;

create table if not exists llm_user
(
    id         varchar(36)      not null comment '使用者ID',
    username   varchar(50)      not null comment '使用者名稱',
    password   varchar(128)     not null comment '密碼',
    nickname   varchar(50)      null comment '暱稱',
    phone      varchar(20)      null comment '手機',
    email      varchar(50)      not null comment 'Email'
    primary key,
    avatar     varchar(100)     null comment '頭像',
    status     char default '0' null comment '狀態 0 無效 1有效',
    created_at timestamp        not null comment '創建時間',
    updated_at timestamp        null comment '更新時間',
    created_by varchar(50)      null comment '創建者',
    updated_by varchar(50)      null comment '最後更新者'
    )
    comment '使用者列表';

create table if not exists llm_user_role
(
    user_id  varchar(36) not null comment '使用者ID',
    role_num int         not null comment '角色代號',
    primary key (user_id, role_num)
    )
    comment '使用者角色對應表';

create table if not exists system_setting
(
    id            bigint auto_increment comment '主鍵'
    primary key,
    setting_key   varchar(100)                        not null comment '設定名稱',
    setting_value text                                null comment '設定值',
    description   text                                null comment '設定說明',
    created_at    timestamp default CURRENT_TIMESTAMP null comment '創建時間',
    updated_at    timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新時間',
    created_by    varchar(50)                         null comment '創建者',
    updated_by    varchar(50)                         null comment '最後更新者',
    constraint idx_system_setting_key
    unique (setting_key)
    )
    comment '系統設定表' collate = utf8mb4_unicode_ci;

create table if not exists vector_store_setting
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
    )
    comment '向量資料庫設定表' collate = utf8mb4_unicode_ci;

-- 文件表資料
INSERT INTO bestpartner.llm_doc (id, knowledge_id, name, type, url, description, size, created_at, updated_at, created_by, updated_by) VALUES ('225efd9e-8acc-4824-81a0-807fd7fe761e', '05a716f8-f92d-4650-8aff-ea4e79c5eb5b', 'profile.txt', 'txt', '', '將文件上傳至向量資料庫', 124, '2025-03-13 20:46:58', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
-- 文件切片表資料
INSERT INTO bestpartner.llm_doc_slice (id, knowledge_id, doc_id, content, created_at, updated_at, created_by, updated_by) VALUES ('f06acf11-5f3b-41ff-b62d-83bba6eb5500', '05a716f8-f92d-4650-8aff-ea4e79c5eb5b', '225efd9e-8acc-4824-81a0-807fd7fe761e', '我是Gary，出身於台灣，身高為175公分，體重73公斤，熱愛健身及戶外活動，目前為Java工程師。', '2025-03-13 20:46:58', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
-- 知識庫表資料
INSERT INTO bestpartner.llm_knowledge (id, user_id, vector_store_id, llm_embedding_id, name, description, created_at, updated_at, created_by, updated_by) VALUES ('05a716f8-f92d-4650-8aff-ea4e79c5eb5b', '670017b4-23d0-4339-a9c0-22b6d9446461', 'b6776d0e-406b-454f-85b0-08e18aaf9248', 'e9b7f145-23a2-43cc-8bc0-3866ea265c50', '測試使用', '將文件上傳至向量資料庫', '2025-03-13 20:46:58', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
-- 權限列表資料
INSERT INTO bestpartner.llm_permission (id, name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('720ad595-712e-43b0-a4f5-8d35794ddbf6', 'admin', 0, '最高權限', '2024-12-31 20:54:10', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_permission (id, name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('f8508ea8-3c0d-479c-a56d-1c604d99b4f6', 'all', 2, '一般權限', '2024-12-31 20:54:44', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_permission (id, name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('93ded4b1-c93d-424c-9aa4-330de9d8bbf7', 'user-read', 1, '一般使用者讀取', '2024-12-31 20:52:51', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
-- 模型平台清單資料
INSERT INTO bestpartner.llm_platform (id, name, created_at, updated_at, created_by, updated_by) VALUES ('166a5745-b89c-410e-ada4-7ae3da426af5', 'OLLAMA', '2024-12-31 21:42:28', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');
INSERT INTO bestpartner.llm_platform (id, name, created_at, updated_at, created_by, updated_by) VALUES ('340580a6-74cf-4126-9c95-55741ec3996c', 'OPENAI', '2024-12-31 21:42:38', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');
-- 角色列表資料
INSERT INTO bestpartner.llm_role (name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('ADMIN', 0, '管理員', '2024-10-30 09:58:26', '2024-10-30 09:58:28', 'SYSTEM', 'SYSTEM');
INSERT INTO bestpartner.llm_role (name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('PRO_USER', 2, '進階使用者', '2024-10-30 17:52:59', '2024-10-30 17:53:02', 'SYSTEM', 'SYSTEM');
INSERT INTO bestpartner.llm_role (name, num, description, created_at, updated_at, created_by, updated_by) VALUES ('USER', 1, '一般使用者', '2024-10-30 09:58:26', '2024-10-30 09:58:28', 'SYSTEM', 'SYSTEM');
-- 角色權限對應表資料
INSERT INTO bestpartner.llm_role_permission (role_num, permission_num) VALUES (0, 0);
INSERT INTO bestpartner.llm_role_permission (role_num, permission_num) VALUES (0, 1);
INSERT INTO bestpartner.llm_role_permission (role_num, permission_num) VALUES (1, 1);
-- 模型配置表資料
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('0b471493-e45a-47da-b73b-58d88eba6194', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '166a5745-b89c-410e-ada4-7ae3da426af5', 'EMBEDDING', 'ollama_local_embedding_test', '{"url": "http://localhost:11434", "topK": 40, "topP": 0.5, "apiKey": null, "timeout": 60000, "platform": "OLLAMA", "maxTokens": 4096, "modelName": "bge-m3:latest", "dimensions": 1024, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-01 20:41:16', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('4069f3c2-ba50-4eed-b3d3-0cc53478dd9a', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '166a5745-b89c-410e-ada4-7ae3da426af5', 'CHAT', 'ollama_local_chat_test', '{"url": "http://localhost:11434", "topK": 40, "topP": 0.5, "apiKey": null, "timeout": 60000, "platform": "OLLAMA", "maxTokens": 4096, "modelName": "llama3.1:latest", "dimensions": null, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-01 20:41:05', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('42758e05-0c8f-4acd-9f72-3b9f6f38c5df', '670017b4-23d0-4339-a9c0-22b6d9446461', '166a5745-b89c-410e-ada4-7ae3da426af5', 'CHAT', 'ollama_user_chat_test', '{"url": "http://localhost:11434", "topK": 40, "topP": 0.5, "apiKey": null, "timeout": 60000, "platform": "OLLAMA", "maxTokens": 4096, "modelName": "llama3.1:latest", "dimensions": null, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-03 10:50:04', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('91a777cf-9b1f-47d5-8690-73542c74d45e', '670017b4-23d0-4339-a9c0-22b6d9446461', '340580a6-74cf-4126-9c95-55741ec3996c', 'STREAMING_CHAT', 'openai_local_chat_test', '{"url": null, "topK": null, "topP": 0.5, "apiKey": "sk-proj-xxxxx", "timeout": 6000, "platform": "OPENAI", "maxTokens": 4096, "modelName": "gpt-4o-mini", "dimensions": null, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-02-22 22:43:37', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('9c6838a3-7d20-4aca-9730-f198a8eb192c', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '340580a6-74cf-4126-9c95-55741ec3996c', 'CHAT', 'openai_local_chat_test', '{"url": null, "topK": null, "topP": 0.5, "apiKey": "sk-proj-g0SN3T2-xxxx", "timeout": 6000, "platform": null, "maxTokens": 4096, "modelName": "gpt-4o-mini", "dimensions": null, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-01 20:39:24', '2025-01-01 21:35:24', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', 'c88f57c8-ad26-4ea0-9f71-a65995b49357');
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('e9b7f145-23a2-43cc-8bc0-3866ea265c50', '670017b4-23d0-4339-a9c0-22b6d9446461', '166a5745-b89c-410e-ada4-7ae3da426af5', 'EMBEDDING', 'ollama_user_embedding_test', '{"url": "http://localhost:11434", "topK": 40, "topP": 0.5, "apiKey": null, "timeout": 60000, "platform": "OLLAMA", "maxTokens": 4096, "modelName": "bge-m3:latest", "dimensions": 1024, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-03 10:50:30', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_setting (id, user_id, platform_id, type, alias, model_setting, created_at, updated_at, created_by, updated_by) VALUES ('fbeafc72-eda0-4a2b-b79e-ed88f53db29f', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '340580a6-74cf-4126-9c95-55741ec3996c', 'EMBEDDING', 'local_embedding_test', '{"url": null, "topK": null, "topP": 0.5, "apiKey": "sk-proj-g0SN3T2-xxxx", "timeout": 6000, "platform": null, "maxTokens": 4096, "modelName": "text-embedding-3-small", "dimensions": 1536, "logRequests": true, "temperature": 0.7, "logResponses": true}', '2025-01-01 20:40:44', '2025-01-01 21:39:03', 'c88f57c8-ad26-4ea0-9f71-a65995b49357', 'c88f57c8-ad26-4ea0-9f71-a65995b49357');
-- 工具表資料
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, function_name, function_description, function_params, description, created_at, updated_at, created_by, updated_by) VALUES ('3737ec4a-2c88-490e-8301-ebc611f2433f', 'DateTool', 'tw.zipe.bastpartner.tool.DateTool', 'ea8a08e2-342d-4ade-9579-127d2d1443c5', 'CUSTOMIZE', null, 'getCurrentTime', '以台灣時間為基準，會根據不同時區取得當地日期時間', '{"zoneId": ["輸入格式為國家/城市，如:Australia/Darwin, Asia/Taipei, Africa/Harare", "String"]}', '日期工具', '2025-02-20 16:07:44', null, '', '');
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, function_name, function_description, function_params, description, created_at, updated_at, created_by, updated_by) VALUES ('c14e82ca-511f-424c-b20c-a96d93cae920', 'GoogleSearch', 'dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine', '90caee3f-2c87-48b9-8912-3dd810f62377', 'BUILT_IN', 'tw.zipe.bastpartner.tool.config.Google', '', '', null, '內建 Google 搜尋工具', '2025-02-20 16:08:47', null, '', '');
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, function_name, function_description, function_params, description, created_at, updated_at, created_by, updated_by) VALUES ('f95fda5f-4632-4a1a-9a21-2d4facbd4279', 'TavilySearch', 'dev.langchain4j.web.search.tavily.TavilyWebSearchEngine', '90caee3f-2c87-48b9-8912-3dd810f62377', 'BUILT_IN', 'tw.zipe.bastpartner.tool.config.Tavily', '', '', null, '內建 Tavily 搜尋工具', '2025-02-21 22:32:01', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');
-- 工具群組表資料
INSERT INTO bestpartner.llm_tool_category (id, name, description, created_at, updated_at, created_by, updated_by) VALUES ('90caee3f-2c87-48b9-8912-3dd810f62377', 'WEB_SEARCH', '網頁搜尋群組', '2024-12-05 15:56:36', '2024-12-05 15:56:36', 'admin', 'admin');
INSERT INTO bestpartner.llm_tool_category (id, name, description, created_at, updated_at, created_by, updated_by) VALUES ('d09e84b9-17f2-4c07-bfa6-4dcf9201bd17', 'TEXT2SQL', 'Text2SQL群組', '2025-02-18 17:41:00', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');
INSERT INTO bestpartner.llm_tool_category (id, name, description, created_at, updated_at, created_by, updated_by) VALUES ('ea8a08e2-342d-4ade-9579-127d2d1443c5', 'DATE', '日期群組', '2024-12-05 15:57:31', '2024-12-05 15:57:31', 'admin', 'admin');
-- 使用者自訂工具表資料
INSERT INTO bestpartner.llm_tool_user_setting (id, alias, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('d946c66f-74c2-4b71-a140-7bf8d6eb08d5', 'openai_test', '670017b4-23d0-4339-a9c0-22b6d9446461', 'eaa798ce-e57e-4782-ac13-bc9fbcd826bf', '{"llmUrl": "https://api.openai.com/v1", "platform": "OPENAI", "llmApiKey": "sk-proj-xxxx", "llmModelName": "gpt-4o-mini", "datasourceUrl": "jdbc:mysql://localhost:3306/sale?allowPublicKeyRetrieval=true&useSSL=false", "datasourcePassword": "sale", "datasourceUsername": "sale", "datasourceDatabaseType": "MYSQL"}', '2025-02-22 20:28:44', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_tool_user_setting (id, alias, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('c544a5f9-0d1f-48f2-9d98-afd4b75a22e8', 'test', '670017b4-23d0-4339-a9c0-22b6d9446461', 'c14e82ca-511f-424c-b20c-a96d93cae920', '{"csi": "63ac3e98103e24d92", "apiKey": "xxxxx", "timeout": 100000, "maxRetries": 10, "logRequests": true, "logResponses": true, "siteRestrict": false, "includeImages": true}', '2025-02-21 16:10:56', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
INSERT INTO bestpartner.llm_tool_user_setting (id, alias, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('2d9003be-1526-4eca-83de-7d0c65db6961', 'test', '670017b4-23d0-4339-a9c0-22b6d9446461', 'eaa798ce-e57e-4782-ac13-bc9fbcd82611', '{"llmUrl": "http://localhost:11434", "platform": "OLLAMA", "llmApiKey": null, "llmModelName": "llama3.1:latest", "datasourceUrl": "jdbc:mysql://localhost:3306/sale?allowPublicKeyRetrieval=true&useSSL=false", "datasourcePassword": "sale", "datasourceUsername": "sale", "datasourceDatabaseType": "MYSQL"}', '2025-02-22 15:10:59', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
-- 使用者列表資料
INSERT INTO bestpartner.llm_user (id, username, password, nickname, phone, email, avatar, status, created_at, updated_at, created_by, updated_by) VALUES ('c88f57c8-ad26-4ea0-9f71-a65995b49357', 'admin', 'c7ad44cbad762a5da0a452f9e854fdc1e0e7a52a38015f23f3eab1d80b931dd472634dfac71cd34ebc35d16ab7fb8a90c81f975113d6c7538dc69dd8de9077ec', '', '1234567890', 'admin@bestpartner.com.tw', '', '1', '2024-10-29 22:27:48', '2024-10-29 22:27:48', '', '');
INSERT INTO bestpartner.llm_user (id, username, password, nickname, phone, email, avatar, status, created_at, updated_at, created_by, updated_by) VALUES ('670017b4-23d0-4339-a9c0-22b6d9446461', 'test_user', 'b14361404c078ffd549c03db443c3fede2f3e534d73f78f77301ed97d4a436a9fd9db05ee8b325c0ad36438b43fec8510c204fc1c1edb21d0941c00e9e2c1ce2', 'test_user', '0987654321', 'test@partmer.com.tw', 'test.jpg', '1', '2024-10-30 22:50:07', '2024-10-30 22:51:15', '', '');
INSERT INTO bestpartner.llm_user (id, username, password, nickname, phone, email, avatar, status, created_at, updated_at, created_by, updated_by) VALUES ('5554f255-08d5-4069-99d2-928372d4a87b', 'user', 'b14361404c078ffd549c03db443c3fede2f3e534d73f78f77301ed97d4a436a9fd9db05ee8b325c0ad36438b43fec8510c204fc1c1edb21d0941c00e9e2c1ce2', '', '1234567890', 'user@bestpartner.com.tw', '', '1', '2024-12-23 14:37:11', null, '', '');
-- 使用者角色對應表資料
INSERT INTO bestpartner.llm_user_role (user_id, role_num) VALUES ('5554f255-08d5-4069-99d2-928372d4a87b', 1);
INSERT INTO bestpartner.llm_user_role (user_id, role_num) VALUES ('c88f57c8-ad26-4ea0-9f71-a65995b49357', 0);
-- 系統設定表資料
INSERT INTO bestpartner.system_setting (id, setting_key, setting_value, description, created_at, updated_at, created_by, updated_by) VALUES (1, 'default_llm_platform', 'OPENAI', 'system default llm_platform', '2024-10-19 20:08:22', '2025-01-04 12:57:17', 'system', 'system');
-- 向量資料庫設定表資料
INSERT INTO bestpartner.vector_store_setting (id, user_id, type, alias, vector_setting, created_at, updated_at, created_by, updated_by) VALUES ('b6776d0e-406b-454f-85b0-08e18aaf9248', '670017b4-23d0-4339-a9c0-22b6d9446461', 'MILVUS', 'rag-test', '{"url": "http://127.0.0.1:19530", "password": null, "username": null, "dimension": 1024, "requestLog": false, "responseLog": false, "collectionName": "test"}', '2025-03-08 14:21:47', '2025-03-08 13:52:28', '670017b4-23d0-4339-a9c0-22b6d9446461', '670017b4-23d0-4339-a9c0-22b6d9446461');
INSERT INTO bestpartner.vector_store_setting (id, user_id, type, alias, vector_setting, created_at, updated_at, created_by, updated_by) VALUES ('d3c6465c-c14e-4968-9a2a-34a05e90189b', '670017b4-23d0-4339-a9c0-22b6d9446461', 'CHROMA', 'chroma-local-test', '{"url": "http://localhost:8000", "password": null, "username": null, "dimension": 1024, "requestLog": false, "responseLog": false, "collectionName": "chroma_local_collection"}', '2025-03-08 14:23:00', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');
