package tw.zipe.bastpartner.config.security

import io.quarkus.security.identity.SecurityIdentity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import tw.zipe.bastpartner.exception.ServiceException

/**
 * @author Gary
 * @created 2024/12/4
 */
@ApplicationScoped
class SecurityValidator @Inject constructor(private val securityIdentity: SecurityIdentity) {

    /**
     * 確認使用者是否已登入，並返回使用者名稱
     * @return 使用者名稱
     * @throws ServiceException 當使用者未登入時
     */
    fun validateLoggedInUser(): String {
        return securityIdentity.principal?.name
            ?.takeIf { it.isNotEmpty() }
            ?: throw ServiceException("請確認已登入")
    }
}
