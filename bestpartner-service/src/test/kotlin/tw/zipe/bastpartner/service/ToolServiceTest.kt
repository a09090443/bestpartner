package tw.zipe.bastpartner.service

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import tw.zipe.bastpartner.dto.ToolDTO

/**
 * @author Gary
 * @created 2024/10/14
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ToolServiceTest {

    @Inject
    lateinit var toolService: ToolService

    @Test
    @Order(1)
    fun `test register tool`() {
        toolService.registerTool(toolDTO)
    }

    @Test
    @Order(2)
    fun `test find tool by name`() {
        val result = toolService.findToolByName(toolDTO.name)
        assertEquals(toolDTO.name, result?.name)
    }

    @Test
    @Order(3)
    fun `test remove tool`() {
        val result = toolService.removeTool(toolDTO.id ?: "")
        assert(result)
    }

    companion object {
        lateinit var toolDTO: ToolDTO

        @BeforeAll
        @JvmStatic
        fun init() {
            toolDTO = ToolDTO(
                name = "DateTool",
                classPath = "tw.zipe.basepartner.tool.DateTool",
                settingContent = "settingContent"
            )
        }
    }
}
