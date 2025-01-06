package tw.zipe.bastpartner.service

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import tw.zipe.bastpartner.config.LLMStore.LLMStore.SYSTEM_DEFAULT_PLATFORM

/**
 * @author Gary
 * @created 2025/01/04
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SystemServiceTest {

    @Inject
    lateinit var systemService: SystemService

    @Inject
    lateinit var llmUserService: LLMUserService

    @Test
    fun `test find setting`(){
        val setting = systemService.getSystemSettingValue(SYSTEM_DEFAULT_PLATFORM)
        val user = llmUserService.findUserByName("admin")
        assertEquals("OPENAI", setting)
    }
}
