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
                                        "setting_content" json DEFAULT NULL,
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
