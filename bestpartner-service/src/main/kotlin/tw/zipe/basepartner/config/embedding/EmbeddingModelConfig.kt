package tw.zipe.basepartner.config.embedding

import dev.langchain4j.model.embedding.EmbeddingModel
import io.netty.util.internal.StringUtil
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.model.LLMEmbeddingModel
import tw.zipe.basepartner.properties.BaseAIPlatform
import tw.zipe.basepartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/9
 */
abstract class EmbeddingModelConfig {

    fun convertEmbeddingModelSetting(baseAIPlatform: BaseAIPlatform, platform: Platform): LLMEmbeddingModel = run {
        LLMEmbeddingModel(
            platform = platform,
            apiKey = baseAIPlatform.apiKey().orElse(null),
            url = baseAIPlatform.url().orElse(StringUtil.EMPTY_STRING),
            modelName = baseAIPlatform.embeddingModelName().orElse(StringUtil.EMPTY_STRING),
            temperature = baseAIPlatform.temperature(),
            timeout = baseAIPlatform.timeout().toMillis()
        )
    }

    fun buildEmbeddingModel(llmEmbeddingModel: LLMEmbeddingModel, modelProvider: ModelProvider): EmbeddingModel {
        return modelProvider.embeddingModel(llmEmbeddingModel)
    }

    /**
     * 建立EmbeddingModel
     */
    abstract fun buildEmbeddingModel(): EmbeddingModel?

}
