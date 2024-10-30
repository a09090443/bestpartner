package tw.zipe.bastpartner.filter

import io.quarkus.security.identity.SecurityIdentity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.ext.Provider
import tw.zipe.bastpartner.service.JwtService

/**
 * @author Gary
 * @created 2024/10/25
 */
@Provider
@PreMatching
@ApplicationScoped
class JwtFilter : ContainerRequestFilter {

    @Inject
    lateinit var jwtService: JwtService

    @Inject
    lateinit var securityIdentity: SecurityIdentity

    override fun filter(requestContext: ContainerRequestContext) {
        val token = extractToken(requestContext)

        if (token != null && jwtService.isTokenNeedingRefresh(token)) {
            val username = securityIdentity.principal.name
            val newToken = jwtService.generateJwtToken(username)

            // 在響應頭中添加新的token
            requestContext.headers.add("New-Token", newToken)
        }
    }

    private fun extractToken(requestContext: ContainerRequestContext): String? {
        val authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else null
    }
}
