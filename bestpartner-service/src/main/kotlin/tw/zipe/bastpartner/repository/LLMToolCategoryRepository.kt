package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMToolCategoryEntity

/**
 * @author Gary
 * @created 2024/12/5
 */
@ApplicationScoped
class LLMToolCategoryRepository : BaseRepository<LLMToolCategoryEntity, String>(){
}
