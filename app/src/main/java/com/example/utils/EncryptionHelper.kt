package com.example.utils

import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val MASTER_SECRET = "SecureFinanceMasterKey2026!@#"

    private fun getKey(userId: String?): SecretKeySpec {
        val resolvedUid = userId ?: try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }
        val seed = if (!resolvedUid.isNullOrBlank()) resolvedUid + MASTER_SECRET else MASTER_SECRET
        val digest = MessageDigest.getInstance("SHA-256")
        val fullHash = digest.digest(seed.toByteArray(StandardCharsets.UTF_8))
        val keyBytes = fullHash.copyOf(16) // 16 bytes for AES-128
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plainText: String?): String = encrypt(null, plainText)

    fun encrypt(userId: String?, plainText: String?): String {
        if (plainText.isNullOrEmpty()) return plainText ?: ""
        try {
            val secretKey = getKey(userId)
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

            // Combine IV + EncryptedBytes
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            return plainText ?: ""
        }
    }

    fun decrypt(cipherText: String?): String = decrypt(null, cipherText)

    fun decrypt(userId: String?, cipherText: String?): String {
        if (cipherText.isNullOrEmpty()) return cipherText ?: ""
        try {
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)
            if (combined.size <= 16) {
                return cipherText
            }

            val iv = ByteArray(16)
            val encryptedBytes = ByteArray(combined.size - 16)
            System.arraycopy(combined, 0, iv, 0, 16)
            System.arraycopy(combined, 16, encryptedBytes, 0, encryptedBytes.size)

            val secretKey = getKey(userId)
            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            return cipherText
        }
    }
}
