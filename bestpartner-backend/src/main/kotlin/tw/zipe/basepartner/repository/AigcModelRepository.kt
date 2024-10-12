package tw.zipe.basepartner.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import tw.zipe.basepartner.entity.AigcModel

/**
 * @author Gary
 * @created 2024/10/11
 */
@ApplicationScoped
class AigcModelRepository : PanacheRepository<AigcModel> {
}
