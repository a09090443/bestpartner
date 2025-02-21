-- 使用者自訂工具表
DROP TABLE IF EXISTS llm_tool_user_setting;
CREATE TABLE llm_tool_user_setting
(
    id              VARCHAR(36) NOT NULL COMMENT '主鍵',
    alias           VARCHAR(50) NULL COMMENT '別名',
    user_id         VARCHAR(36) NOT NULL COMMENT '使用者ID',
    tool_id         VARCHAR(36) NOT NULL COMMENT '工具ID',
    setting_content JSON        NULL COMMENT '設定內容',
    created_at      TIMESTAMP   NOT NULL COMMENT '創建時間',
    updated_at      TIMESTAMP   NULL COMMENT '更新時間',
    created_by      VARCHAR(50) NULL COMMENT '創建者',
    updated_by      VARCHAR(50) NULL COMMENT '最後更新者',
    PRIMARY KEY (user_id, tool_id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用者自訂工具表';

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
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具表';

-- 使用者自訂工具表
INSERT INTO bestpartner.llm_tool_user_setting (id, alias, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('2e8d3421-9ebb-44fa-a468-4fc2d8196d5a', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '0c743a37-1f98-444f-bba8-b063604e5bfb', '{"csi": "xxxxxx", "apiKey": "AIxxxxx", "timeout": 100000, "maxRetries": 5, "logRequests": true, "logResponses": true, "includeImages": false}', '2025-01-13 10:45:56', '2025-01-14 11:44:45', '670017b4-23d0-4339-a9c0-22b6d9446461', '670017b4-23d0-4339-a9c0-22b6d9446461');
INSERT INTO bestpartner.llm_tool_user_setting (id, alias, user_id, tool_id, setting_content, created_at, updated_at, created_by, updated_by) VALUES ('5eac36a5-e780-4992-a1b6-494829f782a3', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '528e6798-8e4e-4233-97c0-3c6fce76ba0d', '{"apiKey": "tvly-uxxxxxx", "timeout": 100000, "includeAnswer": true, "excludeDomains": [], "includeDomains": [], "includeRawContent": false}', '2025-01-13 10:45:19', null, '670017b4-23d0-4339-a9c0-22b6d9446461', '');

-- 工具分類表
INSERT INTO bestpartner.llm_tool_category (id, name, description, created_at, updated_at, created_by, updated_by) VALUES ('d09e84b9-17f2-4c07-bfa6-4dcf9201bd17', 'TEXT2SQL', 'Text2SQL群組', '2025-02-18 17:41:00', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');

-- 內建工具表
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, config_object_path, description, created_at, updated_at, created_by, updated_by) VALUES ('169614bc-7b87-4397-8fc1-ebbfc3f555fe', 'Text2SQLTool', 'tw.zipe.bastpartner.tool.text2sql.Text2SQLTool', 'd09e84b9-17f2-4c07-bfa6-4dcf9201bd17', 'BUILT_IN', 'tw.zipe.bastpartner.tool.text2sql.config.Text2SQL', '內建TEXT2SQL工具', '2025-02-19 15:48:34', null, 'c88f57c8-ad26-4ea0-9f71-a65995b49357', '');

