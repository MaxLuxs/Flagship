package io.maxluxs.flagship.sample

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

actual object ProviderPreferences {
    private const val PREFS_NAME = "flagship_provider_prefs"
    private const val KEY_PROVIDER = "selected_provider"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun saveSelectedProvider(type: ProviderType) {
        prefs.edit { putString(KEY_PROVIDER, type.name) }
    }

    actual fun getSelectedProvider(): ProviderType {
        val saved = prefs.getString(KEY_PROVIDER, ProviderType.MOCK.name)
        return try {
            ProviderType.valueOf(saved ?: ProviderType.MOCK.name)
        } catch (_: Exception) {
            ProviderType.MOCK
        }
    }

    actual fun clearProvider() {
        prefs.edit { remove(KEY_PROVIDER) }
    }
}

