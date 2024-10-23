package tw.zipe.bastpartner.builder.vector

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore
import tw.zipe.bastpartner.model.VectorStoreModel
import tw.zipe.bastpartner.provider.VectorStoreProvider

/**
 * @author Gary
 * @created 2024/10/9
 */
class MilvusBuilder : VectorStoreProvider {

    override fun embeddingStore(vector: VectorStoreModel?): EmbeddingStore<TextSegment> =
        MilvusEmbeddingStore
            .builder()
            .uri(vector?.url)
            .collectionName(vector?.collectionName)
            .dimension(vector?.dimension)
            .build()

}
