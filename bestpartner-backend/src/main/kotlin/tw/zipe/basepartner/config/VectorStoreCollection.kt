package tw.zipe.basepartner.config

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Named
import tw.zipe.basepartner.config.vector.ChromaConfig
import tw.zipe.basepartner.config.vector.MilvusConfig
import tw.zipe.basepartner.enumerate.VectorStore

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class VectorStoreCollection(
    private val milvusConfig: MilvusConfig,
    private val chromaConfig: ChromaConfig
) {

    @Produces
    @Named("vectorStoreMap")
    fun getVectorStoreMap(): Map<String, EmbeddingStore<TextSegment>> {

        val vectorStoreMap = mutableMapOf<String, EmbeddingStore<TextSegment>>()

        vectorStoreMap["default"] = InMemoryEmbeddingStore()

        milvusConfig.buildVectorStore()?.let { vectorStoreMap[VectorStore.MILVUS.name] = it }

        chromaConfig.buildVectorStore()?.let { vectorStoreMap[VectorStore.CHROMA.name] = it }

        return vectorStoreMap
    }
}
