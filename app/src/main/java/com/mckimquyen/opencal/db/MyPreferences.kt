package com.mckimquyen.opencal.db

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.mckimquyen.opencal.model.History
import com.mckimquyen.opencal.util.Logger

class MyPreferences(context: Context) {

    // https://proandroiddev.com/dark-mode-on-android-app-with-kotlin-dc759fc5f0e1
    companion object {
        private const val THEME = "royTHEME"
        private const val FORCE_DAY_NIGHT = "royFORCE_DAY_NIGHT"
        private const val KEY_VIBRATION_STATUS = "royKEY_VIBRATION_STATUS"
        private const val KEY_HISTORY = "royHISTORY"
        private const val KEY_PREVENT_PHONE_FROM_SLEEPING = "royPREVENT_PHONE_FROM_SLEEPING"
        private const val KEY_HISTORY_SIZE = "royHISTORY_SIZE"
        private const val KEY_SCIENTIFIC_MODE_ENABLED_BY_DEFAULT =
            "roySCIENTIFIC_MODE_ENABLED_BY_DEFAULT"
        private const val KEY_RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT =
            "royRADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT"
        private const val KEY_NUMBER_PRECISION = "royNUMBER_PRECISION"
        private const val KEY_APP_LANGUAGE = "royAPP_LANGUAGE"
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    var theme = preferences.getInt(THEME, -1)
        set(value) = preferences.edit().putInt(THEME, value).apply()
    var forceDayNight = preferences.getInt(FORCE_DAY_NIGHT, MODE_NIGHT_UNSPECIFIED)
        set(value) = preferences.edit().putInt(FORCE_DAY_NIGHT, value).apply()

    var vibrationMode = preferences.getBoolean(KEY_VIBRATION_STATUS, true)
        set(value) = preferences.edit().putBoolean(KEY_VIBRATION_STATUS, value).apply()
    var scientificMode = preferences.getBoolean(KEY_SCIENTIFIC_MODE_ENABLED_BY_DEFAULT, true)
        set(value) = preferences.edit().putBoolean(KEY_SCIENTIFIC_MODE_ENABLED_BY_DEFAULT, value)
            .apply()
    var useRadiansByDefault =
        preferences.getBoolean(KEY_RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT, false)
        set(value) = preferences.edit().putBoolean(KEY_RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT, value)
            .apply()
    private var history = preferences.getString(KEY_HISTORY, null)
        set(value) = preferences.edit().putString(KEY_HISTORY, value).apply()
    var preventPhoneFromSleepingMode =
        preferences.getBoolean(KEY_PREVENT_PHONE_FROM_SLEEPING, false)
        set(value) = preferences.edit().putBoolean(KEY_PREVENT_PHONE_FROM_SLEEPING, value).apply()
    var historySize = preferences.getString(KEY_HISTORY_SIZE, "100")
        set(value) = preferences.edit().putString(KEY_HISTORY_SIZE, value).apply()
    var numberPrecision = preferences.getString(KEY_NUMBER_PRECISION, "10")
        set(value) = preferences.edit().putString(KEY_NUMBER_PRECISION, value).apply()

    var appLanguage = preferences.getString(KEY_APP_LANGUAGE, "")
        set(value) {
            val success = preferences.edit().putString(KEY_APP_LANGUAGE, value).commit()
            Logger.d("MyPreferences.appLanguage setter: value=$value, commit success=$success")
        }

    fun getHistory(): MutableList<History> {
        val gson = Gson()
        // Đọc tươi từ SharedPreferences (không dùng field snapshot `history` đã đọc lúc
        // khởi tạo) để an toàn khi instance được cache & tái sử dụng nhiều lần trong một
        // session — tránh trả về dữ liệu cũ sau saveHistory.
        val json = preferences.getString(KEY_HISTORY, null)
        return if (json != null) {
            gson.fromJson(json, Array<History>::class.java).asList().toMutableList()
        } else {
            mutableListOf()
        }
    }

    fun saveHistory(context: Context, history: List<History>) {
        val gson = Gson()
        val history2 = history.toMutableList()
        historySize?.let { hs ->
            while (hs.toInt() > 0 && history2.size > hs.toInt()) {
                history2.removeAt(0)
            }
        }
        MyPreferences(context).history = gson.toJson(history2) // Convert to json
    }
}
