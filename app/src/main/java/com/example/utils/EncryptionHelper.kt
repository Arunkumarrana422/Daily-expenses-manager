package com.example.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets

object EncryptionHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val SECRET_KEY = "FinanceAppSecureK" // 16 bytes for AES-128
    private const val FIXED_IV = "FinAppSecureIV16"   // 16 bytes IV

    fun encrypt(plainText: String?): String {
        if (plainText.isNullOrEmpty()) return plainText ?: ""
        try {
            val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(StandardCharsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(FIXED_IV.toByteArray(StandardCharsets.UTF_8))
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            return plainText
        }
    }

    fun decrypt(cipherText: String?): String {
        if (cipherText.isNullOrEmpty()) return cipherText ?: ""
        try {
            val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(StandardCharsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(FIXED_IV.toByteArray(StandardCharsets.UTF_8))
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // If it wasn't encrypted or failed to decrypt (e.g. plain text), return as is
            return cipherText
        }
    }
}
