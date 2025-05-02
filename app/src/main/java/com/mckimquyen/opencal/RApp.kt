package com.mckimquyen.opencal

import android.app.Application
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import com.mckimquyen.opencal.db.MyPreferences

//TODO firebase
//TODO splash screen

//review in app
//applovin done ad id
//120hz
//font scale
//done
//leak canary
//permission ad id
//policy
//rate app, more app, share app
//github, fork github
//double tap to exit
//join beta
//ic_launcher
//keystore

//https://github.com/tplloi/OpenCalc/tree/dev
class RApp : Application() {

    override fun onCreate() {
        super.onCreate()

//        this.setupApplovinAd()

        // if the theme is overriding the system, the first creation doesn't work properly
        val forceDayNight = MyPreferences(this).forceDayNight
        if (forceDayNight != MODE_NIGHT_UNSPECIFIED && forceDayNight != MODE_NIGHT_FOLLOW_SYSTEM) {
            setDefaultNightMode(forceDayNight)
        }

        if (BuildConfig.DEBUG) {
            Toast.makeText(this, "OpenCalcApplication onCreate", Toast.LENGTH_SHORT).show()
        }
    }
}
