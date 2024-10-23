package tw.zipe.bastpartner.enumerate

import tw.zipe.bastpartner.builder.vector.ChromaBuilder
import tw.zipe.bastpartner.builder.vector.MilvusBuilder
import tw.zipe.bastpartner.provider.VectorStoreProvider

/**
 * @author Gary
 * @created 2024/10/8
 */
enum class VectorStore (val builder: VectorStoreProvider){
    CHROMA(ChromaBuilder()),
    MILVUS(MilvusBuilder());

    fun getVactorStore() = builder

}
