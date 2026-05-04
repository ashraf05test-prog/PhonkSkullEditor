package com.phonkskull.editor.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

class SecureSettingsStore(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "phonkskull_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    private val gson = Gson()

    fun load(): SecureSettings {
        val json = prefs.getString("settings", null) ?: return SecureSettings()
        return try { gson.fromJson(json, SecureSettings::class.java) } catch (e: Exception) { SecureSettings() }
    }

    fun save(s: SecureSettings) {
        prefs.edit().putString("settings", gson.toJson(s)).apply()
    }
}
