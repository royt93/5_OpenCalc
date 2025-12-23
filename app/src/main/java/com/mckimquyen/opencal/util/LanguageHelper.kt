package com.mckimquyen.opencal.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.db.MyPreferences
import java.util.Locale

object LanguageHelper {

    data class Language(
        val code: String,
        val displayName: String,
        val nativeName: String
    )

    // Danh sách ngôn ngữ được support
    val SUPPORTED_LANGUAGES = listOf(
        Language("", "System Default", "System Default"),
        Language("en", "English", "English"),
        Language("vi", "Vietnamese", "Tiếng Việt"),
        Language("ar", "Arabic", "العربية"),
        Language("az", "Azerbaijani", "Azərbaycan"),
        Language("bn", "Bengali", "বাংলা"),
        Language("bs", "Bosnian", "Bosanski"),
        Language("cs", "Czech", "Čeština"),
        Language("de", "German", "Deutsch"),
        Language("el", "Greek", "Ελληνικά"),
        Language("es", "Spanish", "Español"),
        Language("fa", "Persian", "فارسی"),
        Language("fr", "French", "Français"),
        Language("hi", "Hindi", "हिन्दी"),
        Language("hr", "Croatian", "Hrvatski"),
        Language("in", "Indonesian", "Bahasa Indonesia"),
        Language("it", "Italian", "Italiano"),
        Language("ja", "Japanese", "日本語"),
        Language("mk", "Macedonian", "Македонски"),
        Language("ml", "Malayalam", "മലയാളം"),
        Language("nb", "Norwegian", "Norsk"),
        Language("or", "Odia", "ଓଡ଼ିଆ"),
        Language("pl", "Polish", "Polski"),
        Language("pt", "Portuguese", "Português"),
        Language("ru", "Russian", "Русский"),
        Language("sr", "Serbian", "Српски"),
        Language("tr", "Turkish", "Türkçe"),
        Language("uk", "Ukrainian", "Українська"),
        Language("zh-TW", "Chinese Traditional", "繁體中文"),
    )

    fun showLanguagePicker(activity: Activity) {
        val preferences = MyPreferences(activity)
        val currentLanguage = preferences.appLanguage ?: ""

        val languageNames = SUPPORTED_LANGUAGES.map {
            if (it.code.isEmpty()) {
                activity.getString(R.string.language_system_default)
            } else {
                "${it.nativeName} (${it.displayName})"
            }
        }.toTypedArray()

        val currentIndex = SUPPORTED_LANGUAGES.indexOfFirst { it.code == currentLanguage }
            .takeIf { it >= 0 } ?: 0

        val builder = MaterialAlertDialogBuilder(activity)
        builder.background = ContextCompat.getDrawable(activity, R.drawable.ic_rounded)
        builder.setTitle(activity.getString(R.string.dialog_select_language_title))
        builder.setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
            val selectedLanguage = SUPPORTED_LANGUAGES[which]
            Log.d("roy93~", "User selected language: ${selectedLanguage.code}")

            // Lưu ngôn ngữ và đảm bảo được commit ngay lập tức
            preferences.appLanguage = selectedLanguage.code

            // Verify lại bằng cách tạo MyPreferences instance mới để đọc giá trị vừa lưu
            val verifyPrefs = MyPreferences(activity)
            Log.d("roy93~", "Language saved to preferences: ${verifyPrefs.appLanguage}")

            dialog.dismiss()

            // Restart toàn bộ app để apply ngôn ngữ mới cho tất cả activities
            restartApp(activity)
        }
        builder.create().show()
    }

    fun applyLanguage(context: Context): Context {
        val preferences = MyPreferences(context)
        val languageCode = preferences.appLanguage

        Log.d("roy93~", "applyLanguage called with code: $languageCode")

        if (languageCode.isNullOrEmpty()) {
            // Use system default
            Log.d("roy93~", "Using system default language")
            return context
        }

        val locale = when {
            languageCode.contains("-") -> {
                val parts = languageCode.split("-")
                Locale(parts[0], parts[1])
            }
            else -> Locale(languageCode)
        }

        Locale.setDefault(locale)
        Log.d("roy93~", "Locale set to: ${locale.language}_${locale.country}")

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            context
        }
    }

    fun getCurrentLanguageDisplayName(context: Context): String {
        val preferences = MyPreferences(context)
        val currentCode = preferences.appLanguage ?: ""

        return SUPPORTED_LANGUAGES.find { it.code == currentCode }?.let {
            if (it.code.isEmpty()) {
                context.getString(R.string.language_system_default)
            } else {
                it.nativeName
            }
        } ?: context.getString(R.string.language_system_default)
    }

    private fun restartApp(activity: Activity) {
        // Lấy launch intent của MainActivity
        val packageManager = activity.packageManager
        val intent = packageManager.getLaunchIntentForPackage(activity.packageName)

        intent?.let {
            // Clear all activities trong back stack và start MainActivity mới
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            activity.startActivity(it)
            activity.finish()

            // Force kill process để đảm bảo app restart hoàn toàn
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
