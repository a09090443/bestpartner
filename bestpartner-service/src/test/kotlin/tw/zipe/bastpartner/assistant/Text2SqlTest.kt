package tw.zipe.bastpartner.assistant

import com.mysql.cj.jdbc.MysqlDataSource
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.rag.query.Query
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.enumerate.Platform
import tw.zipe.bastpartner.service.LLMService
import tw.zipe.bastpartner.tool.text2sql.Text2SQLTool
import tw.zipe.bastpartner.tool.text2sql.config.Text2SQL
import tw.zipe.bastpartner.tool.text2sql.enumerate.DatabaseType
import tw.zipe.bastpartner.util.instantiate
import tw.zipe.bastpartner.util.reorderAndRenameArguments


@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class Text2SqlTest {

    @Inject
    lateinit var llmService: LLMService

    @Test
    @Order(1)
    fun `test instance tool`() {

        val configClazz = Class.forName("tw.zipe.bastpartner.tool.text2sql.config.Text2SQL")
        val fields = configClazz.declaredFields.joinToString(", ") { it.name }
        val sortFields = reorderAndRenameArguments(paramMap, fields)

        configInstance = instantiate(configClazz.kotlin, sortFields)
        assert(configInstance is Text2SQL)

        val toolClazz = Class.forName("tw.zipe.bastpartner.tool.text2sql.Text2SQLTool")
        val toolFields = toolClazz.declaredFields.joinToString(", ") { it.name }
        val toolSortFields = reorderAndRenameArguments(mapOf("text2SQL" to configInstance!!), toolFields)
        instance = instantiate(toolClazz.kotlin, toolSortFields)
        assert(instance is Text2SQLTool)

    }

//    @Test
//    @Order(2)
//    @TestSecurity(user = "670017b4-23d0-4339-a9c0-22b6d9446461", roles = ["admin", "user"])
    fun `test datasource`() {
        val llm = llmService.buildLLM("f138c97a-a7fe-4328-9ff5-5f25375739ba", ModelType.CHAT).let {
            it as ChatLanguageModel
        }
        val mysqlDataSource = MysqlDataSource()
        mysqlDataSource.setURL("jdbc:mysql://localhost:3306/sale?allowPublicKeyRetrieval=true&useSSL=false")
        mysqlDataSource.user = "sale"
        mysqlDataSource.password = "sale"

        val content = SqlDatabaseContentRetriever.builder()
            .dataSource(mysqlDataSource)
            .sqlDialect("MYSQL")
//            .databaseStructure("")
            .chatLanguageModel(llm)
            .build()

        val retrieved = content.retrieve(Query.from("How many customers do we have?"))

        println(retrieved[0].textSegment().text())
    }

    companion object {
        lateinit var paramMap: Map<String, Any>

        var instance: Any? = null

        var configInstance: Any? = null

        @BeforeAll
        @JvmStatic
        fun init() {
//            val text2SQL = Text2SQL("test", "test", "test", "test", "test", "test", "test", "test")
//            paramMap = mapOf(
//                "text2sql" to text2SQL
//            )
            paramMap = mapOf(
                "platform" to Platform.OLLAMA.name,
                "llmApiKey" to "",
                "llmUrl" to "http://localhost:11434",
                "llmModelName" to "llama3.1:latest",
                "datasourceUrl" to "jdbc:mysql://localhost:3306/sale?useSSL=false",
                "datasourceUsername" to "sale",
                "datasourcePassword" to "sale",
                "datasourceDatabaseType" to DatabaseType.MYSQL.name
            )
        }
    }
}
