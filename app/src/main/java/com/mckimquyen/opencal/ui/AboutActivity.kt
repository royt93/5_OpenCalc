package com.mckimquyen.opencal.ui

import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mckimquyen.opencal.BaseActivity
import com.mckimquyen.opencal.BuildConfig
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.databinding.AAboutBinding
import com.mckimquyen.opencal.db.MyPreferences
import com.mckimquyen.opencal.ext.moreApp
import com.mckimquyen.opencal.ext.openBrowserPolicy
import com.mckimquyen.opencal.ext.rateApp
import com.mckimquyen.opencal.ext.sendEmail
import com.mckimquyen.opencal.ext.shareApp
import com.mckimquyen.opencal.model.Themes
import com.mckimquyen.opencal.rateAppInApp
import com.roy.sdkadbmob.AdManager

class AboutActivity : BaseActivity() {
    private lateinit var binding: AAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViews()
    }

    override fun onResume() {
        super.onResume()
        rateAppInApp(BuildConfig.DEBUG)
    }

    private fun setupViews() {
        // Themes
        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        // Change the status bar color
        if (MyPreferences(this).theme == 1) { // Amoled theme
            window.statusBarColor = ContextCompat.getColor(this, R.color.amoled_background_color)
        } else {
            window.statusBarColor = ContextCompat.getColor(this, R.color.background_color)
        }

        // E-INFRA-6: setupEdgeToEdge1/2 giờ tự động chạy trong BaseActivity.setContentView().
        binding = AAboutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Set app version
        val versionName =
            this.getString(R.string.app_version_title) + " " + BuildConfig.VERSION_NAME
        binding.tvAboutAppVersion.text = versionName

        // back button
        binding.ivAboutBack.setOnClickListener {
            finish()
        }

        // Rate
        binding.tvAboutRate.setOnClickListener {
            rateApp(packageName)
        }
        binding.tvAboutMoreApp.setOnClickListener {
            moreApp()
        }
        binding.tvAboutShareApp.setOnClickListener {
            shareApp()
        }

        binding.tvAboutSendFeedback.setOnClickListener {
            sendEmail()
        }

        binding.tvAboutPrivacyPolicy.setOnClickListener {
            openBrowserPolicy()
        }

        var clickAppVersionCount = 0
        binding.tvAboutAppVersion.setOnClickListener {
            clickAppVersionCount++
            if (clickAppVersionCount > 3) {
                Toast.makeText(
                    this,
                    this.getString(R.string.thanks_for_using_app),
                    Toast.LENGTH_SHORT
                ).show()
                clickAppVersionCount = 0
            }
        }

        // autoManageLifecycle mặc định true: SDK tự hook resume/pause/destroy qua
        // ActivityLifecycleCallbacks — KHÔNG tự gọi bannerResume/Pause/Destroy (double lifecycle).
        AdManager.loadBanner(
            context = this,
            container = binding.layoutAdBanner.bannerContainer,
            tvLabelAd = binding.layoutAdBanner.tvLabelAd,
        )
    }
}
