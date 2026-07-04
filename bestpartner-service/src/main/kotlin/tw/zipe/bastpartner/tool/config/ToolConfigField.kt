package tw.zipe.bastpartner.tool.config

/**
 * 標註於工具 config data class 的建構子參數，補充 settingSchema 的 UI 提示。
 * required 與型別由反射自動推導（nullable -> 選填），此 annotation 僅補人工無法推導的資訊。
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ToolConfigField(
    val description: String = "",
    val sensitive: Boolean = false
)
