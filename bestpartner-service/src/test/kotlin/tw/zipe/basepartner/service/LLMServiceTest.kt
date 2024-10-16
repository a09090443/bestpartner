package tw.zipe.basepartner.service

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import tw.zipe.basepartner.dto.LLMDTO
import tw.zipe.basepartner.enumerate.ModelType
import tw.zipe.basepartner.enumerate.Platform
import tw.zipe.basepartner.model.LLModel

/**
 * @author Gary
 * @created 2024/10/16
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class LLMServiceTest {

    @Inject
    lateinit var llmService: LLMService

    @Test
    @Order(1)
    fun `test add llm setting`() {
        llmService.saveLLM(llmDTO)
    }

    @Test
    @Order(2)
    fun `test find llm setting`() {
        val llmSetting = llmService.getLLMSetting("example")
        assertEquals(llmDTO.alias, llmSetting?.alias)
    }

    companion object {
        lateinit var llmDTO: LLMDTO

        @BeforeAll
        @JvmStatic
        fun init() {
            llmDTO = LLMDTO()
            llmDTO.alias = "example"
            llmDTO.modelType = ModelType.CHAT
            llmDTO.platform = Platform.OLLAMA
            llmDTO.llmModel = LLModel(
                modelName = "llama3.1:latest",
                url = "http://localhost:11434",
                temperature = 0.7,
                topP = 0.5,
                topK = 40,
                maxTokens = 4096,
                timeout = 60000,
                logRequests = true,
                logResponses = true
            )
        }
    }
}
