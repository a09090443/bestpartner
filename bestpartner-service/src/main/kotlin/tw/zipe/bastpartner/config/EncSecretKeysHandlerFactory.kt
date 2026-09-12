package tw.zipe.bastpartner.config

import io.smallrye.config.crypto.AESGCMNoPaddingSecretKeysHandlerFactory

/**
 * 設定檔機密值解密 handler：把 SmallRye 內建的 `aes-gcm-nopadding` 換成短名 `enc`。
 *
 * 設定值因此可寫成 `${enc::<密文>}` 而非冗長的 `${aes-gcm-nopadding::<密文>}`，
 * 讓 .env.<profile> 好讀。
 *
 * 僅覆寫名稱，其餘（金鑰讀取、lazy 初始化、AES-GCM 解密、密文格式）
 * 全部沿用父類別，因此密文完全相容 —— 同一份密文兩種前綴都能解。
 *
 * ⚠️ 金鑰設定鍵沿用父類別硬編碼的
 *    `smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key`（無法覆寫），
 *    與此處的短名不一致屬預期；該鍵在 application.properties 只出現一次，
 *    且已包裝為 CONFIG_ENCRYPTION_KEY 環境變數，日常維運不會直接接觸。
 *
 * 透過 META-INF/services/io.smallrye.config.SecretKeysHandlerFactory 以 ServiceLoader 註冊。
 *
 * @author Gary
 */
class EncSecretKeysHandlerFactory : AESGCMNoPaddingSecretKeysHandlerFactory() {

    override fun getName(): String = HANDLER_NAME

    companion object {
        /** config expression 前綴：`${enc::<密文>}` */
        const val HANDLER_NAME = "enc"
    }
}
