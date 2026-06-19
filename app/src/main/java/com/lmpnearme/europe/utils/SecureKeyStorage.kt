package com.lmpnearme.europe.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureKeyStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "lmp_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API, key.trim()).apply()
    }

    fun getApiKey(): String? = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API).apply()
    }

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    fun savePreferredZone(eicCode: String) {
        prefs.edit().putString(KEY_ZONE, eicCode).apply()
    }

    fun getPreferredZone(): String? = prefs.getString(KEY_ZONE, null)

    companion object {
        private const val KEY_API = "entso_api_key"
        private const val KEY_ZONE = "preferred_zone"
    }
}
