package tw.zipe.bastpartner.exception

/**
 * @author Gary
 * @created 2024/10/18
 */
class ServiceException(message: String) : RuntimeException(message)
{
    constructor(message: String, cause: Throwable) : this(message)
}
