package com.mckimquyen.opencal.model

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.db.MyPreferences

class Themes(private val context: Context) {

    companion object {

        // Themes
        private const val DEFAULT_THEME_INDEX = 0
        private const val AMOLED_THEME_INDEX = 1
        private const val MATERIAL_YOU_THEME_INDEX = 2
        private const val SUNSET_THEME_INDEX = 3
        private const val OCEAN_THEME_INDEX = 4
        private const val FOREST_THEME_INDEX = 5
        private const val NORD_THEME_INDEX = 6
        private const val HIGHCONTRAST_THEME_INDEX = 7
        private const val LAVENDER_THEME_INDEX = 8

        // used to go from Preference int value to actual theme
        private val themeMap = mapOf(
            DEFAULT_THEME_INDEX to R.style.AppTheme,
            AMOLED_THEME_INDEX to R.style.AmoledTheme,
            MATERIAL_YOU_THEME_INDEX to R.style.MaterialYouTheme,
            SUNSET_THEME_INDEX to R.style.SunsetTheme,
            OCEAN_THEME_INDEX to R.style.OceanTheme,
            FOREST_THEME_INDEX to R.style.ForestTheme,
            NORD_THEME_INDEX to R.style.NordTheme,
            HIGHCONTRAST_THEME_INDEX to R.style.HighContrastTheme,
            LAVENDER_THEME_INDEX to R.style.LavenderTheme
        )

        // Styles - Combinations of theme + day/night mode
        private const val SYSTEM_STYLE_INDEX = 0
        private const val LIGHT_STYLE_INDEX = 1
        private const val DARK_STYLE_INDEX = 2
        private const val AMOLED_STYLE_INDEX = 3
        private const val SUNSET_STYLE_INDEX = 4
        private const val OCEAN_STYLE_INDEX = 5
        private const val FOREST_STYLE_INDEX = 6
        private const val NORD_STYLE_INDEX = 7
        private const val HIGHCONTRAST_STYLE_INDEX = 8
        private const val LAVENDER_STYLE_INDEX = 9

        fun openDialogThemeSelector(context: Context) {

            val preferences = MyPreferences(context)

            val builder = MaterialAlertDialogBuilder(context)
            builder.background = ContextCompat.getDrawable(context, R.drawable.ic_rounded)

            val systemName =
                if (DynamicColors.isDynamicColorAvailable())
                    "${context.getString(R.string.theme_system)} (${context.getString(R.string.theme_material_you)})"
                else
                    context.getString(R.string.theme_system)

            val styles = hashMapOf(
                SYSTEM_STYLE_INDEX to systemName,
                LIGHT_STYLE_INDEX to context.getString(R.string.theme_light),
                DARK_STYLE_INDEX to context.getString(R.string.theme_dark),
                AMOLED_STYLE_INDEX to context.getString(R.string.theme_amoled),
                SUNSET_STYLE_INDEX to context.getString(R.string.theme_sunset),
                OCEAN_STYLE_INDEX to context.getString(R.string.theme_ocean),
                FOREST_STYLE_INDEX to context.getString(R.string.theme_forest),
                NORD_STYLE_INDEX to context.getString(R.string.theme_nord),
                HIGHCONTRAST_STYLE_INDEX to context.getString(R.string.theme_highcontrast),
                LAVENDER_STYLE_INDEX to context.getString(R.string.theme_lavender)
            )

            val checkedItem = when (preferences.theme) {
                AMOLED_THEME_INDEX -> AMOLED_STYLE_INDEX
                MATERIAL_YOU_THEME_INDEX -> SYSTEM_STYLE_INDEX
                SUNSET_THEME_INDEX -> SUNSET_STYLE_INDEX
                OCEAN_THEME_INDEX -> OCEAN_STYLE_INDEX
                FOREST_THEME_INDEX -> FOREST_STYLE_INDEX
                NORD_THEME_INDEX -> NORD_STYLE_INDEX
                HIGHCONTRAST_THEME_INDEX -> HIGHCONTRAST_STYLE_INDEX
                LAVENDER_THEME_INDEX -> LAVENDER_STYLE_INDEX
                else -> {
                    when (preferences.forceDayNight) {
                        AppCompatDelegate.MODE_NIGHT_NO -> LIGHT_STYLE_INDEX
                        AppCompatDelegate.MODE_NIGHT_YES -> DARK_STYLE_INDEX
                        else -> SYSTEM_STYLE_INDEX
                    }
                }
            }

            builder.setSingleChoiceItems(
                styles.values.toTypedArray(),
                checkedItem
            ) { dialog, which ->
                when (which) {
                    SYSTEM_STYLE_INDEX -> {
                        // system style uses the Material You theme if supported
                        preferences.theme =
                            if (DynamicColors.isDynamicColorAvailable()) MATERIAL_YOU_THEME_INDEX else DEFAULT_THEME_INDEX
                        preferences.forceDayNight = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }

                    LIGHT_STYLE_INDEX -> {
                        preferences.theme = DEFAULT_THEME_INDEX
                        preferences.forceDayNight = AppCompatDelegate.MODE_NIGHT_NO
                    }

                    DARK_STYLE_INDEX -> {
                        preferences.theme = DEFAULT_THEME_INDEX
                        preferences.forceDayNight = AppCompatDelegate.MODE_NIGHT_YES
                    }

                    AMOLED_STYLE_INDEX -> {
                        preferences.theme = AMOLED_THEME_INDEX
                        preferences.forceDayNight = AppCompatDelegate.MODE_NIGHT_YES
                    }

                    SUNSET_STYLE_INDEX -> {
                        preferences.theme = SUNSET_THEME_INDEX
                        preferences.forceDayNight = AppCompatDelegate.MODE_NIGHT_NO
                    }

                    OCEAN_STYLE_INDEX -> {
                        preferences.theme = OCEAN_THEME_INDEX
                        preferences.forceDayNight = AppCompatDelegate.MODE_NIGHT_NO
                    }

                    FOREST_STYLE_INDEX -> {
                        preferences.theme = FOREST_THEME_INDEX
                        preferences.forceDayNight = AppCompatDelegate.MODE_NIGHT_NO
                    }

                    NORD_STYLE_INDEX -> {
                        preferences.theme = NORD_THEME_INDEX
                        preferences.forceDayNight = AppCompatDelegate.MODE_NIGHT_NO
                    }

                    HIGHCONTRAST_STYLE_INDEX -> {
                        preferences.theme = HIGHCONTRAST_THEME_INDEX
                        preferences.forceDayNight = AppCompatDelegate.MODE_NIGHT_NO
                    }

                    LAVENDER_STYLE_INDEX -> {
                        preferences.theme = LAVENDER_THEME_INDEX
                        preferences.forceDayNight = AppCompatDelegate.MODE_NIGHT_NO
                    }
                }
                dialog.dismiss()
                reloadActivity(context)
            }
            val dialog = builder.create()
            dialog.show()
        }

        private fun reloadActivity(context: Context) {
            (context as Activity).finish()
            startActivity(context, context.intent, null)
        }
    }

    fun applyDayNightOverride() {
        val preferences = MyPreferences(context)
        if (preferences.forceDayNight != AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
            AppCompatDelegate.setDefaultNightMode(preferences.forceDayNight)
        }
    }

    fun getTheme(): Int {
        var theme = MyPreferences(context).theme
        if (theme == -1) {
            theme =
                if (DynamicColors.isDynamicColorAvailable()) MATERIAL_YOU_THEME_INDEX else DEFAULT_THEME_INDEX
        }
        return themeMap[theme] ?: DEFAULT_THEME_INDEX
    }
}
