package tw.zipe.bastpartner.service

import io.quarkus.runtime.Startup
import io.smallrye.jwt.auth.principal.JWTParser
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jose4j.json.JsonUtil
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwx.JsonWebStructure
import tw.zipe.bastpartner.util.logger

/**
 * @author Gary
 * @created 2024/10/30
 */
@ApplicationScoped
class JwtService(
    private val parser: JWTParser
) {
    companion object {
        private const val ISSUER = "bast-partner"
        private const val TOKEN_VALIDITY_MINUTES = 30L
        private const val REFRESH_THRESHOLD_MINUTES = 5L
    }

    private val logger = logger()

    @ConfigProperty(name = "mp.jwt.verify.publickey.location")
    private lateinit var publicKeyLocation: String

    /**
     * 延遲載入 RSA 公鑰，用於 token 簽名驗證。
     * 公鑰路徑由 mp.jwt.verify.publickey.location 設定項決定，與 SmallRye JWT 使用相同設定。
     */
    private val rsaPublicKey: RSAPublicKey by lazy {
        val pem = JwtService::class.java.classLoader
            .getResourceAsStream(publicKeyLocation)
            ?.bufferedReader()?.readText()
            ?: throw IllegalStateException("Public key resource not found: $publicKeyLocation")
        val stripped = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        val decoded = Base64.getDecoder().decode(stripped)
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(decoded)) as RSAPublicKey
    }

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
            .groups(permissions)
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
     * 驗證 JWT 簽名是否有效（使用 RSA 公鑰，不檢查過期時間）。
     * 僅驗證 token 是否由本伺服器私鑰簽發，防止偽造 payload 的 token 觸發 refresh 流程。
     */
    fun isTokenSignatureValid(token: String): Boolean {
        return try {
            val joseObject = JsonWebStructure.fromCompactSerialization(token)
            if (joseObject is JsonWebSignature) {
                joseObject.key = rsaPublicKey
                joseObject.verifySignature()
            } else {
                false
            }
        } catch (e: Exception) {
            logger.debug("JWT signature verification failed: ${e.message}")
            false
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
