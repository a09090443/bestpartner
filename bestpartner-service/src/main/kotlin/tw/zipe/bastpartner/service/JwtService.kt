package tw.zipe.bastpartner.service

import io.quarkus.runtime.Startup
import io.smallrye.jwt.auth.principal.JWTParser
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.jose4j.json.JsonUtil
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwx.JsonWebStructure
import tw.zipe.bastpartner.enumerate.UserStatus
import tw.zipe.bastpartner.repository.LLMPermissionRepository
import tw.zipe.bastpartner.util.logger

/**
 * @author Gary
 * @created 2024/10/30
 */
@ApplicationScoped
class JwtService(
    private val parser: JWTParser,
    private val llmPermissionRepository: LLMPermissionRepository
) {
    companion object {
        private const val ISSUER = "bast-partner"
        private const val TOKEN_VALIDITY_MINUTES = 10L
        private const val REFRESH_THRESHOLD_MINUTES = 5L
    }

    private val logger = logger()

    @Startup
    fun start() {
        logger.info("JwtService start")
    }

    /**
     * 生成 JWT 權杖
     */
    fun generateJwtToken(userId: String, permissions: Set<String?>): String {
        val expirationTime = Instant.now().plus(TOKEN_VALIDITY_MINUTES, ChronoUnit.MINUTES)
        return Jwt.issuer(ISSUER)
            .upn(userId)
            .groups(permissions.ifEmpty {
                llmPermissionRepository.findUserPermissionByStatus(userId, UserStatus.ACTIVE).map { it.name }.toSet()
            })
            .issuedAt(Instant.now())
            .expiresAt(expirationTime)
            .sign();
    }

    /**
     * 檢查 JWT 是否已過期
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            val jwt = parser.parse(token)
            val expiration = Instant.ofEpochSecond(jwt.expirationTime)
            expiration.isBefore(Instant.now())
        } catch (e: Exception) {
            true
        }
    }

    /**
     * 檢查 JWT 是否需要重新生成（過期前5分鐘）
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

    fun getTokenPayload(token: String): Map<String, Any>? {
        val joseObject = JsonWebStructure.fromCompactSerialization(token)
        val payload: String
        if (joseObject is JsonWebSignature) {
            payload = joseObject.unverifiedPayload
            return JsonUtil.parseJson(payload)
        }
        return null
    }
}
