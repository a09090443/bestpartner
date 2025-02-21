package tw.zipe.bastpartner.tool.text2sql.enumerate

enum class DatabaseType {
    MYSQL, POSTGRESQL, ORACLE, SQLSERVER;

    companion object {
        fun fromName(name: String): DatabaseType {
            return DatabaseType.entries.firstOrNull { it.name == name }
                ?: throw IllegalArgumentException("找不到對應的資料庫類型")
        }
    }
}
