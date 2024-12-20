package tw.zipe.bastpartner.exception

import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.jwt.build.JwtException
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import javax.naming.AuthenticationException
import org.hibernate.exception.ConstraintViolationException
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.util.logger

/**
 * @author Gary
 * @created 2024/10/21
 */
@Provider
@Priority(1)
class GlobalExceptionMapper : ExceptionMapper<Exception> {

    private val logger = logger()

    @Inject
    lateinit var objectMapper: ObjectMapper

    override fun toResponse(exception: Exception): Response {
        logger.error("Exception occurred: ${exception.javaClass.simpleName} - ${exception.message}", exception)
        val response = when (exception) {
            is NotFoundException -> ApiResponse<Nothing>(
                code = 404,
                message = "Resource not found"
            )
            is IllegalArgumentException -> ApiResponse<Nothing>(
                code = 400,
                message = exception.message ?: "Invalid request"
            )
            is ValidationException -> ApiResponse<Nothing>(
                code = 400,
                message = exception.message ?: "DTO validation failed"
            )
            is JwtValidationException -> ApiResponse<Nothing>(
                code = 401,
                message = exception.message ?: "Jwt validation failed"
            )
            is JwtException -> ApiResponse<Nothing>(
                code = 403,
                message = exception.message ?: "Jwt validation failed"
            )
            is ForbiddenException -> ApiResponse<Nothing>(
                code = 403,
                message = exception.message ?: "Forbidden"
            )
            is SecurityException -> {
                ApiResponse<Nothing>(
                    code = 403,
                    message = exception.message ?: "Security validation failed"
                )
            }
            is AccessDeniedException -> {
                ApiResponse<Nothing>(
                    code = 403,
                    message = exception.message ?: "Access denied"
                )
            }
            is AuthenticationException -> {
                ApiResponse<Nothing>(
                    code = 403,
                    message = exception.message ?: "Authentication failed"
                )
            }
            is ConstraintViolationException -> ApiResponse<Nothing>(
                code = 400,
                message = "Duplicate data"
            )
            else -> ApiResponse<Nothing>(
                code = 500,
                message = "Internal server error"
            )
        }
        val message = objectMapper.writeValueAsString(response)
        logger.error(message, exception)
        return Response.status(response.code)
            .entity(message)
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
}
