package tw.zipe.basepartner.resource

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.output.Response
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import java.time.Duration
import org.jboss.resteasy.reactive.RestQuery
import org.jboss.resteasy.reactive.RestStreamElementType
import tw.zipe.basepartner.config.ChatModelConfig
import tw.zipe.basepartner.util.ChatModel

@Path("/llm")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LLMResource(
    var chatModelConfig: ChatModelConfig
) {

    @GET
    @Path("/chat")
    fun chat(@RestQuery message: String?) =
        chatModelConfig.getChatModel()["ollama"]?.generate(message) ?: "No model found"

    @GET
    @Path("/chatStreaming")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    fun chatStreamingTest(@RestQuery message: String): Multi<String?> {

        return Multi.createFrom().emitter<String?> { emitter: MultiEmitter<in String?> ->
            chatModelConfig.getStreamingChatModel()["ollama"]?.generate(
                message,
                object : StreamingResponseHandler<AiMessage> {
                    override fun onNext(token: String) {
                        emitter.emit(token)
                    }

                    override fun onError(error: Throwable) {
                        emitter.fail(error)
                    }

                    override fun onComplete(response: Response<AiMessage?>?) {
                        emitter.complete()
                    }
                }) ?: emitter.fail(RuntimeException("No model found"))
        }
    }

}
