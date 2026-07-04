package tw.zipe.bastpartner.service

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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

//    @Test
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

    // 依賴上方已註解的 `test register tool` 產生 toolDTO.id，單獨執行必然 NPE，比照檔內慣例註解停用
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

    @Test
    fun `getTool 回傳結構化 settingSchema`() {
        // init-data 內建 Google 工具實際 name 為 GoogleSearch，configObjectPath 指向 tool.config.Google
        val google = toolService.getTools().firstOrNull { it.name == "GoogleSearch" }
        assertNotNull(google, "init-data 缺 GoogleSearch 工具（請執行 docs/sql/bestpartner-init-data.sql）")
        val tool = toolService.getTool(google!!.id!!)
        val schema = tool.settingSchema!!
        assertTrue(schema.containsKey("apiKey"))
        assertEquals("string", schema["apiKey"]!!.jsonObject["type"]!!.jsonPrimitive.content)
    }

    @Test
    fun `getTools 清單依 configObjectPath 有無帶 settingSchema 或 null`() {
        val tools = toolService.getTools()
        val google = tools.firstOrNull { it.name == "GoogleSearch" }
        assertNotNull(google, "init-data 缺 GoogleSearch 工具（請執行 docs/sql/bestpartner-init-data.sql）")
        assertNotNull(google!!.settingSchema)
        // DateTool 無 configObjectPath，settingSchema 應為 null
        val dateTool = tools.firstOrNull { it.name == "DateTool" }
        assertNotNull(dateTool, "init-data 缺 DateTool 工具（請執行 docs/sql/bestpartner-init-data.sql）")
        assertNull(dateTool!!.settingSchema)
    }

    companion object {
        lateinit var toolDTO: ToolDTO

        @BeforeAll
        @JvmStatic
        fun init() {
//            dateTool()
            googleSearchTool()
//            tavilySearchTool()
        }

        private fun dateTool() {
            toolDTO = ToolDTO(
                name = "DateTool",
                classPath = "tw.zipe.bastpartner.tool.DateTool",
                groupId = "ea8a08e2-342d-4ade-9579-127d2d1443c5",
                groupDescription = "日期群組",
                type = ToolsType.CUSTOMIZE,
                description = "日期工具",
                functionName = "getCurrentTime",
                functionDescription = "以台灣時間為基準，會根據不同時區取得當地日期時間",
                functionParams = "{\"zoneId\":[\"輸入格式為國家/城市，如:Australia/Darwin, Asia/Taipei, Africa/Harare\", \"String\"]}"
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
