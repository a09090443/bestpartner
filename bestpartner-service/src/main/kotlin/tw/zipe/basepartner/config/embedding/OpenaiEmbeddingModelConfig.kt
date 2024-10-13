package tw.zipe.basepartner.config.embedding

import dev.langchain4j.model.embedding.EmbeddingModel
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.builder.aigcmodel.OpenaiModelBuilder
import tw.zipe.basepartner.properties.AIPlatformOpenaiConfig

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class OpenaiEmbeddingModelConfig(var aiPlatformOpenaiConfig: AIPlatformOpenaiConfig) : EmbeddingModelConfig() {

    override fun buildEmbeddingModel(): EmbeddingModel? {
        val llmConfig = aiPlatformOpenaiConfig.defaultConfig().map { convertEmbeddingModelSetting(it) }.orElse(null)
        return llmConfig?.let { OpenaiModelBuilder().embeddingModel(it) }
    }

}

