package com.omadroid.shell

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log

class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as OmadroidShellApp
        window.decorView.setBackgroundColor(app.theme.background)
        val plugin = app.plugins.holder(KIND_HOME)
        val component = plugin?.entryPoint(KIND_HOME)?.let { ComponentName.unflattenFromString(it) }
        if (plugin == null || component == null) {
            Log.e(TAG, "no home plugin")
            finish()
            return
        }
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),
        )
        finish()
    }

    companion object {
        private const val TAG = "OmadroidShell"
        const val KIND_HOME = "home"
    }
}
