package tw.zipe.basepartner.config

import dev.langchain4j.model.embedding.EmbeddingModel
import tw.zipe.basepartner.builder.chatmodel.OllamaBuilder
import tw.zipe.basepartner.builder.chatmodel.OpenaiBuilder
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.model.LLMEmbeddingModel
import tw.zipe.basepartner.properties.AIPlatformOllamaConfig
import tw.zipe.basepartner.properties.AIPlatformOpenaiConfig
import tw.zipe.basepartner.properties.OllamaProp
import tw.zipe.basepartner.properties.OpenaiProp
import tw.zipe.basepartner.util.logger

/**
 * @author Gary
 * @created 2024/10/08
 */
//@ApplicationScoped
class EmbeddingModelConfig(
    var aiPlatformOllamaConfig: AIPlatformOllamaConfig,
    var aiPlatformOpenaiConfig: AIPlatformOpenaiConfig
) {

    private val logger = logger()
    private val embeddingModelMap = mutableMapOf<String, EmbeddingModel>()

    fun getEmbeddingModel(): Map<String, EmbeddingModel> {
        logger.info("根據設定檔建立llm連線")
        embeddingModelMap.ifEmpty {
            aiPlatformOllamaConfig.defaultConfig()
                .map { it.embeddingModelName().let {
                    embeddingModelMap[Platform.OLLAMA.name] =
                        ollamaEmbeddingModel(aiPlatformOllamaConfig.defaultConfig().get())
                }
                }
            aiPlatformOpenaiConfig.defaultConfig()
                .map { it.embeddingModelName().let {
                    embeddingModelMap[Platform.OPENAI.name] =
                        openaiEmbeddingModel(aiPlatformOpenaiConfig.defaultConfig().get())
                }
                }

            aiPlatformOllamaConfig.namedConfig().forEach { (key, value) ->
                embeddingModelMap[key] = ollamaEmbeddingModel(value)
            }

            aiPlatformOpenaiConfig.namedConfig().forEach { (key, value) ->
                embeddingModelMap[key] = openaiEmbeddingModel(value)
            }
        }

        return embeddingModelMap
    }

    fun ollamaEmbeddingModel(ollama: OllamaProp): EmbeddingModel {
        return run {
            LLMEmbeddingModel(
                platform = Platform.OLLAMA,
                url = ollama.url(),
                modelName = ollama.modelName(),
                temperature = ollama.temperature(),
                timeout = ollama.timeout().toMillis()
            )
        }.run {
            OllamaBuilder().embeddingModel(this)
        }
    }

    fun openaiEmbeddingModel(openai: OpenaiProp): EmbeddingModel {
        return run {
            LLMEmbeddingModel(
                platform = Platform.OPENAI,
                url = openai.url(),
                apiKey = openai.apiKey().orElse(null),
                modelName = openai.modelName(),
                temperature = openai.temperature(),
                timeout = openai.timeout().toMillis()
            )
        }.run {
            OpenaiBuilder().embeddingModel(this)
        }
    }

}
