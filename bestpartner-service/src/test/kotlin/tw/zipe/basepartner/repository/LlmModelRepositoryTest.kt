package tw.zipe.basepartner.repository

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test

/**
 * @author Gary
 * @created 2024/10/12
 */
@QuarkusTest
class LlmModelRepositoryTest {

    @Inject
    lateinit var llmModelRepository: LlmModelRepository

    @Test
    internal fun `find_all_llm_models`() {
        val llmModels = llmModelRepository.findAll().list()
        assert(llmModels.isEmpty())
    }
}
