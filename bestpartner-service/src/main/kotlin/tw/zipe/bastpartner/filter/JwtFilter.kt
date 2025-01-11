package tw.zipe.bastpartner.filter

import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty
import tw.zipe.bastpartner.exception.JwtValidationException
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.JwtService
import tw.zipe.bastpartner.util.logger

/**
 * @author Gary
 * @created 2024/10/25
 */
@Provider
@PreMatching
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
class JwtFilter(
    val jwtService: JwtService,
) : ContainerRequestFilter {

    private val logger = logger()

    @Inject
    @ConfigProperty(name = "jwt.refresh.switch", defaultValue = "false")
    private lateinit var jwtRefreshSwitch: String

    override fun filter(requestContext: ContainerRequestContext) {
        val token = extractToken(requestContext) ?: return
        val payload = jwtService.getTokenPayload(token)
        if (jwtRefreshSwitch.toBoolean()) {
            jwtService.isTokenNeedingRefresh(token).takeIf { it }?.let {
                val newToken = handleTokenRefresh(requestContext, payload)
                throw JwtValidationException("憑證過期，已產生新Token", newToken)
            }
        }
        jwtService.isTokenExpired(token).takeIf { it }?.let {
            throw JwtValidationException("憑證過期，請重新登入", null)
        }
    }

    private fun handleTokenRefresh(requestContext: ContainerRequestContext, payload: Map<String, Any>?): String {
        try {
            val userId = payload?.let { content -> (content["upn"] as? String) }.orEmpty()
            val permissions = payload?.let { content ->
                (content["groups"] as? List<*>)?.mapNotNull { it as? String }?.toSet()
            } ?: emptySet()

            val newToken = jwtService.generateJwtToken(userId, permissions)

            // 更新請求中的 Authorization header
            requestContext.headers.remove(HttpHeaders.AUTHORIZATION)
            requestContext.headers.add(HttpHeaders.AUTHORIZATION, "Bearer $newToken")
            return newToken
        } catch (e: Exception) {
            logger.error("Token 更新錯誤", e)
            throw ServiceException("Token 更新錯誤")
        }
    }

    private fun extractToken(requestContext: ContainerRequestContext): String? {
        val authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else null
    }

}
