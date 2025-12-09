/*
 Copyright 2025 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.messagingsample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Helper class for managing app locale changes.
 * 
 * This allows testing of the ML Kit translation feature by changing the device locale
 * without needing to go into system settings.
 */
object LocaleHelper {
    
    private const val SELECTED_LANGUAGE_PREF = "selected_language"
    private const val PREFS_NAME = "locale_prefs"
    
    /**
     * Supported test locales for translation testing.
     * All languages supported by ML Kit Translation API.
     * See: https://developers.google.com/ml-kit/language/translation/translation-language-support
     */
    enum class TestLocale(val displayName: String, val languageCode: String) {
        AFRIKAANS("Afrikaans", "af"),
        ALBANIAN("Albanian", "sq"),
        ARABIC("Arabic (العربية)", "ar"),
        BELARUSIAN("Belarusian", "be"),
        BENGALI("Bengali (বাংলা)", "bn"),
        BULGARIAN("Bulgarian", "bg"),
        CATALAN("Catalan", "ca"),
        CHINESE("Chinese (中文)", "zh"),
        CROATIAN("Croatian", "hr"),
        CZECH("Czech", "cs"),
        DANISH("Danish", "da"),
        DUTCH("Dutch", "nl"),
        ENGLISH("English", "en"),
        ESPERANTO("Esperanto", "eo"),
        ESTONIAN("Estonian", "et"),
        FINNISH("Finnish", "fi"),
        FRENCH("French (Français)", "fr"),
        GALICIAN("Galician", "gl"),
        GEORGIAN("Georgian", "ka"),
        GERMAN("German (Deutsch)", "de"),
        GREEK("Greek", "el"),
        GUJARATI("Gujarati (ગુજરાતી)", "gu"),
        HAITIAN("Haitian", "ht"),
        HEBREW("Hebrew (עברית)", "he"),
        HINDI("Hindi (हिंदी)", "hi"),
        HUNGARIAN("Hungarian", "hu"),
        ICELANDIC("Icelandic", "is"),
        INDONESIAN("Indonesian", "id"),
        IRISH("Irish", "ga"),
        ITALIAN("Italian (Italiano)", "it"),
        JAPANESE("Japanese (日本語)", "ja"),
        KANNADA("Kannada (ಕನ್ನಡ)", "kn"),
        KOREAN("Korean (한국어)", "ko"),
        LATVIAN("Latvian", "lv"),
        LITHUANIAN("Lithuanian", "lt"),
        MACEDONIAN("Macedonian", "mk"),
        MALAY("Malay", "ms"),
        MALTESE("Maltese", "mt"),
        MARATHI("Marathi (मराठी)", "mr"),
        NORWEGIAN("Norwegian", "no"),
        PERSIAN("Persian (فارسی)", "fa"),
        POLISH("Polish", "pl"),
        PORTUGUESE("Portuguese (Português)", "pt"),
        ROMANIAN("Romanian", "ro"),
        RUSSIAN("Russian (Русский)", "ru"),
        SLOVAK("Slovak", "sk"),
        SLOVENIAN("Slovenian", "sl"),
        SPANISH("Spanish (Español)", "es"),
        SWAHILI("Swahili", "sw"),
        SWEDISH("Swedish", "sv"),
        TAGALOG("Tagalog", "tl"),
        TAMIL("Tamil (தமிழ்)", "ta"),
        TELUGU("Telugu (తెలుగు)", "te"),
        THAI("Thai (ไทย)", "th"),
        TURKISH("Turkish (Türkçe)", "tr"),
        UKRAINIAN("Ukrainian", "uk"),
        URDU("Urdu (اردو)", "ur"),
        VIETNAMESE("Vietnamese (Tiếng Việt)", "vi"),
        WELSH("Welsh", "cy");
        
        companion object {
            fun fromLanguageCode(code: String): TestLocale? {
                return values().find { it.languageCode == code }
            }
        }
    }
    
    /**
     * Sets the app locale and persists the preference.
     * 
     * @param context Android context
     * @param languageCode Language code (e.g., "es", "fr", "de")
     */
    fun setLocale(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(SELECTED_LANGUAGE_PREF, languageCode)
            .apply()
        updateResources(context, languageCode)
    }
    
    /**
     * Gets the currently selected locale.
     * 
     * @param context Android context
     * @return Current locale language code, or system default if not set
     */
    fun getCurrentLocale(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SELECTED_LANGUAGE_PREF, Locale.getDefault().language) ?: "en"
    
    /**
     * Applies the persisted locale to the context.
     * Should be called in attachBaseContext() of activities.
     * 
     * @param context Android context
     * @return Context with updated locale
     */
    fun onAttach(context: Context): Context =
        updateResources(context, getCurrentLocale(context))
    
    /**
     * Restarts the entire app to fully apply locale changes.
     * 
     * @param context Android context
     */
    fun restartApp(context: Context) {
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            context.startActivity(Intent.makeRestartActivityTask(it.component))
            Runtime.getRuntime().exit(0)
        }
    }
    
    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Gets a list of all supported test locales for display in a selector.
     * 
     * @return Array of display names for all test locales
     */
    fun getAllLocaleNames(): Array<String> =
        TestLocale.values().map { it.displayName }.toTypedArray()
    
    /**
     * Gets a list of all supported test locale language codes.
     * 
     * @return Array of language codes for all test locales
     */
    fun getAllLocaleCodes(): Array<String> =
        TestLocale.values().map { it.languageCode }.toTypedArray()
}

