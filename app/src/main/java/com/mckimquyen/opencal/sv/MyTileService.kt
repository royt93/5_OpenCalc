package com.mckimquyen.opencal.sv

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.mckimquyen.opencal.ui.MainActivity

@RequiresApi(Build.VERSION_CODES.N)
class MyTileService : TileService() {

    // Called when the user taps on your tile in an active or inactive state.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val pkgName = this.packageName
        val intent = Intent().apply {
            component = ComponentName(
                pkgName,
                MainActivity::class.java.name
            )
            addFlags(FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    /* context = */ this,
                    /* requestCode = */ 0,
                    /* intent = */ intent,
                    /* flags = */ PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
