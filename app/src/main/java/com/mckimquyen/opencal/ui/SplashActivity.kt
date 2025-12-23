package com.mckimquyen.opencal.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.databinding.ActivitySplashBinding
import com.mckimquyen.opencal.sdkadbmob.AdMobManager
import com.mckimquyen.opencal.sdkadbmob.UIUtils

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("roy93~", "onCreate")
        UIUtils.setupEdgeToEdge1(window)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UIUtils.setupEdgeToEdge2(
            rootView = findViewById(R.id.root_layout),
            paddingTop = true,
            paddingBottom = true,
        )
        AdMobManager.initSplashScreen(activity = this, onAdLoaded = {
            goToMain()
        })
    }

    private fun goToMain() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // Sync finish timing with system animation duration for smoother transition
        val animationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime)
        window.decorView.postDelayed({
            finish()
        }, animationDuration.toLong())
    }
}
