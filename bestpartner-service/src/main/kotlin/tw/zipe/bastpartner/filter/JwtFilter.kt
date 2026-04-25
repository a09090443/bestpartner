package tw.zipe.bastpartner.filter

import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.exception.JwtValidationException
import tw.zipe.bastpartner.exception.ServiceException
import tw.zipe.bastpartner.service.JwtService
import tw.zipe.bastpartner.util.MessageUtil
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

    @ConfigProperty(name = "jwt.refresh.switch", defaultValue = "false")
    private lateinit var jwtRefreshSwitch: String

    override fun filter(requestContext: ContainerRequestContext) {
        val token = extractToken(requestContext) ?: return
        val payload = try {
            jwtService.getTokenPayload(token)
        } catch (e: Exception) {
            throw JwtValidationException(MessageUtil.get(AppMessage.AUTH_TOKEN_INVALID), null)
        }
        if (jwtRefreshSwitch.toBoolean()) {
            jwtService.isTokenNeedingRefresh(token).takeIf { it }?.let {
                val newToken = handleTokenRefresh(requestContext, payload)
                throw JwtValidationException(MessageUtil.get(AppMessage.AUTH_TOKEN_EXPIRED_REFRESH), newToken)
            }
        }
        jwtService.isTokenExpired(token).takeIf { it }?.let {
            throw JwtValidationException(MessageUtil.get(AppMessage.AUTH_TOKEN_EXPIRED_RELOGIN), null)
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
            throw ServiceException(AppMessage.AUTH_TOKEN_REFRESH_ERROR)
        }
    }

    private fun extractToken(requestContext: ContainerRequestContext): String? {
        val authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else null
    }

}
