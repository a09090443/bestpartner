package tw.zipe.basepartner.config

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore
import jakarta.enterprise.context.ApplicationScoped
import kotlin.jvm.optionals.getOrNull
import tw.zipe.basepartner.enumerate.VectorStore
import tw.zipe.basepartner.properties.ChromaProp
import tw.zipe.basepartner.properties.MilvusProp
import tw.zipe.basepartner.properties.VectorChromaConfig
import tw.zipe.basepartner.properties.VectorMilvusConfig
import tw.zipe.basepartner.util.logger

/**
 * @author Gary
 * @created 2024/10/8
 */
@ApplicationScoped
class VectorStoreConfig(
    var vectorChromaConfig: VectorChromaConfig,
    var vectorMilvusConfig: VectorMilvusConfig
) {

    private val logger = logger()
    private val vectorStoreMap = mutableMapOf<String, EmbeddingStore<TextSegment>>()

    fun getVectorStore(): Map<String, EmbeddingStore<TextSegment>> {
        logger.info("根據設定檔建立Vector store資料庫: $vectorChromaConfig")
        vectorStoreMap.ifEmpty {
            vectorChromaConfig.defaultConfig().map { vectorStoreMap[VectorStore.CHROMA.name] = chromaStore(it) }
            vectorMilvusConfig.defaultConfig().map { vectorStoreMap[VectorStore.MILVUS.name] = milvusStore(it) }
        }

        return vectorStoreMap
    }

    private fun chromaStore(chroma: ChromaProp): EmbeddingStore<TextSegment> {
        return ChromaEmbeddingStore
            .builder()
            .baseUrl(chroma.url())
            .collectionName(chroma.collectionName().getOrNull())
            .logRequests(true)
            .logResponses(true)
            .build()
    }

    private fun milvusStore(milvus: MilvusProp): EmbeddingStore<TextSegment> {
        return MilvusEmbeddingStore
            .builder()
            .uri(milvus.url())
            .collectionName(milvus.collectionName().getOrNull())
            .dimension(milvus.dimension().orElse(1024))
            .build()
    }
}
