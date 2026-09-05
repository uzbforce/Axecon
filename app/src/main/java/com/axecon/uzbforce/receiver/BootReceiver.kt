package com.axecon.uzbforce.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.axecon.uzbforce.data.PreferencesManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = PreferencesManager.getInstance(context)
            val config = prefs.getConfig()
            // Accessibility service is auto-reconnected by the OS on boot if granted.
            // If the user has auto-start enabled, ensure preferences are intact.
            if (config.isEnabled) {
                // Device booted with AirCursor active. OS will rebind AirCursorAccessibilityService.
            }
        }
    }
}
