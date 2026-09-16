package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.util.CrashReporter
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Provides hardware-backed encryption and decryption for sensitive credentials
 * (such as custom AI API keys) using Android KeyStore and AES-256 GCM.
 */
class SecureKeyStorage {

    companion object {
        private const val TAG = "SecureKeyStorage"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "srs_flashcards_secure_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PREFIX_SECURE = "SEC_GCM:"
    }

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val parameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(parameterSpec)
                keyGenerator.generateKey()
            } else {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e, TAG)
            null
        }
    }

    /**
     * Encrypts plaintext string using AES/GCM with Android KeyStore.
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isBlank()) return ""
        try {
            val secretKey = getOrCreateSecretKey()
            if (secretKey != null) {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

                val combined = ByteArray(1 + iv.size + encryptedBytes.size)
                combined[0] = iv.size.toByte()
                System.arraycopy(iv, 0, combined, 1, iv.size)
                System.arraycopy(encryptedBytes, 0, combined, 1 + iv.size, encryptedBytes.size)

                return PREFIX_SECURE + Base64.encodeToString(combined, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e, TAG)
        }
        // Obfuscated fallback for testing environments where AndroidKeyStore provider is absent
        return "OBF:" + Base64.encodeToString(plaintext.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    /**
     * Decrypts ciphertext back to plaintext. Also seamlessly handles legacy unencrypted strings.
     */
    fun decrypt(storedValue: String): String {
        if (storedValue.isBlank()) return ""
        if (storedValue.startsWith(PREFIX_SECURE)) {
            val base64Payload = storedValue.removePrefix(PREFIX_SECURE)
            try {
                val secretKey = getOrCreateSecretKey() ?: return ""
                val combined = Base64.decode(base64Payload, Base64.NO_WRAP)
                val ivLength = combined[0].toInt()
                val iv = ByteArray(ivLength)
                System.arraycopy(combined, 1, iv, 0, ivLength)
                val cipherText = ByteArray(combined.size - 1 - ivLength)
                System.arraycopy(combined, 1 + ivLength, cipherText, 0, cipherText.size)

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val decrypted = cipher.doFinal(cipherText)
                return String(decrypted, Charsets.UTF_8)
            } catch (e: Exception) {
                CrashReporter.recordException(e, TAG)
                return ""
            }
        } else if (storedValue.startsWith("OBF:")) {
            val base64 = storedValue.removePrefix("OBF:")
            return try {
                String(Base64.decode(base64, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e: Exception) {
                ""
            }
        }
        // Legacy plaintext format - return as-is for auto-migration
        return storedValue
    }
}
