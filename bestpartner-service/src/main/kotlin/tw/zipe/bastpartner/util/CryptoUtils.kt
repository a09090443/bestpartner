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
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * @author Gary
 * @created 2024/10/24
 */
class CryptoUtils {
    companion object {
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

        // 使用公鑰加密
        fun encrypt(data: String, publicKey: PublicKey): String {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encryptedBytes = cipher.doFinal(data.toByteArray())
            return Base64.getEncoder().encodeToString(encryptedBytes)
        }

        // 使用私鑰解密
        fun decrypt(encryptedData: String, privateKey: PrivateKey): String {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
            return String(decryptedBytes)
        }

        // AES CBC 模式加密
        fun encryptAesCBC(data: String, key: String, iv: String? = null): Map<String, String> {
            // 確保金鑰長度為 16、24 或 32 位元組（對應 AES-128、AES-192、AES-256）
            val keyBytes = sha256(key).substring(0, 32).toByteArray()
            val secretKey = SecretKeySpec(keyBytes, "AES")

            // 生成或使用提供的 IV
            val ivBytes = if (iv != null) {
                Base64.getDecoder().decode(iv)
            } else {
                ByteArray(16).apply { SecureRandom().nextBytes(this) }
            }

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(ivBytes))

            val encryptedBytes = cipher.doFinal(data.toByteArray())
            return mapOf(
                "encrypted" to Base64.getEncoder().encodeToString(encryptedBytes),
                "iv" to Base64.getEncoder().encodeToString(ivBytes)
            )
        }

        // AES CBC 模式解密
        fun decryptAesCBC(encryptedData: String, key: String, iv: String): String {
            val keyBytes = sha256(key).substring(0, 32).toByteArray()
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivBytes = Base64.getDecoder().decode(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(ivBytes))

            val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
            return String(decryptedBytes)
        }

        // AES ECB 模式加密（注意：不推薦用於敏感數據）
        fun encryptAesECB(data: String, key: String): String {
            val keyBytes = sha256(key).substring(0, 32).toByteArray()
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val encryptedBytes = cipher.doFinal(data.toByteArray())
            return Base64.getEncoder().encodeToString(encryptedBytes)
        }

        // AES ECB 模式解密
        fun decryptAesECB(encryptedData: String, key: String): String {
            val keyBytes = sha256(key).substring(0, 32).toByteArray()
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
            return String(decryptedBytes)
        }

        // 生成隨機 AES 金鑰
        fun generateAesKey(keySize: Int = 256): String {
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
