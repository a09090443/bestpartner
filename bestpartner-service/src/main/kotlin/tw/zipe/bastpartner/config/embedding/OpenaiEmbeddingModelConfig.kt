package tw.zipe.bastpartner.config.embedding

import dev.langchain4j.model.embedding.EmbeddingModel
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.builder.llm.OpenaiModelBuilder
import tw.zipe.bastpartner.properties.AIPlatformOpenaiConfig

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

