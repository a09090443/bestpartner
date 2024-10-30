package tw.zipe.bastpartner.dto

import kong.unirest.HttpStatus
import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/21
 */
@Serializable
data class ApiResponse<T>(
    val code: Int = 200,
    val message: String = "",
    val data: T? = null
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(code = HttpStatus.OK, message = "success", data = data)

        fun <T> error(message: String, code: Int = HttpStatus.INTERNAL_SERVER_ERROR): ApiResponse<T> =
            ApiResponse(code = code, message = message)
    }
}
