package tw.zipe.basepartner.exception

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import tw.zipe.basepartner.dto.ApiResponse
import tw.zipe.basepartner.util.logger

/**
 * @author Gary
 * @created 2024/10/21
 */
@Provider
class GlobalExceptionMapper : ExceptionMapper<Exception> {

    private val logger = logger()

    @Inject
    lateinit var objectMapper: ObjectMapper

    override fun toResponse(exception: Exception): Response {
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
            else -> ApiResponse<Nothing>(
                code = 500,
                message = "Internal server error"
            )
        }
        val message = objectMapper.writeValueAsString(response)
        logger.error(message)
        return Response.status(response.code)
            .entity(message)
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
}
