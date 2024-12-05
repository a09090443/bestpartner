package tw.zipe.bastpartner.service

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import tw.zipe.bastpartner.dto.ToolDTO
import tw.zipe.bastpartner.enumerate.ToolsType

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
    fun `test find all tools`() {
        val result = toolService.getTools()
        assertNotNull(result)
    }

//    @Test
//    @Order(1)
    fun `test add category`() {
        toolService.saveCategory(toolDTO)
        assertNotNull(toolDTO.groupId)
    }

//    @Test
//    @Order(2)
    fun `test register tool`() {
        toolService.registerTool(toolDTO)
        assertNotNull(toolDTO.id)
    }

//    @Test
//    @Order(3)
    fun `test find tool by id`() {
        val result = toolService.findToolById(toolDTO.id!!)
        assertEquals(toolDTO.name, result?.name)
    }

//    @Test
//    @Order(4)
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
                group = "DATE",
                groupDescription = "日期群組",
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
                group = "WEB_SEARCH",
                groupDescription = "網頁搜尋群組",
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
