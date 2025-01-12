package tw.zipe.bastpartner.tool.config

data class Tavily(
    val baseUrl: String?,
    val apiKey: String,
    val timeout: Long,
    val searchDepth: String?,
    val includeAnswer: Boolean?,
    val includeRawContent: Boolean?,
    val includeDomains: List<String>?,
    val excludeDomains: List<String>?
)
