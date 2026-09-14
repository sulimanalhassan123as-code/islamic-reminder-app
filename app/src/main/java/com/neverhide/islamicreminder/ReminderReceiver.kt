package com.neverhide.islamicreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            AlarmPlanner.ACTION_AZAN -> {
                val prayer = intent.getStringExtra("name") ?: "Prayer"
                Notifications.showAzan(ctx, prayer)
                // Reschedule rolling alarms
                AlarmPlanner.scheduleAll(ctx)
            }
            AlarmPlanner.ACTION_DAILY -> {
                val slot = intent.getIntExtra("slot", -1)
                Notifications.showDaily(ctx, slot)
                AlarmPlanner.scheduleAll(ctx)
            }
            AlarmPlanner.ACTION_FRIDAY -> {
                Notifications.showFriday(ctx)
                AlarmPlanner.scheduleAll(ctx)
            }
            AlarmPlanner.ACTION_RESCHEDULE -> {
                AlarmPlanner.scheduleAll(ctx)
            }
            AlarmPlanner.ACTION_SYNC -> {
                // Keep alarms fresh + check for admin broadcasts + last_seen
                AlarmPlanner.scheduleAll(ctx)
                SupabaseApi.syncAsync(ctx)
            }
        }
    }
}
