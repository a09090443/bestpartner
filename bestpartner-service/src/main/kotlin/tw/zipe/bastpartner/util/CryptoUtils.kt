package tw.zipe.bastpartner.util

import java.io.FileInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * @author Gary
 * @created 2024/10/24
 */
class CryptoUtils {
    companion object {
        private const val AES_KEY_SIZE = 256 // 預設 AES 金鑰大小（256 位元）
        private const val GCM_IV_LENGTH = 12 // GCM 建議使用 12 位元組 IV
        private const val GCM_TAG_LENGTH = 16 // GCM 標籤長度（位元組）

        // SHA-512 雜湊
        fun sha512(input: String): String {
            val messageDigest = MessageDigest.getInstance("SHA-512")
            val bytes = messageDigest.digest(input.toByteArray())
            return bytes.fold("") { str, it -> str + "%02x".format(it) }
        }

        // SHA-256 雜湊
        fun sha256(input: String): String {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            val bytes = messageDigest.digest(input.toByteArray())
            return bytes.fold("") { str, it -> str + "%02x".format(it) }
        }

        // RSA encryption/decryption with OAEP padding
        fun rsaEncrypt(data: String, publicKey: PublicKey): String {
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC") // 强制使用 BouncyCastle 提供者
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.toByteArray()))
        }

        fun rsaDecrypt(encryptedData: String, privateKey: PrivateKey): String {
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC") // 强制使用 BouncyCastle 提供者
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            return String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)))
        }

        // AES-GCM 模式加密
        fun encryptAesGCM(data: String, key: String): Map<String, String> {
            val keyBytes = sha256(key).substring(0, 32).toByteArray()
            val secretKey = SecretKeySpec(keyBytes, "AES")

            // 生成隨機 IV
            val ivBytes = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(ivBytes)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH * 8, ivBytes))

            val encryptedBytes = cipher.doFinal(data.toByteArray())
            return mapOf(
                "encrypted" to Base64.getEncoder().encodeToString(encryptedBytes),
                "iv" to Base64.getEncoder().encodeToString(ivBytes)
            )
        }

        // AES-GCM 模式解密
        fun decryptAesGCM(encryptedData: String, key: String, iv: String): String {
            val keyBytes = sha256(key).substring(0, 32).toByteArray()
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivBytes = Base64.getDecoder().decode(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH * 8, ivBytes))

            val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
            return String(decryptedBytes)
        }

        // 生成隨機 AES 金鑰
        fun generateAesKey(keySize: Int = AES_KEY_SIZE): String {
            require(keySize in listOf(128, 192, 256)) { "Key size must be 128, 192, or 256 bits" }
            val keyBytes = ByteArray(keySize / 8)
            SecureRandom().nextBytes(keyBytes)
            return Base64.getEncoder().encodeToString(keyBytes)
        }

        // 從憑證檔案載入公鑰
        fun loadPublicKeyFromCertificate(certificatePath: String): PublicKey {
            val certificateFile = FileInputStream(certificatePath)
            val certificate = java.security.cert.CertificateFactory.getInstance("X.509")
                .generateCertificate(certificateFile) as X509Certificate
            return certificate.publicKey
        }

        // 從 PKCS12 格式的憑證檔案載入私鑰
        fun loadPrivateKeyFromPKCS12(
            pkcs12Path: String, password: String, alias: String
        ): PrivateKey {
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(FileInputStream(pkcs12Path), password.toCharArray())
            return keyStore.getKey(alias, password.toCharArray()) as PrivateKey
        }

        // 從 PEM 格式的公鑰檔案載入公鑰
        fun loadPublicKeyFromPEM(publicKeyPEM: String): PublicKey {
            val publicKeyContent =
                publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")
                    .replace("\n", "")

            val keyBytes = Base64.getDecoder().decode(publicKeyContent)
            val keySpec = X509EncodedKeySpec(keyBytes)
            return KeyFactory.getInstance("RSA").generatePublic(keySpec)
        }

        // 從 PEM 格式的私鑰檔案載入私鑰
        fun loadPrivateKeyFromPEM(privateKeyPEM: String): PrivateKey {
            val privateKeyContent =
                privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
                    .replace("\n", "")

            val keyBytes = Base64.getDecoder().decode(privateKeyContent)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
        }
    }
}
