-- MCP server 列表
DROP TABLE IF EXISTS llm_mcp_server;
CREATE TABLE llm_mcp_server
(
    id              VARCHAR(36) NOT NULL COMMENT '主鍵',
    name            VARCHAR(50) UNIQUE NOT NULL COMMENT '名稱',
    command_setting JSON        NOT NULL COMMENT '指令設定',
    type            VARCHAR(5)  NOT NULL COMMENT '類型',
    description     TEXT        NULL COMMENT '描述',
    created_at      TIMESTAMP   NOT NULL COMMENT '創建時間',
    updated_at      TIMESTAMP   NULL COMMENT '更新時間',
    created_by      VARCHAR(50) NULL COMMENT '創建者',
    updated_by      VARCHAR(50) NULL COMMENT '最後更新者',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='MCP server 列表';

-- MCP server 使用者自訂表
DROP TABLE IF EXISTS llm_mcp_user_setting;
CREATE TABLE llm_mcp_user_setting
(
    id              VARCHAR(36) NOT NULL COMMENT '主鍵',
    alias           VARCHAR(50) NOT NULL COMMENT '別名',
    user_id         VARCHAR(36) NOT NULL COMMENT '使用者ID',
    mcp_id          VARCHAR(36) NOT NULL COMMENT 'MCP SERVER ID',
    setting_content JSON        NULL COMMENT '設定內容',
    created_at      TIMESTAMP   NOT NULL COMMENT '創建時間',
    updated_at      TIMESTAMP   NULL COMMENT '更新時間',
    created_by      VARCHAR(50) NULL COMMENT '創建者',
    updated_by      VARCHAR(50) NULL COMMENT '最後更新者',
    PRIMARY KEY (alias, user_id, mcp_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='MCP server 使用者自訂表';
