package tw.zipe.bastpartner.service

import io.smallrye.jwt.auth.principal.JWTParser
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.time.temporal.ChronoUnit
import tw.zipe.bastpartner.enumerate.UserStatus
import tw.zipe.bastpartner.repository.LLMUserRepository

/**
 * @author Gary
 * @created 2024/10/30
 */
@ApplicationScoped
class JwtService(
    val parser: JWTParser,
    val llmUserRepository: LLMUserRepository
) {
    companion object {
        private const val ISSUER = "bast-partner"
        private const val TOKEN_VALIDITY_MINUTES = 10L
        private const val REFRESH_THRESHOLD_MINUTES = 5L
    }

    /**
     * 生成 JWT 權杖
     */
    fun generateJwtToken(userId: String): String {
        val expirationTime = Instant.now().plus(TOKEN_VALIDITY_MINUTES, ChronoUnit.MINUTES)
        val permissions =
            llmUserRepository.findUserPermissionByStatus(userId, UserStatus.ACTIVE).map { it.name }.toSet()

        return Jwt.issuer(ISSUER)
            .upn(userId)
            .groups(permissions)
            .issuedAt(Instant.now())
            .expiresAt(expirationTime)
            .sign();
    }

    /**
     * 檢查 JWT 是否需要重新生成
     */
    fun isTokenNeedingRefresh(token: String): Boolean {
        try {
            val jwt = parser.parse(token)
            val expiration = Instant.ofEpochSecond(jwt.expirationTime)
            val refreshThreshold = Instant.now().plus(REFRESH_THRESHOLD_MINUTES, ChronoUnit.MINUTES)

            return expiration.isBefore(refreshThreshold)
        } catch (e: Exception) {
            return true
        }
    }
}
