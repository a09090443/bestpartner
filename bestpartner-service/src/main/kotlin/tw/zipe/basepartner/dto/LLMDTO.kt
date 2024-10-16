package tw.zipe.basepartner.dto

import kotlinx.serialization.Serializable
import tw.zipe.basepartner.enumerate.ModelType

/**
 * @author Gary
 * @created 2024/10/16
 */
@Serializable
class LLMDTO : BaseDTO(){
    var id: String? = null
    var alias: String? = null
    var  modelType: ModelType = ModelType.CHAT
}

