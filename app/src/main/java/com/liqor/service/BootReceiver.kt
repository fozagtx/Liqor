package com.liqor.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.liqor.data.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val prefs = Prefs(context)
        if (prefs.mode == Prefs.MODE_NONE || !Settings.canDrawOverlays(context)) return
        context.startForegroundService(Intent(context, WatchService::class.java))
    }
}
