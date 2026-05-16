package tw.zipe.bastpartner.exception

/**
 * @author Gary
 * @created 2025/4/25
 */
class LLMException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
