package tw.zipe.bastpartner.exception

import tw.zipe.bastpartner.enumerate.AppMessage
import tw.zipe.bastpartner.util.MessageUtil

/**
 * @author Gary
 * @created 2024/10/18
 */
class ServiceException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(appMessage: AppMessage) : super(MessageUtil.get(appMessage))
    constructor(appMessage: AppMessage, vararg args: Any?) : super(MessageUtil.get(appMessage, *args))
}
