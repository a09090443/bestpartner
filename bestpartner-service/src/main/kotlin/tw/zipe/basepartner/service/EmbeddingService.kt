package tw.zipe.basepartner.service

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingStore
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import org.jboss.resteasy.reactive.multipart.FileUpload
import tw.zipe.basepartner.form.FilesFromRequest
import tw.zipe.basepartner.util.logger

/**
 * @author Gary
 * @created 2024/10/9
 */
@ApplicationScoped
class EmbeddingService(
    @Named("embeddingModelMap") val embeddingModelMap: Map<String, EmbeddingModel>,
    @Named("vectorStoreMap") val vectorStoreMap: Map<String, EmbeddingStore<TextSegment>>
) {

    private val logger = logger()

    fun embeddingDocs(
        files: List<FileUpload>,
        filesForm: FilesFromRequest
    ): List<String> {

        val documents = files.map { file ->
            val document = FileSystemDocumentLoader.loadDocument(file.uploadedFile(), ApacheTikaDocumentParser())
            document.metadata().put("knowledgeId", filesForm.knowledgeId).put("docsName", file.fileName())
            document
        }

        val splitter = DocumentSplitters.recursive(filesForm.maxSegmentSize, filesForm.maxOverlapSize)
        val segments = splitter.splitAll(documents)
        val embeddingModel = embeddingModelMap[filesForm.embeddingModelName] ?: embeddingModelMap["default"]
        val embeddings: List<Embedding> = embeddingModel?.embedAll(segments)?.content() ?: emptyList()
        val embeddingStore = vectorStoreMap[filesForm.embeddingStoreName] ?: vectorStoreMap["default"]
        val ids = embeddingStore?.addAll(embeddings, segments)

        logger.info("embeddingDocs: ids = $ids")
        return ids ?: emptyList()
    }
}
