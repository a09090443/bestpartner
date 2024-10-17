package tw.zipe.basepartner.model

import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/8
 */
@Serializable
data class LLModel(
    var apiKey: String? = null,
    var url: String? = null,
    var modelName: String,
    var temperature: Double? = null,
    var topP: Double? = null,
    var topK: Int? = null,
    var dimensions: Int? = null,
    var maxTokens: Int? = null,
    var timeout: Long = 3000,
    var logRequests: Boolean = false,
    var logResponses: Boolean = false
):BaseLLM()
