package tw.zipe.bastpartner.tool.text2sql

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysql.cj.jdbc.MysqlDataSource
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.rag.query.Query
import dev.langchain4j.service.tool.ToolExecutor
import javax.sql.DataSource
import org.postgresql.ds.PGSimpleDataSource
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
): ToolExecutor {
    private val logger = logger()

    fun buildDatasource(): DataSource {
        return when (DatabaseType.valueOf(datasourceDatabaseType)) {
            DatabaseType.MYSQL -> MysqlDataSource().apply {
                setURL(datasourceUrl)
                user = datasourceUsername
                password = datasourcePassword
            }
            DatabaseType.POSTGRESQL -> PGSimpleDataSource().apply {
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
            logRequests = true
            logResponses = true
        }

        return LLMBuilder().build(Platform.getPlatform(platform), llmModel, ModelType.CHAT).let {
            it as ChatLanguageModel
        }
    }

    override fun execute(toolExecutionRequest: ToolExecutionRequest?, memoryId: Any?): String {
        logger.info("呼叫 Text2SQL 工具")
        val paramMap = ObjectMapper().readValue(toolExecutionRequest?.arguments(), Map::class.java)
        val query = paramMap["query"].toString()

        val content = SqlDatabaseContentRetriever.builder()
            .dataSource(buildDatasource())
            .sqlDialect(datasourceDatabaseType)
//            .databaseStructure("")
            .chatLanguageModel(buildLLM())
            .build()
        val retrieved = content.retrieve(Query.from(query))
        return retrieved[0].textSegment().text()
    }
}
