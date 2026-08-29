package com.mckimquyen.opencal.model

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.databinding.DThemeSelectorBinding
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

        private fun resolveThemeAttrColor(context: Context, attrResId: Int): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attrResId, typedValue, true)
            return typedValue.data
        }

        /**
         * X-UI-1: áp dụng cấu hình theme+day/night tương ứng 1 style index — logic tách riêng
         * khỏi UI dialog để adapter chỉ cần biết styleIndex, không cần biết chi tiết mapping.
         */
        private fun applyStyleSelection(preferences: MyPreferences, styleIndex: Int) {
            when (styleIndex) {
                SYSTEM_STYLE_INDEX -> {
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
        }

        fun openDialogThemeSelector(context: Context) {

            val preferences = MyPreferences(context)

            val systemName =
                if (DynamicColors.isDynamicColorAvailable())
                    "${context.getString(R.string.theme_system)} (${context.getString(R.string.theme_material_you)})"
                else
                    context.getString(R.string.theme_system)

            // X-UI-1: preview carousel — mỗi theme hiện swatch màu thật (background + accent)
            // thay vì chỉ tên chữ, giúp thấy trước theme trông ra sao trước khi áp dụng.
            val swatches = listOf(
                ThemeSwatch(
                    SYSTEM_STYLE_INDEX, systemName,
                    backgroundColor = resolveThemeAttrColor(context, R.attr.background_color),
                    accentColor = resolveThemeAttrColor(context, R.attr.button_equals_color),
                ),
                ThemeSwatch(
                    LIGHT_STYLE_INDEX, context.getString(R.string.theme_light),
                    backgroundColor = ContextCompat.getColor(context, R.color.theme_preview_light_background),
                    accentColor = ContextCompat.getColor(context, R.color.theme_preview_light_accent),
                ),
                ThemeSwatch(
                    DARK_STYLE_INDEX, context.getString(R.string.theme_dark),
                    backgroundColor = ContextCompat.getColor(context, R.color.theme_preview_dark_background),
                    accentColor = ContextCompat.getColor(context, R.color.theme_preview_dark_accent),
                ),
                ThemeSwatch(
                    AMOLED_STYLE_INDEX, context.getString(R.string.theme_amoled),
                    backgroundColor = ContextCompat.getColor(context, R.color.amoled_background_color),
                    accentColor = null,
                ),
                ThemeSwatch(
                    SUNSET_STYLE_INDEX, context.getString(R.string.theme_sunset),
                    backgroundColor = ContextCompat.getColor(context, R.color.sunset_background_color),
                    accentColor = ContextCompat.getColor(context, R.color.sunset_button_equals_color),
                ),
                ThemeSwatch(
                    OCEAN_STYLE_INDEX, context.getString(R.string.theme_ocean),
                    backgroundColor = ContextCompat.getColor(context, R.color.ocean_background_color),
                    accentColor = ContextCompat.getColor(context, R.color.ocean_button_equals_color),
                ),
                ThemeSwatch(
                    FOREST_STYLE_INDEX, context.getString(R.string.theme_forest),
                    backgroundColor = ContextCompat.getColor(context, R.color.forest_background_color),
                    accentColor = ContextCompat.getColor(context, R.color.forest_button_equals_color),
                ),
                ThemeSwatch(
                    NORD_STYLE_INDEX, context.getString(R.string.theme_nord),
                    backgroundColor = ContextCompat.getColor(context, R.color.nord_background_color),
                    accentColor = ContextCompat.getColor(context, R.color.nord_button_equals_color),
                ),
                ThemeSwatch(
                    HIGHCONTRAST_STYLE_INDEX, context.getString(R.string.theme_highcontrast),
                    backgroundColor = ContextCompat.getColor(context, R.color.highcontrast_background_color),
                    accentColor = ContextCompat.getColor(context, R.color.highcontrast_button_equals_color),
                ),
                ThemeSwatch(
                    LAVENDER_STYLE_INDEX, context.getString(R.string.theme_lavender),
                    backgroundColor = ContextCompat.getColor(context, R.color.lavender_background_color),
                    accentColor = ContextCompat.getColor(context, R.color.lavender_button_equals_color),
                ),
            )

            val checkedStyleIndex = when (preferences.theme) {
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
            val checkedPosition = swatches.indexOfFirst { it.styleIndex == checkedStyleIndex }
                .takeIf { it >= 0 } ?: SYSTEM_STYLE_INDEX

            val dialogBinding = DThemeSelectorBinding.inflate(
                android.view.LayoutInflater.from(context)
            )
            val builder = MaterialAlertDialogBuilder(context)
            builder.background = ContextCompat.getDrawable(context, R.drawable.ic_rounded)
            builder.setView(dialogBinding.root)
            val dialog = builder.create()

            dialogBinding.rvThemeSwatches.layoutManager = GridLayoutManager(context, 3)
            dialogBinding.rvThemeSwatches.adapter = ThemeSwatchAdapter(
                items = swatches,
                selectedIndex = checkedPosition,
            ) { selected ->
                applyStyleSelection(preferences, selected.styleIndex)
                dialog.dismiss()
                reloadActivity(context)
            }

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
