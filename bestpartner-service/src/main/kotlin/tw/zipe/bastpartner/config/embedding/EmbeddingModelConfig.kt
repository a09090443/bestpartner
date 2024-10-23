package tw.zipe.bastpartner.config.embedding

import dev.langchain4j.model.embedding.EmbeddingModel
import io.netty.util.internal.StringUtil
import tw.zipe.bastpartner.model.LLModel
import tw.zipe.bastpartner.properties.BaseAIPlatform
import tw.zipe.bastpartner.provider.ModelProvider

/**
 * @author Gary
 * @created 2024/10/9
 */
abstract class EmbeddingModelConfig {

    fun convertEmbeddingModelSetting(baseAIPlatform: BaseAIPlatform): LLModel = run {
        LLModel(
            apiKey = baseAIPlatform.apiKey().orElse(null),
            url = baseAIPlatform.url().orElse(StringUtil.EMPTY_STRING),
            modelName = baseAIPlatform.embeddingModelName().orElse(StringUtil.EMPTY_STRING),
            temperature = baseAIPlatform.temperature(),
            timeout = baseAIPlatform.timeout().toMillis()
        )
    }

    fun buildEmbeddingModel(llModel: LLModel, modelProvider: ModelProvider): EmbeddingModel {
        return modelProvider.embeddingModel(llModel)
    }

    /**
     * 建立EmbeddingModel
     */
    abstract fun buildEmbeddingModel(): EmbeddingModel?

}
