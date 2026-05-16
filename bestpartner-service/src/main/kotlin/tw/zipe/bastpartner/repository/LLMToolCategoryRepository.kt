package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import tw.zipe.bastpartner.entity.LLMToolCategoryEntity

/**
 * @author Gary
 * @created 2024/12/5
 */
@ApplicationScoped
class LLMToolCategoryRepository : BaseRepository<LLMToolCategoryEntity, String>() {

    @Transactional
    fun updateByNative(id: String, name: String, description: String): Int {
        val entity = findById(id) ?: return 0
        entity.name = name
        entity.description = description
        // @PreUpdate 自動設定 updatedAt / updatedBy
        update(entity)
        return 1
    }
}
