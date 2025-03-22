package tw.zipe.bastpartner.config.security

import io.quarkus.runtime.Startup
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import tw.zipe.bastpartner.util.logger

@Singleton
@Startup
class SecurityConfig {

    private val logger = logger()

    @PostConstruct
    fun registerBouncyCastleProvider() {
        // 檢查 BouncyCastle 是否已註冊
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
            logger.info("BouncyCastle provider registered.")
        } else {
            logger.info("BouncyCastle provider is already registered.")
        }
    }
}
