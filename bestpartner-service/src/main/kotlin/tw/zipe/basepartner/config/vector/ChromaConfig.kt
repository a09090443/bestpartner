package tw.zipe.basepartner.config.vector

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.builder.vector.ChromaBuilder
import tw.zipe.basepartner.properties.VectorChromaConfig

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class ChromaConfig(var vectorChromaConfig: VectorChromaConfig) : VectorStoreConfig() {

    override fun buildVectorStore(): EmbeddingStore<TextSegment>? {
        val vectorStoreConfig = vectorChromaConfig.defaultConfig().map { convertVectorStoreSetting(it) }.orElse(null)
        return vectorStoreConfig?.let { ChromaBuilder().embeddingStore(it) }
    }

}
