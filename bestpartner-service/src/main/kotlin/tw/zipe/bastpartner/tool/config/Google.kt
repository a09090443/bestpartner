package tw.zipe.bastpartner.tool.config

data class Google(
    @ToolConfigField(description = "Google Custom Search API 金鑰", sensitive = true)
    val apiKey: String,
    @ToolConfigField(description = "Custom Search Engine ID")
    val csi: String,
    val siteRestrict: Boolean?,
    val includeImages: Boolean?,
    @ToolConfigField(description = "逾時毫秒數")
    val timeout: Long,
    val maxRetries: Int?,
    val logRequests: Boolean?,
    val logResponses: Boolean?
)
