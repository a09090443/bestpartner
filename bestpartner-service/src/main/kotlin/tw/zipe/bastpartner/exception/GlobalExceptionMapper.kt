package tw.zipe.bastpartner.exception

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.security.ForbiddenException
import io.quarkus.security.UnauthorizedException
import io.smallrye.jwt.build.JwtException
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.transaction.RollbackException
import jakarta.ws.rs.NotAllowedException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import javax.naming.AuthenticationException
import kotlin.io.AccessDeniedException
import org.apache.http.HttpStatus
import org.hibernate.exception.ConstraintViolationException
import tw.zipe.bastpartner.dto.ApiResponse
import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.exception.LLMException
import tw.zipe.bastpartner.util.MessageUtil
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
            is ServiceException -> ApiResponse<Nothing>(
                code = 400,
                message = exception.message ?: MessageUtil.get(AppMessage.HTTP_SERVICE_EXCEPTION)
            )
            is LLMException -> ApiResponse<Nothing>(
                code = 400,
                message = exception.message ?: MessageUtil.get(AppMessage.HTTP_LLM_SERVICE_ERROR)
            )
            // 保留原有的異常處理
            is NotFoundException -> ApiResponse<Nothing>(
                code = 404,
                message = MessageUtil.get(AppMessage.HTTP_NOT_FOUND)
            )
            is IllegalArgumentException -> ApiResponse<Nothing>(
                code = 400,
                message = exception.message ?: MessageUtil.get(AppMessage.HTTP_INVALID_REQUEST)
            )
            is ValidationException -> ApiResponse<Nothing>(
                code = 400,
                message = exception.message ?: MessageUtil.get(AppMessage.HTTP_VALIDATION_FAILED)
            )
            is JwtValidationException -> {
                ApiResponse<String>(
                    code = 401,
                    message = exception.message ?: MessageUtil.get(AppMessage.HTTP_JWT_VALIDATION_FAILED),
                    data = exception.newToken
                )
            }
            is JwtException -> ApiResponse<Nothing>(
                code = 403,
                message = exception.message ?: MessageUtil.get(AppMessage.HTTP_JWT_VALIDATION_FAILED)
            )
            is ForbiddenException -> ApiResponse<Nothing>(
                code = 403,
                message = exception.message ?: MessageUtil.get(AppMessage.HTTP_FORBIDDEN)
            )
            // 新增更詳細的權限相關異常處理
            // 僅 io.quarkus.security 的 UnauthorizedException / ForbiddenException 繼承 SecurityException，
            // 後者已於上方先行攔截；其餘 SecurityException 一律走 else。
            is SecurityException -> {
                ApiResponse<Nothing>(
                    code = 403,
                    message = when (exception) {
                        is UnauthorizedException -> MessageUtil.get(AppMessage.HTTP_AUTHENTICATION_FAILED)
                        else -> exception.message ?: MessageUtil.get(AppMessage.HTTP_SECURITY_VALIDATION_FAILED)
                    }
                )
            }
            // ⚠️ 此處為 kotlin.io.AccessDeniedException（檔案系統存取失敗，繼承 IOException），
            // 並非授權失敗——Kotlin 預設匯入 kotlin.io.* 使裸寫的 AccessDeniedException 解析至此。
            // 已改為顯式 import 避免誤讀；授權失敗一律由上方的 Forbidden / Security 分支處理。
            is AccessDeniedException -> {
                ApiResponse<Nothing>(
                    code = 403,
                    message = exception.message ?: MessageUtil.get(AppMessage.HTTP_ACCESS_DENIED)
                )
            }
            is AuthenticationException -> {
                ApiResponse<Nothing>(
                    code = HttpStatus.SC_UNAUTHORIZED,
                    message = exception.message ?: MessageUtil.get(AppMessage.HTTP_AUTHENTICATION_FAILED)
                )
            }
            is ConstraintViolationException -> ApiResponse<Nothing>(
                code = 400,
                message = MessageUtil.get(AppMessage.HTTP_DUPLICATE_DATA)
            )
            is RollbackException -> ApiResponse<Nothing>(
                code = 400,
                message = MessageUtil.get(AppMessage.HTTP_DATABASE_ERROR)
            )
            is NotAllowedException -> ApiResponse<Nothing>(
                code = 405,
                message = MessageUtil.get(AppMessage.HTTP_METHOD_NOT_ALLOWED)
            )
            is WebApplicationException -> {
                val status = exception.response.status
                ApiResponse<Nothing>(
                    code = 403,
                    message = when (status) {
                        403 -> MessageUtil.get(AppMessage.HTTP_FORBIDDEN)
                        401 -> MessageUtil.get(AppMessage.HTTP_UNAUTHORIZED)
                        404 -> MessageUtil.get(AppMessage.HTTP_NOT_FOUND)
                        else -> MessageUtil.get(AppMessage.HTTP_WEB_APP_EXCEPTION)
                    }
                )
            }
            else -> ApiResponse<Nothing>(
                code = 500,
                message = MessageUtil.get(AppMessage.HTTP_INTERNAL_SERVER_ERROR)
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
