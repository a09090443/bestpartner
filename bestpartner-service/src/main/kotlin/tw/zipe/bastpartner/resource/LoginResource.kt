package tw.zipe.bastpartner.resource

import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.security.PermitAll
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.SecurityContext
import tw.zipe.bastpartner.dto.UserDTO
import tw.zipe.bastpartner.service.LLMUserService
import tw.zipe.bastpartner.util.DTOValidator


/**
 * @author Gary
 * @created 2024/10/24
 */
@Path("/login")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LoginResource(
    val llmUserService: LLMUserService,
    val identity: SecurityIdentity
) {

    @POST
    @Path("/")
    @PermitAll
    fun login(userDTO: UserDTO): String {
        DTOValidator.validate(userDTO) {
            requireNotEmpty("username")
            requireNotEmpty("password")
            throwOnInvalid()
        }
//        llmUserService.getUser(userDTO.username)
        val token = llmUserService.generateJwtToken(userDTO)
        println(token)
        return token
    }

    @POST
    @Path("/check")
//    @RolesAllowed("VIEW_ADMIN_DETAILS")
    fun check(@Context ctx: SecurityContext): String {
        println(identity.principal.name)
        println(ctx.userPrincipal)
        return "check"
    }
}
