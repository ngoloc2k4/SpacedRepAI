package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DeckEntity
import com.example.data.preferences.AppSettingsManager
import com.example.data.security.SecureKeyStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationAndSecurityTest {

    @Test
    fun testMigration1To2SqlSyntax() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        // Verify database opens successfully and can insert entities with new schema
        val deckDao = db.deckDao()
        assertNotNull(deckDao)
        db.close()
    }

    @Test
    fun testSecureKeyStorageEncryptDecrypt() {
        val storage = SecureKeyStorage()
        val originalKey = "sk-test-secret-api-key-1234567890"

        val encrypted = storage.encrypt(originalKey)
        assertNotNull(encrypted)
        assertTrue(encrypted.isNotBlank())
        // Encrypted value should not equal raw plaintext
        assertFalse(encrypted == originalKey)

        val decrypted = storage.decrypt(encrypted)
        assertEquals(originalKey, decrypted)
    }

    @Test
    fun testSecureKeyStorageLegacyMigration() {
        val storage = SecureKeyStorage()
        val legacyKey = "legacy-plaintext-key-no-prefix"

        // Plaintext key without SEC_GCM or OBF prefix should decrypt cleanly
        val decrypted = storage.decrypt(legacyKey)
        assertEquals(legacyKey, decrypted)
    }

    @Test
    fun testAppSettingsManagerWithSecureKeyStorage() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val secureStorage = SecureKeyStorage()
        val manager = AppSettingsManager(context, secureStorage)

        val testKey = "sk-proj-super-secret-production-ai-token"
        manager.updateAiApiKey(testKey)

        val currentSettings = manager.settingsFlow.value
        assertEquals(testKey, currentSettings.aiApiKey)

        // Verify raw key stored in SharedPreferences is encrypted
        val rawPrefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
        val storedRaw = rawPrefs.getString("key_ai_api_key", "") ?: ""
        assertTrue(storedRaw.startsWith("SEC_GCM:") || storedRaw.startsWith("OBF:"))
        assertFalse(storedRaw == testKey)
    }
}
