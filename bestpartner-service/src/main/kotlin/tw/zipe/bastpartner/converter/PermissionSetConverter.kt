package tw.zipe.bastpartner.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tw.zipe.bastpartner.enumerate.Permission

/**
 * @author Gary
 * @created 2024/10/25
 */
@Converter
class PermissionSetConverter : AttributeConverter<Set<Permission>, String> {

    // 将 Set<Permission> 转换为数据库中的字符串，用逗号分隔
    override fun convertToDatabaseColumn(attribute: Set<Permission>?): String {
        return attribute?.joinToString(",") { it.name } ?: ""
    }

    // 将字符串转换为 Set<Permission>
    override fun convertToEntityAttribute(dbData: String?): Set<Permission> {
        return dbData?.split(",")?.mapNotNull {
            try { Permission.valueOf(it) } catch (e: IllegalArgumentException) { null }
        }?.toSet() ?: emptySet()
    }
}
