package tw.zipe.basepartner.builder.vector

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore
import tw.zipe.basepartner.model.VectorStoreModel
import tw.zipe.basepartner.provider.VectorStoreProvider

/**
 * @author Gary
 * @created 2024/10/9
 */
class ChromaBuilder : VectorStoreProvider {

    override fun embeddingStore(vector: VectorStoreModel): EmbeddingStore<TextSegment> =
        ChromaEmbeddingStore
            .builder()
            .baseUrl(vector.url)
            .collectionName(vector.collectionName)
            .logRequests(true)
            .logResponses(true)
            .build()

}
