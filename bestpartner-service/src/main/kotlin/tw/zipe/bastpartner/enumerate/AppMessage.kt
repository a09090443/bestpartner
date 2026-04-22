package tw.zipe.bastpartner.enumerate

enum class AppMessage(val key: String) {
    // AUTH
    AUTH_NOT_LOGGED_IN("auth.not.logged.in"),
    AUTH_ACCOUNT_DISABLED("auth.account.disabled"),
    AUTH_TOKEN_EXPIRED_REFRESH("auth.token.expired.refresh"),
    AUTH_TOKEN_EXPIRED_RELOGIN("auth.token.expired.relogin"),
    AUTH_TOKEN_REFRESH_ERROR("auth.token.refresh.error"),

    // LLM
    LLM_PLATFORM_NOT_FOUND("llm.platform.not.found"),
    LLM_SETTING_NOT_FOUND("llm.setting.not.found"),
    LLM_SETTING_TYPE_MISMATCH("llm.setting.type.mismatch"),
    LLM_TYPE_INVALID("llm.type.invalid"),
    LLM_MODEL_NOT_FOUND("llm.model.not.found"),
    LLM_USER_NOT_FOUND("llm.user.not.found"),

    // FILE
    FILE_TYPE_UNSUPPORTED("file.type.unsupported"),
    FILE_UPLOAD_FAILED("file.upload.failed"),

    // EMBEDDING
    EMBEDDING_VECTOR_STORE_NOT_FOUND("embedding.vector.store.not.found"),
    EMBEDDING_DOC_ALREADY_EXISTS("embedding.doc.already.exists"),
    EMBEDDING_SAVE_KNOWLEDGE_FAILED("embedding.save.knowledge.failed"),

    // MCP
    MCP_SERVER_NOT_FOUND("mcp.server.not.found"),
    MCP_SERVER_SETTING_NOT_FOUND("mcp.server.setting.not.found"),
    MCP_USER_SETTING_NOT_FOUND("mcp.user.setting.not.found"),
    MCP_ARGS_SETTING_INVALID("mcp.args.setting.invalid"),
    MCP_ENV_SETTING_INVALID("mcp.env.setting.invalid"),
    MCP_MISSING_ARGS("mcp.missing.args"),
    MCP_TYPE_INVALID("mcp.type.invalid"),

    // SKILL
    SKILL_NOT_FOUND("skill.not.found"),
    SKILL_NAME_DUPLICATE("skill.name.duplicate"),
    SKILL_RESOURCE_NOT_FOUND("skill.resource.not.found"),
    SKILL_ZIP_NOT_PROVIDED("skill.zip.not.provided"),
    SKILL_ZIP_FORMAT_INVALID("skill.zip.format.invalid"),
    SKILL_ZIP_PATH_TRAVERSAL("skill.zip.path.traversal"),
    SKILL_MD_NOT_FOUND("skill.md.not.found"),

    // TOOL
    TOOL_NOT_FOUND("tool.not.found"),
    TOOL_SETTING_NOT_FOUND("tool.setting.not.found"),
    TOOL_CATEGORY_HAS_TOOLS("tool.category.has.tools"),
    TOOL_PARAM_TYPE_INVALID("tool.param.type.invalid"),
    TOOL_FUNCTION_PARAM_FORMAT_INVALID_VALUE("tool.function.param.format.invalid.value"),
    TOOL_FUNCTION_PARAM_FORMAT_INVALID("tool.function.param.format.invalid"),

    // SYSTEM
    SYSTEM_PLATFORM_NOT_FOUND("system.platform.not.found"),
    SYSTEM_DB_QUERY_FAILED("system.db.query.failed"),
    SYSTEM_ENUM_CONSTANT_NOT_FOUND("system.enum.constant.not.found"),
    SYSTEM_ENUM_INDEX_NOT_FOUND("system.enum.index.not.found"),
    SYSTEM_ENUM_TYPE_UNSUPPORTED("system.enum.type.unsupported"),
    SYSTEM_USER_STATUS_INVALID("system.user.status.invalid"),
    SYSTEM_DATE_FORMAT_UNKNOWN("system.date.format.unknown"),
    SYSTEM_TIME_UNIT_UNSUPPORTED("system.time.unit.unsupported"),
    SYSTEM_DATABASE_TYPE_NOT_FOUND("system.database.type.not.found"),

    // HTTP
    HTTP_SERVICE_EXCEPTION("http.service.exception"),
    HTTP_LLM_SERVICE_ERROR("http.llm.service.error"),
    HTTP_NOT_FOUND("http.not.found"),
    HTTP_INVALID_REQUEST("http.invalid.request"),
    HTTP_VALIDATION_FAILED("http.validation.failed"),
    HTTP_JWT_VALIDATION_FAILED("http.jwt.validation.failed"),
    HTTP_FORBIDDEN("http.forbidden"),
    HTTP_UNAUTHORIZED("http.unauthorized"),
    HTTP_AUTHENTICATION_FAILED("http.authentication.failed"),
    HTTP_AUTHENTICATION_REQUIRED("http.authentication.required"),
    HTTP_SECURITY_VALIDATION_FAILED("http.security.validation.failed"),
    HTTP_ACCESS_DENIED("http.access.denied"),
    HTTP_DUPLICATE_DATA("http.duplicate.data"),
    HTTP_DATABASE_ERROR("http.database.error"),
    HTTP_METHOD_NOT_ALLOWED("http.method.not.allowed"),
    HTTP_WEB_APP_EXCEPTION("http.web.app.exception"),
    HTTP_INTERNAL_SERVER_ERROR("http.internal.server.error"),

    // SUCCESS
    SUCCESS_VECTOR_STORE_SAVED("success.vector.store.saved"),
    SUCCESS_LLM_SETTING_UPDATED("success.llm.setting.updated"),
    SUCCESS_DATA_DELETED("success.data.deleted");
}
