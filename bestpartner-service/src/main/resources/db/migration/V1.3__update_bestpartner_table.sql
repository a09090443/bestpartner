DROP TABLE IF EXISTS `llm_tool`;
CREATE TABLE `llm_tool`
(
    `id`             VARCHAR(36)         NOT NULL COMMENT '主鍵',
    `name`           VARCHAR(100) UNIQUE NOT NULL COMMENT '工具名稱',
    `class_path`     VARCHAR(100)        NOT NULL COMMENT 'Class path',
    `category_id`    VARCHAR(36)         NOT NULL COMMENT '工具群組表 ID',
    `type`           VARCHAR(10)         NOT NULL COMMENT '工具類型',
    `setting_fields` TEXT      DEFAULT NULL COMMENT '設定值欄位名稱,以逗號分隔,並要以正確順序填寫',
    `description`    TEXT      DEFAULT NULL COMMENT '工具描述',
    `created_at`     TIMESTAMP           NOT NULL COMMENT '創建時間',
    `updated_at`     TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    `created_by`     VARCHAR(50) COMMENT '創建者',
    `updated_by`     VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具表';

DROP TABLE IF EXISTS `llm_tool_category`;
CREATE TABLE `llm_tool_category`
(
    `id`          VARCHAR(36)        NOT NULL COMMENT '主鍵',
    `name`        VARCHAR(30) UNIQUE NOT NULL COMMENT '群組名稱',
    `description` TEXT      DEFAULT NULL COMMENT '群組描述',
    `created_at`  TIMESTAMP          NOT NULL COMMENT '創建時間',
    `updated_at`  TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    `created_by`  VARCHAR(50) COMMENT '創建者',
    `updated_by`  VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具群組表';

DROP TABLE IF EXISTS `llm_tool_user_setting`;
CREATE TABLE `llm_tool_user_setting`
(
    `id`              VARCHAR(36) NOT NULL COMMENT '主鍵',
    `user_id`         VARCHAR(36) NOT NULL COMMENT '使用者ID',
    `tool_id`         VARCHAR(36) NOT NULL COMMENT '工具ID',
    `setting_content` JSON      DEFAULT NULL COMMENT '設定內容',
    `created_at`      TIMESTAMP   NOT NULL COMMENT '創建時間',
    `updated_at`      TIMESTAMP DEFAULT NULL COMMENT '更新時間',
    `created_by`      VARCHAR(50) COMMENT '創建者',
    `updated_by`      VARCHAR(50) COMMENT '最後更新者',
    PRIMARY KEY (`user_id`, `tool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用者自訂工具表';

-- 註冊工具
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, setting_fields, description, created_at, updated_at, created_by, updated_by) VALUES ('7af4b81f-0dbe-4055-a9f9-306b7e031c72', 'GoogleSearch', 'dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine', '90caee3f-2c87-48b9-8912-3dd810f62377', 'BUILT_IN', 'apiKey,csi,siteRestrict,includeImages,timeout,maxRetries,logRequests,logResponses', '內建 Google 搜尋工具', '2024-12-05 15:56:36', '2024-12-05 15:56:36', 'admin', 'admin');
INSERT INTO bestpartner.llm_tool (id, name, class_path, category_id, type, setting_fields, description, created_at, updated_at, created_by, updated_by) VALUES ('b7c23848-2d09-461d-9000-721666228347', 'DateTool', 'tw.zipe.bastpartner.tool.DateTool', 'ea8a08e2-342d-4ade-9579-127d2d1443c5', 'BUILT_IN', null, '內建日期工具', '2024-12-05 15:57:31', '2024-12-05 15:57:31', 'admin', 'admin');

-- 註冊工具群組
INSERT INTO bestpartner.llm_tool_category (id, name, description, created_at, updated_at, created_by, updated_by) VALUES ('90caee3f-2c87-48b9-8912-3dd810f62377', 'WEB_SEARCH', '網頁搜尋群組', '2024-12-05 15:56:36', '2024-12-05 15:56:36', 'admin', 'admin');
INSERT INTO bestpartner.llm_tool_category (id, name, description, created_at, updated_at, created_by, updated_by) VALUES ('ea8a08e2-342d-4ade-9579-127d2d1443c5', 'DATE', '日期群組', '2024-12-05 15:57:31', '2024-12-05 15:57:31', 'admin', 'admin');
