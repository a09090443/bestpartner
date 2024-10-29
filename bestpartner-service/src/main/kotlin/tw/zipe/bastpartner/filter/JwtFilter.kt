package tw.zipe.bastpartner.filter

import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.jwt.JsonWebToken

/**
 * @author Gary
 * @created 2024/10/25
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
class JwtFilter : ContainerRequestFilter {

    @Inject
    lateinit var jwt:JsonWebToken

    override fun filter(requestContext: ContainerRequestContext?) {
        if (jwt.getRawToken() == null || jwt.getClaimNames() == null) {
            println("jwt is null")
//            requestContext?.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}
