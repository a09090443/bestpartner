package tw.zipe.basepartner.dto

import io.netty.util.internal.StringUtil

/**
 * @author Gary
 * @created 2024/10/22
 */
class UserDTO(
    var id: String? = null,
    var username: String = StringUtil.EMPTY_STRING,
    var password: String = StringUtil.EMPTY_STRING,
    var nickname: String? = null,
    var phone: String? = null,
    var email: String? = null,
    var avatar: String? = null,
    var status: String = StringUtil.EMPTY_STRING
)
