package tw.zipe.basepartner.config.vector

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import tw.zipe.basepartner.model.VectorStoreModel
import tw.zipe.basepartner.properties.BaseVector

/**
 * @author Gary
 * @created 2024/10/9
 */
abstract class VectorStoreConfig {

    fun convertVectorStoreSetting(baseVector: BaseVector): VectorStoreModel = run {
        VectorStoreModel(
            url = baseVector.url(),
            collectionName = baseVector.collectionName().orElse(null),
            dimension = baseVector.dimension().orElse(384),
        )
    }

    /**
     * 建立VectorStore
     */
    abstract fun buildVectorStore(): EmbeddingStore<TextSegment>?

}
