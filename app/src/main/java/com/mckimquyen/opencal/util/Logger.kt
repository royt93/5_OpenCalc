package com.mckimquyen.opencal.util

import android.util.Log
import com.mckimquyen.opencal.BuildConfig

/**
 * Lightweight logging wrapper. Mọi log tự động tắt ở bản release
 * (chỉ in khi [BuildConfig.DEBUG] = true) để tránh spam logcat
 * và rò rỉ thông tin nội bộ ngoài production.
 */
object Logger {
    private const val DEFAULT_TAG = "roy93~"

    fun d(message: String, tag: String = DEFAULT_TAG) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = DEFAULT_TAG) {
        if (BuildConfig.DEBUG) Log.e(tag, message, throwable)
    }
}
