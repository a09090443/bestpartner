package tw.zipe.bastpartner.tool.text2sql.enumerate

import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.util.MessageUtil

enum class DatabaseType {
    MYSQL, POSTGRESQL, ORACLE, SQLSERVER;

    companion object {
        fun fromName(name: String): DatabaseType {
            return DatabaseType.entries.firstOrNull { it.name == name }
                ?: throw IllegalArgumentException(MessageUtil.get(AppMessage.SYSTEM_DATABASE_TYPE_NOT_FOUND))
        }
    }
}
