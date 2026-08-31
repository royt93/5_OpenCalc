package com.mckimquyen.opencal.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.databinding.ActivitySplashBinding
import com.mckimquyen.opencal.util.Logger
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.UIUtils

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d("onCreate")
        UIUtils.setupEdgeToEdge1(window)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UIUtils.setupEdgeToEdge2(
            rootView = findViewById(R.id.root_layout),
            paddingTop = true,
            paddingBottom = true,
        )
        // UMP consent trước — gate App Open warmup (và Banner/Interstitial về sau) tới khi
        // consent resolved. KHÔNG show App Open trên splash thuần: initSplashScreen warmup
        // xong mới navigate sang main (tuân thủ AdMob policy).
        AdManager.requestConsentInfoUpdate(this) { canRequestAds ->
            Logger.d("UMP consent gathered, canRequestAds=$canRequestAds")
            proceedAfterConsent()
        }
    }

    private fun proceedAfterConsent() {
        if (isFinishing || isDestroyed) return
        AdManager.initSplashScreen(this) {
            goToMain()
        }
    }

    private val finishRunnable = Runnable { finish() }

    private fun goToMain() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // Sync finish timing with system animation duration for smoother transition
        val animationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime)
        window.decorView.postDelayed(finishRunnable, animationDuration.toLong())
    }

    override fun onDestroy() {
        // Fix memory leak: remove pending postDelayed callbacks
        window.decorView.removeCallbacks(finishRunnable)
        super.onDestroy()
    }
}
