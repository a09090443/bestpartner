package tw.zipe.bastpartner.config.embedding

import dev.langchain4j.model.embedding.EmbeddingModel
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.builder.llm.OpenrouterModelBuilder
import tw.zipe.bastpartner.properties.AIPlatformOpenrouterConfig

/**
 * @author Gary
 * @created 2026/04/04
 */
@ApplicationScoped
class OpenrouterEmbeddingModelConfig(var aiPlatformOpenrouterConfig: AIPlatformOpenrouterConfig) : EmbeddingModelConfig() {

    override fun buildEmbeddingModel(): EmbeddingModel? {
        val llmConfig = aiPlatformOpenrouterConfig.defaultConfig().map { convertEmbeddingModelSetting(it) }.orElse(null)
        return llmConfig?.let { OpenrouterModelBuilder().embeddingModel(it) }
    }
}
