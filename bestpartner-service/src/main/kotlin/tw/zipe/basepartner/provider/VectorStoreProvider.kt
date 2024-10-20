package tw.zipe.basepartner.provider

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import tw.zipe.basepartner.model.VectorStoreModel

/**
 * @author Gary
 * @created 2024/10/9
 */
interface VectorStoreProvider {

    fun embeddingStore(vector: VectorStoreModel?): EmbeddingStore<TextSegment>

}
