package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.VectorStoreSettingEntity
import tw.zipe.bastpartner.enumerate.VectorStore

/**
 * @author Gary
 * @created 2024/10/11
 */
@ApplicationScoped
class VectorStoreSettingRepository : BaseRepository<VectorStoreSettingEntity, String>() {

    fun findByAlias(alias: String) = find("alias", alias).singleResult()

    fun findByAccountAndType(account: String, type: VectorStore?): List<VectorStoreSettingEntity> {
        // 建立基礎查詢和參數 Map
        val conditions = mutableListOf<String>()
        val parameters = mutableMapOf<String, Any>()
        conditions.add("account = :account")
        parameters["account"] = account

        type?.let {
            conditions.add("type = :type")
            parameters["type"] = it
        }

        // 構建最終查詢字串
        val query = if (conditions.isEmpty()) {
            "1=1"
        } else {
            conditions.joinToString(" and ")
        }

        return list(query, parameters)
    }

    fun updateSetting(parasMap: Map<String, Any>): Int {
        val sql = """
            UPDATE vector_store_setting vss 
            SET vss.alias = :alias,
                vss.type = :type,
                vss.vector_setting = :vectorSetting
            WHERE vss.id = :id
        """.trimIndent()
        val executor = createSqlExecutor().withSql(sql).withParamMap(parasMap)
        return executeUpdateWithTransaction(executor)
    }
}
