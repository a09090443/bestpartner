package tw.zipe.basepartner.config.vector

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.builder.vector.MilvusBuilder
import tw.zipe.basepartner.properties.VectorMilvusConfig

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class MilvusConfig(var vectorMilvusConfig: VectorMilvusConfig) : VectorStoreConfig() {

    override fun buildVectorStore(): EmbeddingStore<TextSegment>? {
        val vectorStoreConfig = vectorMilvusConfig.defaultConfig().map { convertVectorStoreSetting(it) }.orElse(null)
        return vectorStoreConfig?.let { MilvusBuilder().embeddingStore(it) }
    }

}
