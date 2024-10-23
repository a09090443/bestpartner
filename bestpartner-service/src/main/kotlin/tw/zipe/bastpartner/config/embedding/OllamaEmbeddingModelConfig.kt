package tw.zipe.bastpartner.config.embedding

import dev.langchain4j.model.embedding.EmbeddingModel
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.builder.llm.OllamaModelBuilder
import tw.zipe.bastpartner.properties.AIPlatformOllamaConfig

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class OllamaEmbeddingModelConfig(var aiPlatformOllamaConfig: AIPlatformOllamaConfig) : EmbeddingModelConfig() {

    override fun buildEmbeddingModel(): EmbeddingModel? {
        val llmConfig = aiPlatformOllamaConfig.defaultConfig().map { convertEmbeddingModelSetting(it) }.orElse(null)
        return llmConfig?.let { OllamaModelBuilder().embeddingModel(it) }
    }

}

