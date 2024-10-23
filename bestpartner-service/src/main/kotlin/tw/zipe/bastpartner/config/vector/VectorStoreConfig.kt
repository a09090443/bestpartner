package tw.zipe.bastpartner.config.vector

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import tw.zipe.bastpartner.model.VectorStoreModel
import tw.zipe.bastpartner.properties.BaseVector

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
