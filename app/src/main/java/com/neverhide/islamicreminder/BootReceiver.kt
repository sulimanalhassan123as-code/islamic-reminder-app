package com.neverhide.islamicreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // Delay slightly so the system is ready after boot
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        Notifications.ensureChannels(ctx)
                        AlarmPlanner.scheduleAll(ctx)
                        SupabaseApi.registerAsync(ctx)
                    } catch (e: Exception) { }
                }, 3000)
            }
        }
    }
}
