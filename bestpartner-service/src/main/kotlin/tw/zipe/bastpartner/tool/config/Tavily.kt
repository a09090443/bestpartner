package tw.zipe.bastpartner.tool.config

data class Tavily(
    @ToolConfigField(description = "Tavily API 基礎 URL，未填時使用官方預設")
    val baseUrl: String?,
    @ToolConfigField(description = "Tavily API 金鑰", sensitive = true)
    val apiKey: String,
    @ToolConfigField(description = "逾時毫秒數")
    val timeout: Long,
    @ToolConfigField(description = "搜尋深度（basic / advanced）")
    val searchDepth: String?,
    val includeAnswer: Boolean?,
    val includeRawContent: Boolean?,
    @ToolConfigField(description = "限定搜尋的網域清單")
    val includeDomains: List<String>?,
    @ToolConfigField(description = "排除搜尋的網域清單")
    val excludeDomains: List<String>?
)
