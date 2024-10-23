package tw.zipe.bastpartner.exception

/**
 * @author Gary
 * @created 2024/10/22
 */
class ValidationException(
    private val errors: List<String>
) : RuntimeException(errors.joinToString("\n")) {
    override fun toString(): String {
        return "ValidationException: \n${errors.joinToString("\n")}"
    }
}

