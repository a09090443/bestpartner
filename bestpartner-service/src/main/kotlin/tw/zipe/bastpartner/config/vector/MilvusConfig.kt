package tw.zipe.bastpartner.config.vector

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.builder.vector.MilvusBuilder
import tw.zipe.bastpartner.properties.VectorMilvusConfig

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
