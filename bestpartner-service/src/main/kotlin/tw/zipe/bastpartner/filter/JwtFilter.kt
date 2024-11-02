package tw.zipe.bastpartner.filter

import io.netty.util.internal.StringUtil
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import tw.zipe.bastpartner.exception.JwtValidationException
import tw.zipe.bastpartner.service.JwtService

/**
 * @author Gary
 * @created 2024/10/25
 */
@Provider
@PreMatching
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
class JwtFilter(
    val jwtService: JwtService
) : ContainerRequestFilter {

    override fun filter(requestContext: ContainerRequestContext) {
        val token = extractToken(requestContext) ?: return
        val payload = jwtService.getTokenPayload(token)
        when {
            jwtService.isTokenNeedingRefresh(token) -> {
                // Token接近過期，生成新token
                handleTokenRefresh(requestContext, payload)
            }

            jwtService.isTokenExpired(token) -> {
                // Token已過期，嘗試重新生成
                throw JwtValidationException("憑證過期，請重新登入")
            }
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

            // 在響應頭中添加新的token
            requestContext.headers.add("New-Token", newToken)
            return newToken
        } catch (e: Exception) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token refresh failed")
                    .build()
            )
        }
        return StringUtil.EMPTY_STRING
    }

    private fun extractToken(requestContext: ContainerRequestContext): String? {
        val authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else null
    }
}
