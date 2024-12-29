package tw.zipe.bastpartner.dto

import io.netty.util.internal.StringUtil

/**
 * @author Gary
 * @created 2024/12/28
 */
class LLMSettingDTO {
    val id: String = StringUtil.EMPTY_STRING
    val userId: String = StringUtil.EMPTY_STRING
    val platformId: String = StringUtil.EMPTY_STRING
    val type: String = StringUtil.EMPTY_STRING
    val alias: String = StringUtil.EMPTY_STRING
    val modelSetting: String = StringUtil.EMPTY_STRING
    val platformName: String = StringUtil.EMPTY_STRING
}
