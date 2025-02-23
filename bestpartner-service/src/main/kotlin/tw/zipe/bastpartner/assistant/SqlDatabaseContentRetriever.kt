package tw.zipe.bastpartner.assistant

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.input.Prompt
import dev.langchain4j.model.input.PromptTemplate
import dev.langchain4j.rag.content.Content
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.query.Query
import java.sql.DatabaseMetaData
import java.sql.SQLException
import java.sql.Statement
import javax.sql.DataSource
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.select.Select
import tw.zipe.bastpartner.util.logger

/**
 * 本程式來源由 Langchain4j 專案提供，為一個 SQL 資料庫內容檢索器。
 * 參考: https://github.com/langchain4j/langchain4j/blob/main/experimental/langchain4j-experimental-sql/src/main/java/dev/langchain4j/experimental/rag/content/retriever/sql/SqlDatabaseContentRetriever.java
 */
class SqlDatabaseContentRetriever private constructor(
    private val dataSource: DataSource,
    sqlDialect: String?,
    databaseStructure: String?,
    private val promptTemplate: PromptTemplate,
    private val chatLanguageModel: ChatLanguageModel,
    private val maxRetries: Int
) : ContentRetriever {

    private val logger = logger()

    private val sqlDialect: String = sqlDialect ?: getSqlDialect(dataSource)
    private val databaseStructure: String = databaseStructure ?: generateDDL(dataSource)

    // Builder class
    class Builder {
        private var dataSource: DataSource? = null
        private var sqlDialect: String? = null
        private var databaseStructure: String? = null
        private var promptTemplate: PromptTemplate = DEFAULT_PROMPT_TEMPLATE
        private var chatLanguageModel: ChatLanguageModel? = null
        private var maxRetries: Int = 1

        fun dataSource(dataSource: DataSource) = apply { this.dataSource = dataSource }
        fun sqlDialect(sqlDialect: String) = apply { this.sqlDialect = sqlDialect }
        fun databaseStructure(databaseStructure: String) = apply { this.databaseStructure = databaseStructure }
        fun promptTemplate(promptTemplate: PromptTemplate) = apply { this.promptTemplate = promptTemplate }
        fun chatLanguageModel(chatLanguageModel: ChatLanguageModel) = apply { this.chatLanguageModel = chatLanguageModel }
        fun maxRetries(maxRetries: Int) = apply { this.maxRetries = maxRetries }

        fun build(): SqlDatabaseContentRetriever {
            requireNotNull(dataSource) { "dataSource must not be null" }
            requireNotNull(chatLanguageModel) { "chatLanguageModel must not be null" }

            return SqlDatabaseContentRetriever(
                dataSource = dataSource!!,
                sqlDialect = sqlDialect,
                databaseStructure = databaseStructure,
                promptTemplate = promptTemplate,
                chatLanguageModel = chatLanguageModel!!,
                maxRetries = maxRetries
            )
        }
    }

    companion object {
        private val DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
            """
            You are an expert in writing SQL queries.
            You have access to a {{sqlDialect}} database with the following structure:
            {{databaseStructure}}
            If a user asks a question that can be answered by querying this database, generate an SQL SELECT query.
            Do not output anything else aside from a valid SQL statement!
            """.trimIndent()
        )

        fun builder() = Builder()

        fun getSqlDialect(dataSource: DataSource): String {
            return dataSource.connection.use { connection ->
                connection.metaData.databaseProductName
            }
        }

        private fun generateDDL(dataSource: DataSource): String {
            val ddl = StringBuilder()

            dataSource.connection.use { connection ->
                val metaData = connection.metaData
                val tables = metaData.getTables(null, null, "%", arrayOf("TABLE"))

                while (tables.next()) {
                    val tableName = tables.getString("TABLE_NAME")
                    val createTableStatement = generateCreateTableStatement(tableName, metaData)
                    ddl.append(createTableStatement).append("\n")
                }
            }

            return ddl.toString()
        }

        private fun generateCreateTableStatement(tableName: String, metaData: DatabaseMetaData): String {
            val createTableStatement = StringBuilder()

            try {
                val columns = metaData.getColumns(null, null, tableName, null)
                val pk = metaData.getPrimaryKeys(null, null, tableName)
                val fks = metaData.getImportedKeys(null, null, tableName)

                val primaryKeyColumn = if (pk.next()) pk.getString("COLUMN_NAME") else ""

                createTableStatement
                    .append("CREATE TABLE ")
                    .append(tableName)
                    .append(" (\n")

                while (columns.next()) {
                    val columnName = columns.getString("COLUMN_NAME")
                    val columnType = columns.getString("TYPE_NAME")
                    val size = columns.getInt("COLUMN_SIZE")
                    val nullable = if (columns.getString("IS_NULLABLE") == "YES") " NULL" else " NOT NULL"
                    val columnDef = columns.getString("COLUMN_DEF")?.let { " DEFAULT $it" } ?: ""
                    val comment = columns.getString("REMARKS")

                    createTableStatement
                        .append("  ")
                        .append(columnName)
                        .append(" ")
                        .append(columnType)
                        .append("(")
                        .append(size)
                        .append(")")
                        .append(nullable)
                        .append(columnDef)

                    if (columnName == primaryKeyColumn) {
                        createTableStatement.append(" PRIMARY KEY")
                    }

                    createTableStatement.append(",\n")

                    if (!comment.isNullOrEmpty()) {
                        createTableStatement
                            .append("  COMMENT ON COLUMN ")
                            .append(tableName)
                            .append(".")
                            .append(columnName)
                            .append(" IS '")
                            .append(comment)
                            .append("',\n")
                    }
                }

                while (fks.next()) {
                    val fkColumnName = fks.getString("FKCOLUMN_NAME")
                    val pkTableName = fks.getString("PKTABLE_NAME")
                    val pkColumnName = fks.getString("PKCOLUMN_NAME")
                    createTableStatement
                        .append("  FOREIGN KEY (")
                        .append(fkColumnName)
                        .append(") REFERENCES ")
                        .append(pkTableName)
                        .append("(")
                        .append(pkColumnName)
                        .append("),\n")
                }

                if (createTableStatement[createTableStatement.length - 2] == ',') {
                    createTableStatement.delete(createTableStatement.length - 2, createTableStatement.length)
                }

                createTableStatement.append(");\n")

                val tableRemarks = metaData.getTables(null, null, tableName, null)
                if (tableRemarks.next()) {
                    val tableComment = tableRemarks.getString("REMARKS")
                    if (!tableComment.isNullOrEmpty()) {
                        createTableStatement
                            .append("COMMENT ON TABLE ")
                            .append(tableName)
                            .append(" IS '")
                            .append(tableComment)
                            .append("';\n")
                    }
                }
            } catch (e: SQLException) {
                throw RuntimeException(e)
            }

            return createTableStatement.toString()
        }
    }

    override fun retrieve(naturalLanguageQuery: Query): List<Content> {
        var sqlQuery: String? = null
        var errorMessage: String? = null
        var attemptsLeft = maxRetries + 1

        while (attemptsLeft > 0) {
            attemptsLeft--

            sqlQuery = generateSqlQuery(naturalLanguageQuery, sqlQuery, errorMessage)
            sqlQuery = clean(sqlQuery)

            if (!isSelect(sqlQuery)) {
                return emptyList()
            }

            try {
                validate(sqlQuery)
                logger.info("Executing SQL query: $sqlQuery")
                dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        val result = execute(sqlQuery, statement)
                        val content = format(result, sqlQuery)
                        return listOf(content)
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }

        return emptyList()
    }

    private fun generateSqlQuery(
        naturalLanguageQuery: Query,
        previousSqlQuery: String?,
        previousErrorMessage: String?
    ): String {
        val messages = mutableListOf<ChatMessage>()
        messages.add(createSystemPrompt().toSystemMessage())
        messages.add(UserMessage.from(naturalLanguageQuery.text()))

        if (previousSqlQuery != null && previousErrorMessage != null) {
            messages.add(AiMessage.from(previousSqlQuery))
            messages.add(UserMessage.from(previousErrorMessage))
        }
        return chatLanguageModel.chat(messages).aiMessage().text();
    }

    private fun createSystemPrompt(): Prompt {
        val variables = mapOf(
            "sqlDialect" to sqlDialect,
            "databaseStructure" to databaseStructure
        )
        return promptTemplate.apply(variables)
    }

    private fun clean(sqlQuery: String): String {
        return when {
            sqlQuery.contains("```sql") -> sqlQuery.substring(
                sqlQuery.indexOf("```sql") + 6,
                sqlQuery.lastIndexOf("```")
            )
            sqlQuery.contains("```") -> sqlQuery.substring(
                sqlQuery.indexOf("```") + 3,
                sqlQuery.lastIndexOf("```")
            )
            else -> sqlQuery
        }
    }

    fun validate(sqlQuery: String) {
        // Override this method to add custom validation
    }

    private fun isSelect(sqlQuery: String): Boolean {
        return try {
            CCJSqlParserUtil.parse(sqlQuery) is Select
        } catch (e: JSQLParserException) {
            false
        }
    }

    private fun execute(sqlQuery: String, statement: Statement): String {
        val resultRows = mutableListOf<String>()

        statement.executeQuery(sqlQuery).use { resultSet ->
            val columnCount = resultSet.metaData.columnCount

            // header
            val columnNames = (1..columnCount).map {
                resultSet.metaData.getColumnName(it)
            }
            resultRows.add(columnNames.joinToString(","))

            // rows
            while (resultSet.next()) {
                val columnValues = (1..columnCount).map { i ->
                    resultSet.getObject(i)?.toString() ?: ""
                }.map { columnValue ->
                    if (columnValue.contains(",")) "\"$columnValue\"" else columnValue
                }
                resultRows.add(columnValues.joinToString(","))
            }
        }

        return resultRows.joinToString("\n")
    }

    private fun format(result: String, sqlQuery: String): Content {
        return Content.from("Result of executing '$sqlQuery':\n$result")
    }
}
