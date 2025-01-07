package tw.zipe.bastpartner.config.security

import io.quarkus.runtime.Startup
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

@Singleton
@Startup
class SecurityConfig {

    @PostConstruct
    fun registerBouncyCastleProvider() {
        // 檢查 BouncyCastle 是否已註冊
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
            println("BouncyCastle provider registered.")
        } else {
            println("BouncyCastle provider is already registered.")
        }
    }
}
