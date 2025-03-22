package tw.zipe.bastpartner.exception

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.security.ForbiddenException
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import tw.zipe.bastpartner.dto.ApiResponse

/**
 * @author zipe1
 * @created 2025/3/22
 */
@Provider
@Priority(2) // 優先級高於GlobalExceptionMapper
class ForbiddenExceptionMapper : ExceptionMapper<ForbiddenException> {
    @Inject
    lateinit var objectMapper: ObjectMapper

    override fun toResponse(exception: ForbiddenException): Response {
        val response = ApiResponse<Nothing>(
            code = 403,
            message = "權限錯誤"
        )

        return Response.status(response.code)
            .entity(objectMapper.writeValueAsString(response))
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
}
