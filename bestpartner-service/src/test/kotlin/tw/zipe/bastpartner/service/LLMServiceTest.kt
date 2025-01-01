package tw.zipe.bastpartner.service

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import tw.zipe.bastpartner.dto.LLMDTO
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.enumerate.Platform
import tw.zipe.bastpartner.model.LLModel

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
    @TestSecurity(user = "testUser", roles = ["admin", "user"])
    fun `test add llm setting`() {
        val llmSetting = llmService.saveLLMSetting(llmDTO)
        assertNotNull(llmSetting)
    }

    @Test
    @Order(2)
    @TestSecurity(user = "testUser", roles = ["admin", "user"])
    fun `test find llm setting`() {
        val llmDTOResult = llmService.getLLMSetting(llmDTO.id!!)
        assertEquals(llmDTO.id, llmDTOResult?.id)
    }

    @Test
    @Order(3)
    @TestSecurity(user = "testUser", roles = ["admin", "user"])
    fun `test build llm`() {
        val llm = llmDTO.modelType?.let { llmService.buildLLM(llmDTO.id!!, it) }
        assertNotNull(llm)
    }

    @Test
    @Order(4)
    @TestSecurity(user = "testUser", roles = ["admin", "user"])
    fun `test update llm setting`() {
        llmDTO.modelType = ModelType.EMBEDDING
        llmDTO.llmModel.modelName = "llama3.2:latest"
        llmService.updateLLMSetting(llmDTO)
        val llmDTOResult = llmService.getLLMSetting(llmDTO.id!!)
        assertEquals(llmDTO.modelType, llmDTOResult?.modelType)
    }

    @Test
    @Order(5)
    @TestSecurity(user = "testUser", roles = ["admin", "user"])
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
            llmDTO.platformId = "166a5745-b89c-410e-ada4-7ae3da426af5"
            llmDTO.llmModel = with(LLModel()) {
                modelName = "llama3.1:latest"
                url = "http://localhost:11434"
                temperature = 0.7
                topP = 0.5
                topK = 40
                maxTokens = 4096
                timeout = 60000
                logRequests = true
                logResponses = true
                this
            }
        }
    }
}
