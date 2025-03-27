package tw.zipe.bastpartner.repository

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

/**
 * @author Gary
 * @created 2025/3/27
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class LLMMcpUserSettingRepositoryTest {


    @Inject
    lateinit var llmMcpUserSettingRepository: LLMMcpUserSettingRepository

    @Test
    fun testFindSettingByUserIdAndMcpId() {
        val userId = "670017b4-23d0-4339-a9c0-22b6d9446461"
        val settingId = "54aec0e7-12c9-43f5-9c8c-69b8962313f5"
        val result = llmMcpUserSettingRepository.findByCondition(settingId, userId)
        assert(result != null)
    }

}
