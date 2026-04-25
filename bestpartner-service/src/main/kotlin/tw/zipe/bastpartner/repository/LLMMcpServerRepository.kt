package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.dto.McpDTO
import tw.zipe.bastpartner.entity.LLMMcpServerEntity

/**
 * @author Gary
 * @created 2025/3/19
 */
@ApplicationScoped
class LLMMcpServerRepository : BaseRepository<LLMMcpServerEntity, String>() {

    @Transactional
    fun update(mcpDto: McpDTO): Int {
        val entity = findById(mcpDto.mcpId.orEmpty()) ?: return 0
        entity.name = mcpDto.name.orEmpty()
        // commandSetting 為 JSON 欄位，Hibernate Converter 自動序列化，無需手動 writeValueAsString
        mcpDto.commandSetting?.let { entity.commandSetting = it }
        // type 為 lateinit，僅在 DTO 有值時才更新
        mcpDto.type?.let { entity.type = it }
        // @PreUpdate 自動設定 updatedAt / updatedBy
        update(entity)
        return 1
    }
}
