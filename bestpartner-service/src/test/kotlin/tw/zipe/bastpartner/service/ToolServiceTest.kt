package tw.zipe.bastpartner.service

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
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
        val result = toolService.deleteTool(toolDTO.id.orEmpty())
        assert(result)
    }

    companion object {
        lateinit var toolDTO: ToolDTO

        @BeforeAll
        @JvmStatic
        fun init() {
//            dateTool()
//            googleSearchTool()
            tavilySearchTool()
        }

        private fun dateTool() {
            toolDTO = ToolDTO(
                name = "DateTool",
                classPath = "tw.zipe.bastpartner.tool.DateTool",
                groupId = "ea8a08e2-342d-4ade-9579-127d2d1443c5",
                groupDescription = "日期群組",
                type = ToolsType.BUILT_IN,
                description = "內建日期工具",
            )
        }

        private fun googleSearchTool() {
            toolDTO = ToolDTO(
                name = "GoogleSearch",
                classPath = "dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine",
                groupId = "90caee3f-2c87-48b9-8912-3dd810f62377",
                groupDescription = "網頁搜尋群組",
                configObjectPath = "tw.zipe.bastpartner.tool.config.Google",
                settingContent = "{\"apiKey\":\"AIzaSyDOSA26AyzMOO87_j_fcypWVoaTJXqaYj0\",\"csi\":\"f009fbd9a12af4ccb\",\"siteRestrict\":false,\"includeImages\":true,\"timeout\":100000,\"maxRetries\":10,\"logRequests\":true,\"logResponses\":true}",
                type = ToolsType.BUILT_IN,
                description = "內建 Google 搜尋工具",
            )
        }

        private fun tavilySearchTool() {
            toolDTO = ToolDTO(
                name = "TavilySearch",
                classPath = "dev.langchain4j.web.search.tavily.TavilyWebSearchEngine",
                groupId = "90caee3f-2c87-48b9-8912-3dd810f62377",
                groupDescription = "網頁搜尋群組",
                configObjectPath = "tw.zipe.bastpartner.tool.config.Tavily",
                settingContent = "{\"apiKey\":\"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\",\"timeout\":100000,\"includeAnswer\":true,\"includeRawContent\":false,\"includeDomains\":[],\"excludeDomains\":[]}",
                type = ToolsType.BUILT_IN,
                description = "內建 Tavily 搜尋工具",
            )
        }
    }
}
