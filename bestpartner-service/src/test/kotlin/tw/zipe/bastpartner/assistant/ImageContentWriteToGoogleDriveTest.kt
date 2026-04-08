package tw.zipe.bastpartner.assistant

import dev.langchain4j.data.message.ImageContent
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.mcp.McpToolProvider
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.McpClient
import dev.langchain4j.mcp.client.transport.McpTransport
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport
import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.service.AiServices
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.util.Base64
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder

/**
 * @author Gary
 * @created 2025/4/16
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ImageContentWriteToGoogleDriveTest {

    private lateinit var geminiChatModel: GoogleAiGeminiChatModel
    private lateinit var openaiChatModel: OpenAiChatModel
    private lateinit var grokChatModel: OpenAiChatModel
    private lateinit var anthropicChatModel: AnthropicChatModel
    private val imagePath = "src\\test\\kotlin\\tw\\zipe\\bastpartner\\assistant\\images\\image1.jpg"
    var imageJsonResult: String = String()
    var readFileContentJson: String = String()

    @BeforeEach
    fun setUp() {
        geminiChatModel = buildGeminiModel()
        openaiChatModel = buildOpenAIModel()
        grokChatModel = buildGrokModel()
        anthropicChatModel = buildAnthropicModel()
    }

    //    @Test
//    @Order(1)
    fun `test analysis image`() {
        try {
            val tempFile = File(imagePath)
            val mimeType = Files.probeContentType(tempFile.toPath()) ?: "image/png"

            val imageContent = ImageContent.from(
                String(
                    Base64.getEncoder()
                        .encode(File(tempFile.toURI()).readBytes())
                ), mimeType
            )

            val prompt = """
                            請用中文描述圖中標籤部分資訊，輸出格式為json，並且包含以下欄位：
                        {
                          "profuct_name":"產品名稱",
                          "product_info":"產品資訊",
                          "original_price":10000,
                          "discount_price":500,
                          "description":"產品其他描述"
                        }
            """.trimIndent()

            val result = geminiChatModel.chat(
                UserMessage.userMessage(
                    TextContent.from(prompt),
                    imageContent
                )
            )

            imageJsonResult = extractJsonFromResponse(result)

            // 可以在此处添加断言验证结果
            Assertions.assertNotNull(imageJsonResult)
            Assertions.assertTrue(imageJsonResult.isNotEmpty())

            println("Image JSON Result: $imageJsonResult")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    @Order(2)
    fun `test write google drive`() {
        println("jsonResult: $imageJsonResult")
        val dateTransport: McpTransport = StdioMcpTransport.Builder()
            .command(listOf("java", "-jar", "D:/MCP/date-1.0-SNAPSHOT-runner.jar")).logEvents(true)
            .build()

        val dateMcpClient: McpClient = DefaultMcpClient.Builder()
            .transport(dateTransport)
            .build()

        val fileSystemTransport: McpTransport = StdioMcpTransport.Builder()
            .command(listOf("C:/nvm4w/nodejs/npx.cmd", "-y", "@modelcontextprotocol/server-filesystem", "D:/doc", "G:/我的雲端硬碟"))
            .logEvents(true)
            .build()

        val fileSystemMcpClient: McpClient = DefaultMcpClient.Builder()
            .transport(fileSystemTransport)
            .build()
        val mcpToolProvider = McpToolProvider.builder()
            .mcpClients(listOf(dateMcpClient, fileSystemMcpClient))
            .build()

        val assistant = AiServices.builder(AIAssistant::class.java).systemMessageProvider { _ ->
            """
            你是一個AI助手，會遵循使用者指令完成任務。
            """.trimIndent()
        }
            .chatModel(grokChatModel)
            .toolProvider(mcpToolProvider)
            .build()
        try {
            val answer = assistant.chat(
                """
                    **目標：** 更新每日銷售紀錄 CSV 檔案 (`account.csv`)。

                    **執行步驟 (請確保按以下順序操作)：**
                    
                    1.  **定位檔案：**
                        *   確認**今天**的日期。
                        *   將日期格式化為 `YYYY-MM-DD` (或您目錄使用的格式，例如 `YYYYMMDD`)。
                        *   找到目標檔案：`G:/我的雲端硬碟/[格式化後的今日日期]/account.csv`。
                    
                    2.  **讀取與準備資料：**
                        *   使用支援 `UTF-8` 編碼的工具（如試算表軟體、文字編輯器）開啟指定的 `account.csv` 檔案。(大部分工具讀取時能自動處理有無 BOM 的 UTF-8)
                        *   **識別並暫時移除**檔案中現有的「總計」列。
                            *   *假設：* 此列可透過第一欄 (或 `product_name` 欄) 的值為 "總計" 來識別。請確認實際標識。
                            *   *操作：* 將此列複製到旁邊或暫存區備用，然後從主要資料區域刪除。
                    
                    3.  **準備新資料：**
                        *   準備要新增的一筆新資料：
                            ```
                            product_name: 熱壓三明治機
                            product_info: 全新
                            original_price: 690
                            discount_price: 250
                            description: 可壓雙片喔!
                            ```
                        *   確保此資料的欄位順序與 CSV 檔案的標頭 (Header) 一致。
                    
                    4.  **新增資料：**
                        *   將步驟 3 準備好的新資料，作為新的一列**附加**到主要資料區域的末尾（即，在所有產品資料之後，移除舊「總計」列的位置）。
                    
                    5.  **重新計算總計：**
                        *   檢查所有包含產品資料的列（**包含**剛剛在步驟 4 新增的列）。
                        *   計算 `original_price` 欄位所有數值的總和。
                        *   計算 `discount_price` 欄位所有數值的總和。
                    
                    6.  **創建新的總計列：**
                        *   基於步驟 5 計算出的總和，創建一個新的「總計」列。
                        *   將第一欄 (或 `product_name` 欄) 設為 "總計"。
                        *   將計算出的 `original_price` 總和填入對應的欄位。
                        *   將計算出的 `discount_price` 總和填入對應的欄位。
                        *   其他欄位（如 `product_info`, `description`）可以留空或填入特定標識 (例如 `---`)。
                    
                    7.  **放置新的總計列：**
                        *   將步驟 6 創建的**新**「總計」列，放置在整個資料表格的**最後一行**。
                    
                    8.  **儲存檔案：**
                        *   將修改後的完整資料（包含標頭、所有產品資料列、以及位於最後一行的**新**「總計」列），**儲存**回**原始檔案路徑** (`G:/我的雲端硬碟/[格式化後的今日日期]/account.csv`)。
                        *   **重要：** 儲存時，請務必選擇 **CSV (逗號分隔值)** 格式，並指定使用 **具有 BOM 的 UTF-8 (UTF-8 with BOM)** 編碼。
                """.trimIndent()
            )
            readFileContentJson = answer.content().text()
            println(readFileContentJson)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildGeminiModel(): GoogleAiGeminiChatModel {
        return GoogleAiGeminiChatModel.builder()
            .apiKey(System.getenv("GEMINI_API_KEY"))
            .modelName("gemini-2.0-flash")
            .temperature(0.1)
            .topP(0.5)
            .maxOutputTokens(4096)
            .timeout(Duration.ofSeconds(6000))
            .logRequestsAndResponses(true)
            .build()
    }

    private fun buildOpenAIModel(): OpenAiChatModel {
        return OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4.1-2025-04-14")
            .temperature(0.3)
            .topP(0.5)
            .maxTokens(32768)
            .timeout(Duration.ofSeconds(6000))
            .logRequests(true)
            .logResponses(true)
            .build()

    }

    private fun buildGrokModel(): OpenAiChatModel {
        return OpenAiChatModel.builder()
            .baseUrl("https://api.x.ai/v1")
            .apiKey(System.getenv("GROK_API_KEY"))
            .modelName("grok-3-fast-beta")
            .temperature(0.1)
            .topP(0.5)
            .maxTokens(32768)
            .timeout(Duration.ofSeconds(6000))
            .logRequests(true)
            .logResponses(true)
            .build()

    }

    private fun buildAnthropicModel(): AnthropicChatModel {
        return AnthropicChatModel.builder()
            .apiKey(System.getenv("ANTHROPIC_API_KEY"))
            .modelName("claude-3-7-sonnet-20250219")
            .temperature(0.7)
            .topP(0.5)
            .maxTokens(4096)
            .timeout(Duration.ofSeconds(6000))
            .logRequests(true)
            .logResponses(true)
            .build()
    }

    private fun extractJsonFromResponse(response: ChatResponse): String {
        return response.aiMessage().text().replace("```json", "").replace("```", "").trim()
    }
}
