package tw.zipe.bastpartner.dto

import kotlinx.serialization.Serializable
import org.apache.http.HttpStatus

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
            ApiResponse(code = HttpStatus.SC_OK, message = "success", data = data)

        fun <T> error(message: String, code: Int = HttpStatus.SC_INTERNAL_SERVER_ERROR, data: String? = null): ApiResponse<T> =
            ApiResponse(code = code, message = message)
    }
}
