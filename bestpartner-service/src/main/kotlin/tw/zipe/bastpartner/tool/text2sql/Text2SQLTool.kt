package tw.zipe.bastpartner.tool.text2sql

import com.mysql.cj.jdbc.MysqlDataSource
import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.rag.query.Query
import javax.sql.DataSource
import tw.zipe.bastpartner.assistant.SqlDatabaseContentRetriever
import tw.zipe.bastpartner.enumerate.ModelType
import tw.zipe.bastpartner.enumerate.Platform
import tw.zipe.bastpartner.model.LLModel
import tw.zipe.bastpartner.tool.text2sql.enumerate.DatabaseType
import tw.zipe.bastpartner.util.LLMBuilder
import tw.zipe.bastpartner.util.logger

class Text2SQLTool(
    private val platform: String,
    private val llmApiKey: String?,
    private val llmUrl: String?,
    private val llmModelName: String,
    private val datasourceUrl: String,
    private val datasourceUsername: String,
    private val datasourcePassword: String,
    private val datasourceDatabaseType: String,
) {
    private val logger = logger()

    @Tool(name = "text2sql", value = ["將自然語言查詢轉換為對應的 SQL 查詢語句。"])
    fun text2sql(@P("query") query: String?): String {
        logger.info("呼叫 Text2SQL 工具")

        val content = SqlDatabaseContentRetriever.builder()
            .dataSource(buildDatasource())
            .sqlDialect(datasourceDatabaseType)
//            .databaseStructure("")
            .chatLanguageModel(buildLLM())
            .build()
        val retrieved = content.retrieve(Query.from(query))
        return retrieved[0].textSegment().text()
    }

    fun buildDatasource(): DataSource {
        return when (DatabaseType.valueOf(datasourceDatabaseType)) {
            DatabaseType.MYSQL -> MysqlDataSource().apply {
                setURL(datasourceUrl)
                user = datasourceUsername
                password = datasourcePassword
            }

            else -> throw IllegalArgumentException("Unsupported database type: $datasourceDatabaseType")
        }
    }

    private fun buildLLM(): ChatLanguageModel {
        val llmModel: LLModel = LLModel().apply {
            apiKey = llmApiKey
            url = llmUrl
            modelName = llmModelName
            temperature = 0.0
        }

        return LLMBuilder().build(Platform.getPlatform(platform), llmModel, ModelType.CHAT).let {
            it as ChatLanguageModel
        }
    }
}
