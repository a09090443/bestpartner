package tw.zipe.bastpartner.tool.text2sql.config

import tw.zipe.bastpartner.tool.text2sql.enumerate.DatabaseType

data class DataSourceConfig(
    val url: String,
    val username: String,
    val password: String,
    val databaseType: DatabaseType
)
