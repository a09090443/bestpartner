package tw.zipe.bastpartner.filter

import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
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
class JwtFilter : ContainerRequestFilter {

    @Inject
    lateinit var jwtService: JwtService

    override fun filter(requestContext: ContainerRequestContext) {
        val token = extractToken(requestContext) ?: return
        val payload = jwtService.getTokenPayload(token)
        when {
            jwtService.isTokenNeedingRefresh(token) -> {
                // Token接近過期，生成新token
                handleTokenRefresh(requestContext, payload)
                throw JwtValidationException("憑證過期，請重新登入")
            }

            jwtService.isTokenExpired(token) -> {
                // Token已過期，嘗試重新生成
                handleTokenRefresh(requestContext, payload)
            }
        }
    }

    private fun checkUserPermission(payload: Map<String, Any>?): Boolean {
        // 自定义权限检查逻辑
        // 比如：检查用户是否具有所需的角色
        val permissions = payload?.get("groups") as? List<*> ?: return false
        return permissions.contains("required-role")  // 假设需要 "required-role" 权限
    }

    private fun handleTokenRefresh(requestContext: ContainerRequestContext, payload: Map<String, Any>?) {
        try {
            val userId = requestContext.getHeaderString("user-id") ?: throw Exception("user-id not found")
            val permissions = payload?.let { content ->
                (content["groups"] as? List<*>)?.mapNotNull { it as? String }?.toSet()
            } ?: emptySet()

            val newToken = jwtService.generateJwtToken(userId, permissions)

            // 更新請求中的 Authorization header
            requestContext.headers.remove(HttpHeaders.AUTHORIZATION)
            requestContext.headers.add(HttpHeaders.AUTHORIZATION, "Bearer $newToken")

            // 在響應頭中添加新的token
            requestContext.headers.add("New-Token", newToken)
        } catch (e: Exception) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token refresh failed")
                    .build()
            )
        }
    }

    private fun extractToken(requestContext: ContainerRequestContext): String? {
        val authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else null
    }
}
