create table flyway_schema_history
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

create table llm_doc
(
    id              varchar(36)  not null comment '主鍵'
        primary key,
    knowledge_id    varchar(36)  not null comment '知識庫ID',
    vector_store_id varchar(36)  null comment '向量資料庫ID',
    name            varchar(255) null comment '名稱',
    type            varchar(50)  null comment '類型',
    url             varchar(255) null comment '網址',
    description     varchar(255) null comment '描述',
    size            bigint       null comment '文件大小',
    created_at      timestamp    not null comment '創建時間',
    updated_at      timestamp    null comment '更新時間',
    created_by      varchar(50)  null comment '創建者',
    updated_by      varchar(50)  null comment '最後更新者'
)
    comment '文件表' collate = utf8mb4_unicode_ci;

create table llm_doc_slice
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

create table llm_permission
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

create table llm_platform
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

create table llm_role
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

create table llm_role_permission
(
    role_num       int not null comment '角色代號',
    permission_num int not null comment '權限代號',
    primary key (role_num, permission_num)
)
    comment '角色權限對應表';

create table llm_setting
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

create table llm_tool
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

create table llm_tool_category
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

create table llm_tool_user_setting
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

create table llm_user
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

create table llm_user_role
(
    user_id  varchar(36) not null comment '使用者ID',
    role_num int         not null comment '角色代號',
    primary key (user_id, role_num)
)
    comment '使用者角色對應表';

create table system_setting
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

create table vector_store_setting
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

