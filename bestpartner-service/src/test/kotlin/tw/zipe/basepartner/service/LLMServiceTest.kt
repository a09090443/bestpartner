package tw.zipe.basepartner.service

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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

//    @Test
//    @Order(1)
    fun `test add llm setting`() {
        val llmSetting = llmService.saveLLMSetting(llmDTO)
        llmDTO.id = llmSetting.id
        assertNotNull(llmSetting.id)
    }

    @Test
    @Order(2)
    fun `test find llm setting`() {
        val llmDTOResult = llmService.getLLMSetting("45d5c19f-819a-4a3a-83ad-d76812f97db0")
        assertEquals(llmDTO.id, llmDTOResult?.id)
    }

//    @Test
//    @Order(3)
    fun `test build llm`() {
        val llm = llmService.buildLLM(llmDTO.id!!)
        assertNotNull(llm)
    }

//    @Test
//    @Order(4)
    fun `test update llm setting`() {
        llmDTO.modelType = ModelType.EMBEDDING
        llmDTO.llmModel?.modelName = "llama3.2:latest"
        llmService.updateLLMSetting(llmDTO)
        val llmDTOResult = llmService.getLLMSetting(llmDTO.id!!)
        assertEquals(llmDTO.modelType, llmDTOResult?.modelType)
    }

//    @Test
//    @Order(5)
    fun `test delete llm setting`() {
        llmService.deleteLLMSetting(llmDTO.id!!)
        val llmDTOResult = llmService.getLLMSetting(llmDTO.id!!)
        assertNull(llmDTOResult)
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
