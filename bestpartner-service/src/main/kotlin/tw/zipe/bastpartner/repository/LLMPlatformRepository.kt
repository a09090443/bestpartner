package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMPlatformEntity

/**
 * @author Gary
 * @created 2024/12/23
 */
@ApplicationScoped
class LLMPlatformRepository : BaseRepository<LLMPlatformEntity, String>() {
}
