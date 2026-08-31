package com.mckimquyen.opencal

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.mckimquyen.opencal.util.Logger
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.UIUtils
import java.util.Calendar

open class BaseActivity : AppCompatActivity() {

    // E-INFRA-6: gom setupEdgeToEdge1/2 vào đây thay vì để từng Activity con tự nhớ gọi — đã
    // quên sót 2 lần liên tiếp (BillSplitterActivity, BaseConverterActivity, xem F-UI-12) khi
    // targetSdk 37 (Android 15+) ép edge-to-edge bất kể app có opt-in hay không. Override cả 2
    // overload setContentView vì SettingsActivity dùng bản nhận resId, các activity còn lại dùng
    // bản nhận View (qua ViewBinding).
    override fun setContentView(layoutResID: Int) {
        UIUtils.setupEdgeToEdge1(window)
        super.setContentView(layoutResID)
        applyEdgeToEdgePadding()
    }

    override fun setContentView(view: View) {
        UIUtils.setupEdgeToEdge1(window)
        super.setContentView(view)
        applyEdgeToEdgePadding()
    }

    // Lấy root view thật (con duy nhất của content frame android.R.id.content) thay vì
    // findViewById theo 1 id cố định — mỗi layout hiện đặt tên root khác nhau ("layoutRoot" ở
    // hầu hết activity, "root_layout" ở SplashActivity), cách này không phụ thuộc quy ước đặt tên.
    private fun applyEdgeToEdgePadding() {
        val contentFrame = findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = contentFrame.getChildAt(0) ?: return
        UIUtils.setupEdgeToEdge2(rootView = root, paddingTop = true, paddingBottom = true)
    }

    override fun attachBaseContext(context: Context) {
        // Apply language first
        val newContext = com.mckimquyen.opencal.util.LanguageHelper.applyLanguage(context)

        val override = Configuration(newContext.resources.configuration)
        override.fontScale = 1.0f
        applyOverrideConfiguration(override)
        super.attachBaseContext(newContext)
    }

    override fun onResume() {
        super.onResume()
        // Chung cho mọi Activity: App Open tự-động resume dùng đúng Activity đang foreground thay vì
        // reference cũ (vd MainActivity đã stop) khi quay lại app từ About/Settings/Vip/Splash.
        AdManager.setCurrentActivity(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableAdaptiveRefreshRate()
        }
    }

    private fun enableAdaptiveRefreshRate() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display // Sử dụng API mới
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay // Fallback cho API thấp hơn
        }

        if (display != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val supportedModes = display.supportedModes
                val highestRefreshRateMode = supportedModes.maxByOrNull { it.refreshRate }
                if (highestRefreshRateMode != null) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = highestRefreshRateMode.modeId
                    }
                    Logger.d("Adaptive refresh rate applied: ${highestRefreshRateMode.refreshRate} Hz")
                }
            }
        }
    }
}

//rateAppInApp(BuildConfig.DEBUG)
fun Activity.rateAppInApp(forceRateInApp: Boolean = false) {
    //import gradle app
//    implementation("com.google.android.play:review:2.0.2")
//    implementation("com.google.android.play:review-ktx:2.0.2")

    val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    val lastReviewTime = sharedPreferences.getLong("last_review_time", 0L)
//    Log.d("roy93~", "requestReview lastReviewTime $lastReviewTime")
    val currentTime = Calendar.getInstance().timeInMillis
    val daysSinceLastReview = (currentTime - lastReviewTime) / (1000 * 60 * 60 * 24)
//    Log.d("roy93~", "requestReview forceRateInApp $forceRateInApp")
//    Log.d("roy93~", "requestReview daysSinceLastReview $daysSinceLastReview")
    if (daysSinceLastReview >= 7 || forceRateInApp) {
//    if (daysSinceLastReview >= 7) {
        val reviewManager = ReviewManagerFactory.create(this)
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo: ReviewInfo = task.result
                reviewManager.launchReviewFlow(this, reviewInfo)
                sharedPreferences.edit().putLong("last_review_time", currentTime).apply()
//                Log.d("roy93~", "requestReview result ${task.result}")
//                Log.d("roy93~", "requestReview isSuccessful ${task.isSuccessful}")
//                Log.d("roy93~", "requestReview isCanceled ${task.isCanceled}")
//                Log.d("roy93~", "requestReview isComplete ${task.isComplete}")
//                Log.d("roy93~", "requestReview exception ${task.exception}")
            } else {
//                Log.e("roy93~", "requestReview error ${task.exception}")
            }
        }
    }
}
