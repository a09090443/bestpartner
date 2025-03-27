package tw.zipe.bastpartner.enumerate

import dev.langchain4j.data.message.AudioContent
import dev.langchain4j.data.message.Content
import dev.langchain4j.data.message.ImageContent
import dev.langchain4j.data.message.PdfFileContent
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.VideoContent
import kotlin.reflect.KClass

/**
 * @author Gary
 * @created 2025/3/24
 */
enum class FileType(
    val description: String,
    val clazz: KClass<out Content>
) {
    IMAGE("圖像檔案", ImageContent::class),
    VIDEO("影片檔案", VideoContent::class),
    AUDIO("音訊檔案", AudioContent::class),
    PDF("PDF檔案", PdfFileContent::class),
    DOCUMENT("文件檔案", PdfFileContent::class),
    OTHER("其他檔案", TextContent::class);

    companion object {
        /**
         * 從 MIME 類型取得檔案類型
         */
        fun getFileTypeFromMimeType(mimeType: String?): FileType {
            if (mimeType == null) return OTHER

            return when {
                mimeType.startsWith("image/") -> IMAGE
                mimeType.startsWith("video/") -> VIDEO
                mimeType.startsWith("audio/") -> AUDIO
                mimeType.startsWith("application/pdf") -> PDF
                mimeType.startsWith("text/") -> DOCUMENT
                else -> OTHER
            }
        }
    }
}
