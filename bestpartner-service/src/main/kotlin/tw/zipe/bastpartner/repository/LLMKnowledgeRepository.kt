package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMKnowledgeEntity

/**
 * @author Gary
 * @created 2025/3/12
 */
@ApplicationScoped
class LLMKnowledgeRepository : BaseRepository<LLMKnowledgeEntity, String>() {
}
