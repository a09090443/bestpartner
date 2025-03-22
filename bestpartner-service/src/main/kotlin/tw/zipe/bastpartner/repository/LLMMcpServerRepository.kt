package tw.zipe.bastpartner.repository

import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.bastpartner.entity.LLMMcpServerEntity

/**
 * @author Gary
 * @created 2025/3/19
 */
@ApplicationScoped
class LLMMcpServerRepository : BaseRepository<LLMMcpServerEntity, String>() {
}
