package tw.zipe.basepartner.assistant

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.output.Response

/**
 * @author Gary
 * @created 2024/10/7
 */
interface BaseAssistant {

    fun chat(userMessage: String): Response<AiMessage>

}
