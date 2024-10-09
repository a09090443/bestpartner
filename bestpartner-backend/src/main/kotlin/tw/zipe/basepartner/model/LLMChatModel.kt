package tw.zipe.basepartner.model

import kotlinx.serialization.Serializable
import tw.zipe.basepartner.enumerate.Platform

/**
 * @author Gary
 * @created 2024/10/8
 */
@Serializable
data class LLMChatModel(
    val platform: Platform? = null,
    val url: String,
    val apiKey: String? = null,
    val modelName: String,
    val temperature: Double? = null,
    val topP: Double? = null,
    val dimensions: Int? = null,
    val maxTokens: Int? = null,
    val timeout: Long = 3000,
    val logRequests: Boolean = false,
    val logResponses: Boolean = false
)
