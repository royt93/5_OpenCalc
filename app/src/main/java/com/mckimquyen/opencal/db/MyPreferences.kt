package com.mckimquyen.opencal.db

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.mckimquyen.opencal.model.History
import com.mckimquyen.opencal.util.Logger

class MyPreferences(context: Context) {

    // https://proandroiddev.com/dark-mode-on-android-app-with-kotlin-dc759fc5f0e1
    companion object {
        private const val THEME = "royTHEME"
        private const val FORCE_DAY_NIGHT = "royFORCE_DAY_NIGHT"
        // F-UI-1: phải khớp app:key trong res/xml/root_preferences.xml — trước đây lệch
        // ("roy*" vs "mckimquyen.opencal.*") khiến toggle Settings không có tác dụng gì.
        private const val KEY_VIBRATION_STATUS = "mckimquyen.opencal.KEY_VIBRATION_STATUS"
        private const val KEY_HISTORY = "royHISTORY"
        private const val KEY_PREVENT_PHONE_FROM_SLEEPING =
            "mckimquyen.opencal.PREVENT_PHONE_FROM_SLEEPING"
        private const val KEY_HISTORY_SIZE = "mckimquyen.opencal.HISTORY_SIZE"
        private const val KEY_SCIENTIFIC_MODE_ENABLED_BY_DEFAULT =
            "mckimquyen.opencal.SCIENTIFIC_MODE_ENABLED_BY_DEFAULT"
        private const val KEY_RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT =
            "mckimquyen.opencal.RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT"
        private const val KEY_NUMBER_PRECISION = "mckimquyen.opencal.NUMBER_PRECISION"
        private const val KEY_APP_LANGUAGE = "royAPP_LANGUAGE"

        // F-DATA-4: historySize="-1" (∞) không được nghĩa là "không giới hạn tuyệt đối" —
        // vẫn phải chặn ở 1 trần cứng để tránh blob SharedPreferences+Gson phình vô hạn
        // (re-serialize toàn bộ mỗi lần thêm 1 phần tử) gây chậm dần/OOM về lâu dài.
        private const val HISTORY_HARD_CAP = 10_000
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
            try {
                gson.fromJson(json, Array<History>::class.java).asList().toMutableList()
            } catch (e: JsonSyntaxException) {
                // F-DATA-1: JSON hỏng (restore backup lệch version, ghi dở khi bị kill...)
                // không được crash app mỗi lần mở — coi như mất lịch sử, không mất cả app.
                Logger.d("MyPreferences.getHistory: corrupt history JSON, resetting. ${e.message}")
                mutableListOf()
            }
        } else {
            mutableListOf()
        }
    }

    fun saveHistory(history: List<History>) {
        val gson = Gson()
        val history2 = history.toMutableList()
        val userLimit = historySize?.toIntOrNull()?.takeIf { it > 0 }
        val effectiveLimit = userLimit ?: HISTORY_HARD_CAP
        // N-DATA-4: xoá entry cũ nhất CHƯA GHIM trước — break nếu toàn bộ còn lại đã ghim (thà
        // vượt effectiveLimit một chút còn hơn xoá nhầm data user chủ ý giữ lại).
        while (history2.size > effectiveLimit) {
            val index = history2.indexOfFirst { !it.isPinned }
            if (index == -1) break
            history2.removeAt(index)
        }
        // F-DATA-6: ghi thẳng qua `preferences` của chính instance này thay vì tạo
        // MyPreferences(context) mới chỉ để gọi setter — field `history` cũ chỉ tồn tại làm
        // trung gian cho setter đó, không ai đọc, nên bỏ luôn cả field lẫn param `context` thừa.
        preferences.edit().putString(KEY_HISTORY, gson.toJson(history2)).apply()
    }
}
