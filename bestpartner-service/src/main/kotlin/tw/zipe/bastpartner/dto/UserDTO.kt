package tw.zipe.bastpartner.dto

import io.netty.util.internal.StringUtil
import kotlinx.serialization.Serializable
import tw.zipe.bastpartner.enumerate.UserStatus

/**
 * @author Gary
 * @created 2024/10/22
 */
@Serializable
class UserDTO(
    var id: String? = null,
    var username: String = StringUtil.EMPTY_STRING,
    var password: String? = null,
    var nickname: String? = null,
    var phone: String? = null,
    var email: String? = null,
    var avatar: String? = null,
    var status: Int? = null,
    var role: UserRole? = null
)
