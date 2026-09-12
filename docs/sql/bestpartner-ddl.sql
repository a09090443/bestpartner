DROP TABLE IF EXISTS "llm_doc";
CREATE TABLE "llm_doc" (
                           "id" varchar(36) NOT NULL,
                           "knowledge_id" varchar(36) NOT NULL,
                           "name" varchar(255) DEFAULT NULL,
                           "type" varchar(50) DEFAULT NULL,
                           "url" varchar(255) DEFAULT NULL,
                           "description" varchar(255) DEFAULT NULL,
                           "size" bigint DEFAULT NULL,
                           "created_at" timestamp NOT NULL,
                           "updated_at" timestamp NULL DEFAULT NULL,
                           "created_by" varchar(50) DEFAULT NULL,
                           "updated_by" varchar(50) DEFAULT NULL,
                           PRIMARY KEY ("id")
);

DROP TABLE IF EXISTS "llm_doc_slice";
CREATE TABLE "llm_doc_slice" (
                                 "id" varchar(36) NOT NULL,
                                 "knowledge_id" varchar(36) NOT NULL,
                                 "doc_id" varchar(36) DEFAULT NULL,
                                 "content" text,
                                 "created_at" timestamp NOT NULL,
                                 "updated_at" timestamp NULL DEFAULT NULL,
                                 "created_by" varchar(50) DEFAULT NULL,
                                 "updated_by" varchar(50) DEFAULT NULL,
                                 PRIMARY KEY ("id")
);

DROP TABLE IF EXISTS "llm_knowledge";
CREATE TABLE "llm_knowledge" (
                                 "id" varchar(36) NOT NULL,
                                 "user_id" varchar(36) NOT NULL,
                                 "vector_store_id" varchar(36) NOT NULL,
                                 "llm_embedding_id" varchar(36) NOT NULL,
                                 "name" varchar(255) NOT NULL,
                                 "description" varchar(255) DEFAULT NULL,
                                 "created_at" timestamp NOT NULL,
                                 "updated_at" timestamp NULL DEFAULT NULL,
                                 "created_by" varchar(50) DEFAULT NULL,
                                 "updated_by" varchar(50) DEFAULT NULL,
                                 PRIMARY KEY ("id")
);

DROP TABLE IF EXISTS "llm_mcp_server";
CREATE TABLE "llm_mcp_server" (
                                  "id" varchar(36) NOT NULL,
                                  "name" varchar(50) NOT NULL,
                                  "command_setting" json NOT NULL,
                                  "type" varchar(5) NOT NULL,
                                  "description" text,
                                  "created_at" timestamp NOT NULL,
                                  "updated_at" timestamp NULL DEFAULT NULL,
                                  "created_by" varchar(50) DEFAULT NULL,
                                  "updated_by" varchar(50) DEFAULT NULL,
                                  PRIMARY KEY ("id"),
                                  UNIQUE ("name")
);

DROP TABLE IF EXISTS "llm_mcp_user_setting";
CREATE TABLE "llm_mcp_user_setting" (
                                        "id" varchar(36) NOT NULL,
                                        "alias" varchar(50) NOT NULL,
                                        "user_id" varchar(36) NOT NULL,
                                        "mcp_id" varchar(36) NOT NULL,
                                        "setting_content" text DEFAULT NULL, -- 整包 AES-GCM 加密（"{iv}${encrypted}"），非 JSON
                                        "created_at" timestamp NOT NULL,
                                        "updated_at" timestamp NULL DEFAULT NULL,
                                        "created_by" varchar(50) DEFAULT NULL,
                                        "updated_by" varchar(50) DEFAULT NULL,
                                        PRIMARY KEY ("alias","user_id","mcp_id")
);

DROP TABLE IF EXISTS "llm_permission";
CREATE TABLE "llm_permission" (
                                  "id" varchar(36) NOT NULL,
                                  "name" varchar(50) NOT NULL,
                                  "num" int NOT NULL,
                                  "description" varchar(100) DEFAULT NULL,
                                  "created_at" timestamp NOT NULL,
                                  "updated_at" timestamp NULL DEFAULT NULL,
                                  "created_by" varchar(50) DEFAULT NULL,
                                  "updated_by" varchar(50) DEFAULT NULL,
                                  PRIMARY KEY ("name","num")
);

DROP TABLE IF EXISTS "llm_platform";
CREATE TABLE "llm_platform" (
                                "id" varchar(36) NOT NULL,
                                "name" varchar(100) NOT NULL,
                                "created_at" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                "updated_at" timestamp DEFAULT CURRENT_TIMESTAMP,
                                "created_by" varchar(50) DEFAULT NULL,
                                "updated_by" varchar(50) DEFAULT NULL,
                                PRIMARY KEY ("id")
);

DROP TABLE IF EXISTS "llm_role";
CREATE TABLE "llm_role" (
                            "name" varchar(50) NOT NULL,
                            "num" int NOT NULL,
                            "description" varchar(100) NOT NULL,
                            "created_at" timestamp NOT NULL,
                            "updated_at" timestamp NULL DEFAULT NULL,
                            "created_by" varchar(50) DEFAULT NULL,
                            "updated_by" varchar(50) DEFAULT NULL,
                            PRIMARY KEY ("name"),
                            UNIQUE ("num")
);

DROP TABLE IF EXISTS "llm_role_permission";
CREATE TABLE "llm_role_permission" (
                                       "role_num" int NOT NULL,
                                       "permission_num" int NOT NULL,
                                       PRIMARY KEY ("role_num","permission_num")
);

DROP TABLE IF EXISTS "llm_setting";
CREATE TABLE "llm_setting" (
                               "id" varchar(36) NOT NULL,
                               "user_id" varchar(36) NOT NULL,
                               "platform_id" varchar(36) NOT NULL,
                               "type" varchar(15) NOT NULL,
                               "alias" varchar(100) DEFAULT NULL,
                               "model_setting" json DEFAULT NULL,
                               "api_key" varchar(500) DEFAULT NULL,
                               "created_at" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                               "updated_at" timestamp DEFAULT CURRENT_TIMESTAMP,
                               "created_by" varchar(50) DEFAULT NULL,
                               "updated_by" varchar(50) DEFAULT NULL,
                               PRIMARY KEY ("id")
);

DROP TABLE IF EXISTS "llm_tool";
CREATE TABLE "llm_tool" (
                            "id" varchar(36) NOT NULL,
                            "name" varchar(100) NOT NULL,
                            "class_path" varchar(100) NOT NULL,
                            "category_id" varchar(36) NOT NULL,
                            "type" varchar(10) NOT NULL,
                            "config_object_path" varchar(500) DEFAULT NULL,
                            "function_name" varchar(100) DEFAULT NULL,
                            "function_description" text,
                            "function_params" json DEFAULT NULL,
                            "description" text,
                            "created_at" timestamp NOT NULL,
                            "updated_at" timestamp NULL DEFAULT NULL,
                            "created_by" varchar(50) DEFAULT NULL,
                            "updated_by" varchar(50) DEFAULT NULL,
                            PRIMARY KEY ("id"),
                            UNIQUE ("name")
);

DROP TABLE IF EXISTS "llm_tool_category";
CREATE TABLE "llm_tool_category" (
                                     "id" varchar(36) NOT NULL,
                                     "name" varchar(30) NOT NULL,
                                     "description" text,
                                     "created_at" timestamp NOT NULL,
                                     "updated_at" timestamp NULL DEFAULT NULL,
                                     "created_by" varchar(50) DEFAULT NULL,
                                     "updated_by" varchar(50) DEFAULT NULL,
                                     PRIMARY KEY ("id"),
                                     UNIQUE ("name")
);

DROP TABLE IF EXISTS "llm_tool_user_setting";
CREATE TABLE "llm_tool_user_setting" (
                                         "id" varchar(36) NOT NULL,
                                         "alias" varchar(50) NOT NULL,
                                         "user_id" varchar(36) NOT NULL,
                                         "tool_id" varchar(36) NOT NULL,
                                         "setting_content" json DEFAULT NULL,
                                         "created_at" timestamp NOT NULL,
                                         "updated_at" timestamp NULL DEFAULT NULL,
                                         "created_by" varchar(50) DEFAULT NULL,
                                         "updated_by" varchar(50) DEFAULT NULL,
                                         PRIMARY KEY ("alias","user_id","tool_id")
);

DROP TABLE IF EXISTS "llm_user";
CREATE TABLE "llm_user" (
                            "id" varchar(36) NOT NULL,
                            "username" varchar(50) NOT NULL,
                            "password" varchar(128) NOT NULL,
                            "nickname" varchar(50) DEFAULT NULL,
                            "phone" varchar(20) DEFAULT NULL,
                            "email" varchar(50) NOT NULL,
                            "avatar" varchar(100) DEFAULT NULL,
                            "status" char(1) DEFAULT '0',
                            "created_at" timestamp NOT NULL,
                            "updated_at" timestamp NULL DEFAULT NULL,
                            "created_by" varchar(50) DEFAULT NULL,
                            "updated_by" varchar(50) DEFAULT NULL,
                            PRIMARY KEY ("email")
);

DROP TABLE IF EXISTS "llm_user_role";
CREATE TABLE "llm_user_role" (
                                 "user_id" varchar(36) NOT NULL,
                                 "role_num" int NOT NULL,
                                 PRIMARY KEY ("user_id","role_num")
);

DROP TABLE IF EXISTS "system_setting";
CREATE TABLE "system_setting" (
                                  "id" bigserial,
                                  "setting_key" varchar(100) NOT NULL,
                                  "setting_value" text,
                                  "description" text,
                                  "created_at" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                  "updated_at" timestamp DEFAULT CURRENT_TIMESTAMP,
                                  "created_by" varchar(50) DEFAULT NULL,
                                  "updated_by" varchar(50) DEFAULT NULL,
                                  PRIMARY KEY ("id"),
                                  UNIQUE ("setting_key")
);

DROP TABLE IF EXISTS "vector_store_setting";
CREATE TABLE "vector_store_setting" (
                                        "id" varchar(36) NOT NULL,
                                        "user_id" varchar(36) NOT NULL,
                                        "type" varchar(50) NOT NULL,
                                        "alias" varchar(100) DEFAULT NULL,
                                        "url" varchar(500) DEFAULT NULL,
                                        "username" varchar(100) DEFAULT NULL,
                                        "password" varchar(200) DEFAULT NULL,
                                        "collection_name" varchar(200) DEFAULT NULL,
                                        "dimension" int DEFAULT NULL,
                                        "request_log" boolean DEFAULT false,
                                        "response_log" boolean DEFAULT false,
                                        "created_at" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                        "updated_at" timestamp DEFAULT CURRENT_TIMESTAMP,
                                        "created_by" varchar(50) DEFAULT NULL,
                                        "updated_by" varchar(50) DEFAULT NULL,
                                        PRIMARY KEY ("id")
);

DROP TABLE IF EXISTS "llm_skill_resource";
DROP TABLE IF EXISTS "llm_skill";
CREATE TABLE "llm_skill" (
    "id" varchar(36) NOT NULL,
    "user_id" varchar(36) NOT NULL,
    "name" varchar(255) NOT NULL,
    "description" text,
    "content" text,
    "dir_path" varchar(500) DEFAULT NULL,
    "scope" varchar(10) NOT NULL DEFAULT 'USER',
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NULL DEFAULT NULL,
    "created_by" varchar(50) DEFAULT NULL,
    "updated_by" varchar(50) DEFAULT NULL,
    PRIMARY KEY ("id"),
    UNIQUE ("user_id", "name")
);

CREATE TABLE "llm_skill_resource" (
    "id" varchar(36) NOT NULL,
    "skill_id" varchar(36) NOT NULL,
    "relative_path" varchar(255) NOT NULL,
    "content" text,
    "created_at" timestamp NOT NULL,
    "updated_at" timestamp NULL DEFAULT NULL,
    "created_by" varchar(50) DEFAULT NULL,
    "updated_by" varchar(50) DEFAULT NULL,
    PRIMARY KEY ("id")
);

-- ============================================================
-- Workflow 引擎 v1（n8n-like 視覺化編排）
-- 設計來源：docs/workflow-engine/system-design.md §1
-- ============================================================

DROP TABLE IF EXISTS "llm_workflow_node_execution";
DROP TABLE IF EXISTS "llm_workflow_execution";
DROP TABLE IF EXISTS "llm_workflow_trigger";
DROP TABLE IF EXISTS "llm_workflow_edge";
DROP TABLE IF EXISTS "llm_workflow_node";
DROP TABLE IF EXISTS "llm_workflow";

CREATE TABLE "llm_workflow" (
    "id"          varchar(36)  NOT NULL,
    "user_id"     varchar(36)  NOT NULL,
    "name"        varchar(100) NOT NULL,
    "description" varchar(255) DEFAULT NULL,
    "status"      varchar(10)  NOT NULL DEFAULT 'DRAFT',
    "version"     int          NOT NULL DEFAULT 1,
    "canvas_meta" json         DEFAULT NULL,
    "created_at"  timestamp    NOT NULL,
    "updated_at"  timestamp    NULL DEFAULT NULL,
    "created_by"  varchar(50)  DEFAULT NULL,
    "updated_by"  varchar(50)  DEFAULT NULL,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_workflow_user" ON "llm_workflow" ("user_id");
CREATE INDEX "idx_workflow_status" ON "llm_workflow" ("status");

CREATE TABLE "llm_workflow_node" (
    "id"          varchar(36)  NOT NULL,
    "workflow_id" varchar(36)  NOT NULL,
    "node_key"    varchar(64)  NOT NULL,
    "type"        varchar(30)  NOT NULL,
    "name"        varchar(100) DEFAULT NULL,
    "position_x"  double precision NOT NULL,
    "position_y"  double precision NOT NULL,
    "config"      json         NOT NULL,
    "created_at"  timestamp    NOT NULL,
    "updated_at"  timestamp    NULL DEFAULT NULL,
    "created_by"  varchar(50)  DEFAULT NULL,
    "updated_by"  varchar(50)  DEFAULT NULL,
    PRIMARY KEY ("id"),
    UNIQUE ("workflow_id", "node_key")
);
CREATE INDEX "idx_workflow_node_wf" ON "llm_workflow_node" ("workflow_id");

CREATE TABLE "llm_workflow_edge" (
    "id"              varchar(36)  NOT NULL,
    "workflow_id"     varchar(36)  NOT NULL,
    "source_node_key" varchar(64)  NOT NULL,
    "target_node_key" varchar(64)  NOT NULL,
    "source_handle"   varchar(64)  DEFAULT NULL,
    "target_handle"   varchar(64)  DEFAULT NULL,
    "label"           varchar(255) DEFAULT NULL,
    "condition"       json         DEFAULT NULL,
    "created_at"      timestamp    NOT NULL,
    "updated_at"      timestamp    NULL DEFAULT NULL,
    "created_by"      varchar(50)  DEFAULT NULL,
    "updated_by"      varchar(50)  DEFAULT NULL,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_workflow_edge_wf" ON "llm_workflow_edge" ("workflow_id");

CREATE TABLE "llm_workflow_trigger" (
    "id"              varchar(36)  NOT NULL,
    "workflow_id"     varchar(36)  NOT NULL,
    "node_key"        varchar(64)  DEFAULT NULL,
    "type"            varchar(10)  NOT NULL,
    "enabled"         boolean      NOT NULL DEFAULT true,
    "webhook_token"   varchar(64)  DEFAULT NULL,
    "cron_expression" varchar(120) DEFAULT NULL,
    "overlap_policy"  varchar(10)  DEFAULT 'SKIP',
    "config"          json         DEFAULT NULL,
    "created_at"      timestamp    NOT NULL,
    "updated_at"      timestamp    NULL DEFAULT NULL,
    "created_by"      varchar(50)  DEFAULT NULL,
    "updated_by"      varchar(50)  DEFAULT NULL,
    PRIMARY KEY ("id"),
    UNIQUE ("webhook_token")
);
CREATE INDEX "idx_workflow_trigger_wf" ON "llm_workflow_trigger" ("workflow_id");
CREATE INDEX "idx_workflow_trigger_scan" ON "llm_workflow_trigger" ("type", "enabled");

CREATE TABLE "llm_workflow_execution" (
    "id"               varchar(36) NOT NULL,
    "workflow_id"      varchar(36) NOT NULL,
    "workflow_version" int         NOT NULL,
    "trigger_id"       varchar(36) DEFAULT NULL,
    "trigger_node_key" varchar(64) DEFAULT NULL,
    "trigger_type"     varchar(10) NOT NULL,
    "triggered_by"     varchar(80) NOT NULL,
    "status"           varchar(12) NOT NULL,
    "input_payload"    json        DEFAULT NULL,
    "output_result"    json        DEFAULT NULL,
    "error_node_key"   varchar(64) DEFAULT NULL,
    "error_message"    text        DEFAULT NULL,
    "started_at"       timestamp   NOT NULL,
    "finished_at"      timestamp   NULL DEFAULT NULL,
    "duration_ms"      bigint      DEFAULT NULL,
    "created_at"       timestamp   NOT NULL,
    "updated_at"       timestamp   NULL DEFAULT NULL,
    "created_by"       varchar(50) DEFAULT NULL,
    "updated_by"       varchar(50) DEFAULT NULL,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_workflow_exec_wf" ON "llm_workflow_execution" ("workflow_id", "started_at");
CREATE INDEX "idx_workflow_exec_status" ON "llm_workflow_execution" ("status");

CREATE TABLE "llm_workflow_node_execution" (
    "id"            varchar(36) NOT NULL,
    "execution_id"  varchar(36) NOT NULL,
    "workflow_id"   varchar(36) NOT NULL,
    "node_key"      varchar(64) NOT NULL,
    "node_type"     varchar(30) NOT NULL,
    "seq_no"        int         NOT NULL,
    "status"        varchar(12) NOT NULL,
    "input"         json        DEFAULT NULL,
    "output"        json        DEFAULT NULL,
    "error_message" text        DEFAULT NULL,
    "loop_index"    int         DEFAULT NULL,
    "started_at"    timestamp   NULL DEFAULT NULL,
    "finished_at"   timestamp   NULL DEFAULT NULL,
    "duration_ms"   bigint      DEFAULT NULL,
    "created_at"    timestamp   NOT NULL,
    "updated_at"    timestamp   NULL DEFAULT NULL,
    "created_by"    varchar(50) DEFAULT NULL,
    "updated_by"    varchar(50) DEFAULT NULL,
    PRIMARY KEY ("id")
);
CREATE INDEX "idx_workflow_nodeexec_exec" ON "llm_workflow_node_execution" ("execution_id", "seq_no");
