package tw.zipe.basepartner.exception

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import tw.zipe.basepartner.dto.ApiResponse

/**
 * @author Gary
 * @created 2024/10/21
 */
@Provider
class GlobalExceptionMapper : ExceptionMapper<Exception> {
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
            else -> ApiResponse<Nothing>(
                code = 500,
                message = "Internal server error"
            )
        }

        return Response.status(response.code)
            .entity(objectMapper.writeValueAsString(response))
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
}
