package com.centwise.features.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val titleEnglish: String, val titleNative: String, val flag: String) {
    ENGLISH("en", "English", "English", "🇺🇸"),
    BENGALI("bn", "Bengali", "বাংলা", "🇧🇩")
}

object LanguagePrefs {
    private const val PREFS_NAME = "centwise_language_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    var selectedLanguage by mutableStateOf(AppLanguage.ENGLISH)
        private set

    val isBengali: Boolean
        get() = selectedLanguage == AppLanguage.BENGALI

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
        selectedLanguage = if (code == AppLanguage.BENGALI.code) AppLanguage.BENGALI else AppLanguage.ENGLISH
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        selectedLanguage = language
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
    }
}
