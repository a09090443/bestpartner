package tw.zipe.bastpartner.enumerate

import tw.zipe.bastpartner.util.MessageUtil

/**
 * @author Gary
 * @created 2024/10/25
 */
enum class UserStatus {
    INACTIVE,
    ACTIVE;

    companion object {
        /**
         * 根據 ordinal 值返回對應的 UserStatus。
         * 如果 ordinal 無效，則拋出 IllegalArgumentException。
         */
        fun fromOrdinal(ordinal: Int): UserStatus {
            return entries.getOrNull(ordinal)
                ?: throw IllegalArgumentException(MessageUtil.get(AppMessage.SYSTEM_USER_STATUS_INVALID, ordinal))
        }
    }
}
