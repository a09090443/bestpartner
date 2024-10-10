package tw.zipe.basepartner.config.embedding

import dev.langchain4j.model.embedding.EmbeddingModel
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.model.LLMEmbeddingModel
import tw.zipe.basepartner.properties.BaseAIPlatform
import tw.zipe.basepartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/9
 */
abstract class EmbeddingModelConfig {

    fun convertEmbeddingModelSetting(baseAIPlatform: BaseAIPlatform): LLMEmbeddingModel = run {
        LLMEmbeddingModel(
            platform = Platform.OLLAMA,
            url = baseAIPlatform.url(),
            modelName = baseAIPlatform.modelName(),
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
