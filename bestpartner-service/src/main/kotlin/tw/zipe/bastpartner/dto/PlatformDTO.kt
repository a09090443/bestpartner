package tw.zipe.bastpartner.dto

import kotlinx.serialization.Serializable
import tw.zipe.bastpartner.enumerate.Platform

@Serializable
class PlatformDTO {
    var id: String? = null
    var platform: Platform? = null
}
