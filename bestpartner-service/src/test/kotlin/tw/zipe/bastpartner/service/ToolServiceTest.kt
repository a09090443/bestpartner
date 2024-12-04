package tw.zipe.bastpartner.service

import dev.langchain4j.web.search.WebSearchEngine
import dev.langchain4j.web.search.WebSearchTool
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlin.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import tw.zipe.bastpartner.dto.ToolDTO
import tw.zipe.bastpartner.enumerate.ToolsCategory
import tw.zipe.bastpartner.enumerate.ToolsType
import tw.zipe.bastpartner.model.tool.GoogleSearchModel
import tw.zipe.bastpartner.util.instantiate
import tw.zipe.bastpartner.util.reorderAndRenameArguments

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
        assertNotNull(toolDTO.id)
    }

    @Test
    @Order(2)
    fun `test find tool by id`() {
        val result = toolService.findToolById(toolDTO.id!!)
        assertEquals(toolDTO.name, result?.name)
    }

    @Test
    @Order(3)
    fun `test remove tool`() {
        val result = toolService.removeTool(toolDTO.id.orEmpty())
        assert(result)
    }

    companion object {
        lateinit var toolDTO: ToolDTO

        @BeforeAll
        @JvmStatic
        fun init() {
            dateTool()
//            googleSearchTool()
        }

        private fun dateTool() {
            toolDTO = ToolDTO(
                name = "DateTool",
                classPath = "tw.zipe.bastpartner.tool.DateTool",
                group = ToolsCategory.DATE,
                type = ToolsType.BUILT_IN,
                description = "內建日期工具",
            )
//            val tools = instantiate(toolDTO.classPath)
//            println(tools)
        }

        private fun googleSearchTool() {
            toolDTO = ToolDTO(
                name = "GoogleSearch",
                classPath = "dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine",
                group = ToolsCategory.WEB_SEARCH,
                type = ToolsType.BUILT_IN,
                description = "內建 Google 搜尋工具",
                settingFields = listOf(
                    "apiKey",
                    "csi",
                    "siteRestrict",
                    "includeImages",
                    "timeout",
                    "maxRetries",
                    "logRequests",
                    "logResponses"
                )
            )

//            val argMap = mutableMapOf<String, Any>(
//                "maxRetries" to 3,
//                "logRequests" to true,
//                "apiKey" to "123456",
//                "csi" to "123456",
//                "siteRestrict" to false,
//                "timeout" to java.time.Duration.ofSeconds(60),
//                "includeImages" to true,
//                "logResponses" to true
//            )
//
//            val tools = instantiate(
//                "dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine",
//                reorderAndRenameArguments(
//                    argMap,
//                    "apiKey, csi, siteRestrict, includeImages, timeout, maxRetries, logRequests, logResponses"
//                )
//            )
//            val webSearchTool = WebSearchTool.from(tools as GoogleCustomWebSearchEngine?)
//            println(webSearchTool)
        }
    }
}
