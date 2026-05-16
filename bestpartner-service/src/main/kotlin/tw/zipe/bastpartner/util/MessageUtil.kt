package tw.zipe.bastpartner.util

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle
import tw.zipe.bastpartner.enumerate.AppMessage

object MessageUtil {

    private const val BUNDLE_BASE_NAME = "messages/messages"

    private val localeContext: ThreadLocal<Locale> = ThreadLocal.withInitial { Locale.US }

    fun setLocale(locale: Locale) = localeContext.set(locale)
    fun clearLocale() = localeContext.remove()

    private fun bundle(): ResourceBundle =
        ResourceBundle.getBundle(BUNDLE_BASE_NAME, localeContext.get())

    fun get(message: AppMessage): String =
        runCatching { bundle().getString(message.key) }.getOrDefault(message.key)

    fun get(message: AppMessage, vararg args: Any?): String {
        val template = runCatching { bundle().getString(message.key) }.getOrDefault(message.key)
        return if (args.isEmpty()) template else MessageFormat.format(template, *args)
    }
}

fun AppMessage.msg(): String = MessageUtil.get(this)
fun AppMessage.msg(vararg args: Any?): String = MessageUtil.get(this, *args)
