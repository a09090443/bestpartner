package tw.zipe.bastpartner.tool.text2sql.config

data class Text2SQL(
    val platform: String,
    val llmApiKey: String?,
    val llmUrl: String?,
    val llmModelName: String,
    val datasourceUrl: String,
    val datasourceUsername: String,
    val datasourcePassword: String,
    val datasourceDatabaseType: String,
)
