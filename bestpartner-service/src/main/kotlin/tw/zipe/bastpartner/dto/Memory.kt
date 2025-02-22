package tw.zipe.bastpartner.dto

import io.netty.util.internal.StringUtil
import kotlinx.serialization.Serializable

/**
 * @author Gary
 * @created 2024/10/15
 */
@Serializable
class Memory(
    val id: String = StringUtil.EMPTY_STRING,
    val maxSize: Int = 10
)
