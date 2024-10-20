package tw.zipe.basepartner.enumerate

import tw.zipe.basepartner.builder.vector.ChromaBuilder
import tw.zipe.basepartner.builder.vector.MilvusBuilder
import tw.zipe.basepartner.provider.VectorStoreProvider

/**
 * @author Gary
 * @created 2024/10/8
 */
enum class VectorStore (val builder: VectorStoreProvider){
    CHROMA(ChromaBuilder()),
    MILVUS(MilvusBuilder());

    fun getVactorStore() = builder

}
