package tw.zipe.bastpartner.exception

import io.smallrye.jwt.build.JwtException

/**
 * @author Gary
 * @created 2024/10/31
 */
class JwtValidationException(message: String) : JwtException(message)
