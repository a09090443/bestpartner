package tw.zipe.bastpartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity_.id
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import org.bouncycastle.asn1.x500.style.RFC4519Style.description
import tw.zipe.bastpartner.entity.SystemSettingEntity

/**
 * @author Gary
 * @created 2024/10/19
 */
@ApplicationScoped
class SystemSettingRepository : BaseRepository<SystemSettingEntity, String>() {

    fun findBySettingKey(settingKey: String) = find("settingKey", settingKey).firstResult()

    fun deleteByKey(settingKey: String) = delete("settingKey", settingKey)

    fun updateSystemSettingByNative(id: Long, settingKey: String, settingValue: String, description: String): Int {
        val paramMap =
            mapOf("id" to id, "settingKey" to settingKey, "settingValue" to settingValue, "description" to description)
        val sql = """
            UPDATE system_setting ss
            SET ss.setting_key = :settingKey, setting_value = :settingValue, description = :description
            WHERE ss.id = :id
        """.trimIndent()
        val executor = createSqlExecutor()
            .withSql(sql)
            .withParamMap(paramMap)
        return executeUpdateWithTransaction(executor)
    }
}
