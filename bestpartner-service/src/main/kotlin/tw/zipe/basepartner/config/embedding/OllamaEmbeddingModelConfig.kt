package tw.zipe.basepartner.config.embedding

import dev.langchain4j.model.embedding.EmbeddingModel
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.builder.aigcmodel.OllamaModelBuilder
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.properties.AIPlatformOllamaConfig

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class OllamaEmbeddingModelConfig(var aiPlatformOllamaConfig: AIPlatformOllamaConfig) : EmbeddingModelConfig() {

    override fun buildEmbeddingModel(): EmbeddingModel? {
        val llmConfig = aiPlatformOllamaConfig.defaultConfig().map { convertEmbeddingModelSetting(it, Platform.OLLAMA) }.orElse(null)
        return llmConfig?.let { OllamaModelBuilder().embeddingModel(it) }
    }

}

