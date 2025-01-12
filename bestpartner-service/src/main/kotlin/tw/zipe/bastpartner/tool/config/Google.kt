package tw.zipe.bastpartner.tool.config

data class Google(
    val apiKey: String,
    val csi: String,
    val siteRestrict: Boolean?,
    val includeImages: Boolean?,
    val timeout: Long,
    val maxRetries: Int?,
    val logRequests: Boolean?,
    val logResponses: Boolean?
)
