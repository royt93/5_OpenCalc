package com.mckimquyen.opencal.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mckimquyen.opencal.BuildConfig
import com.mckimquyen.opencal.databinding.ActivitySplashBinding
import com.mckimquyen.opencal.sdkadbmob.AdMobManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("roy93~", "onCreate")
        binding = ActivitySplashBinding.inflate(layoutInflater)
        lifecycleScope.launch {
            var hasCalledGoToMain = false
            val job = launch {
                delay(3_000)
                if (!hasCalledGoToMain) {
                    hasCalledGoToMain = true
                    Log.d("roy93~", "goToMain #1")
                    goToMain()
                }
            }
            AdMobManager.loadAppOpenAd(
                context = this@SplashActivity,
                adUnitId = BuildConfig.ADMOB_APP_OPEN_ID,
                onAdLoaded = {
                    if (!hasCalledGoToMain) {
                        hasCalledGoToMain = true
                        job.cancel()
                        Log.d("roy93~", "goToMain #2")
                        goToMain()
                        AdMobManager.showAppOpenAd(this@SplashActivity)
                    }
                },
            )
        }
    }

    fun goToMain() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finishAffinity()
    }
}
