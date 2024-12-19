package tw.zipe.bastpartner.resource

import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.security.PermitAll
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.SecurityContext
import kong.unirest.HttpStatus
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.dto.UserDTO
import tw.zipe.bastpartner.service.JwtService
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
    val jwtService: JwtService,
    val identity: SecurityIdentity
) {

    @POST
    @Path("/")
    @PermitAll
    fun login(userDTO: UserDTO): ApiResponse<String> {
        DTOValidator.validate(userDTO) {
            requireNotEmpty("email")
            requireNotEmpty("password")
            throwOnInvalid()
        }
        val loginResult = llmUserService.loginVerification(userDTO.email.orEmpty(), userDTO.password)
            ?: return ApiResponse.error("登入帳號或密碼錯誤", HttpStatus.UNAUTHORIZED)
        return loginResult.let {
            val token = jwtService.generateJwtToken(it, setOf())
            ApiResponse.success(token)
        }
    }

    @POST
    @Path("/check")
    @RolesAllowed("user-write")
//    @Authenticated
    fun check(@Context ctx: SecurityContext): String {
        println(identity.principal.name)
        println(ctx.userPrincipal)
        return "check"
    }
}
