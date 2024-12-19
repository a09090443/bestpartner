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
