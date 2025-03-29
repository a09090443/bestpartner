package tw.zipe.mcp.googledrive

import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * @author Gary
 * @created 2025/3/28
 */
class SSLUtil {
    companion object {
        private var originalFactory = HttpsURLConnection.getDefaultSSLSocketFactory()
        private var originalHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
        private var isDisabled = false

        /**
         * 僅用於開發和測試環境，禁用 SSL 驗證
         * 警告: 在生產環境中使用此方法會導致安全漏洞
         */
        fun disableSSLVerification() {
            if (isDisabled) return

            try {
                // 保存原始的安全設定
                originalFactory = HttpsURLConnection.getDefaultSSLSocketFactory()
                originalHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()

                // 創建一個信任所有憑證的 TrustManager
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                })

                // 安裝全局的 SSL 信任管理器
                val sc = SSLContext.getInstance("TLS")
                sc.init(null, trustAllCerts, java.security.SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

                // 禁用主機名驗證
                HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }

                isDisabled = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * 恢復正常的 SSL 證書驗證
         * 應在測試完成後或在生產環境中使用此方法
         */
        fun enableSSLVerification() {
            if (!isDisabled) return

            try {
                // 恢復預設的安全設定
                HttpsURLConnection.setDefaultSSLSocketFactory(originalFactory)
                HttpsURLConnection.setDefaultHostnameVerifier(originalHostnameVerifier)
                isDisabled = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
