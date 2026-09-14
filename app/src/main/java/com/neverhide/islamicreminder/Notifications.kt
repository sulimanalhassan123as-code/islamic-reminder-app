package com.neverhide.islamicreminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object Notifications {

    const val CH_AZAN = "azan_channel"
    const val CH_REMINDER = "reminder_channel"
    const val CH_ADMIN = "admin_channel"

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val azan = NotificationChannel(
            CH_AZAN, "Azan — Prayer Times", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Plays azan and shows a full-screen alert at prayer time"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            setBypassDnd(true)
        }

        val reminder = NotificationChannel(
            CH_REMINDER, "Daily Reminders", NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Qur'an, hadith and dhikr reminders — 4 times a day"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val admin = NotificationChannel(
            CH_ADMIN, "Admin Announcements", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Messages sent by Never Hide"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannel(azan)
        nm.createNotificationChannel(reminder)
        nm.createNotificationChannel(admin)
    }

    private fun contentIntent(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun showAzan(ctx: Context, prayer: String) {
        ensureChannels(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Full-screen intent → AzanActivity shows over the lock screen
        val fsIntent = Intent(ctx, AzanActivity::class.java).apply {
            putExtra("prayer", prayer)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val fsPi = PendingIntent.getActivity(
            ctx, prayer.hashCode(), fsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = Notification.Builder(ctx, CH_AZAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🕌 Azan — $prayer")
            .setContentText("It is time for $prayer prayer. Azan is playing.")
            .setPriority(Notification.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(fsPi, true)
            .setContentIntent(contentIntent(ctx))
            .setAutoCancel(true)
            .build()

        nm.notify(1001, notif)

        // Start the azan audio service
        val svc = Intent(ctx, AzanService::class.java).apply { putExtra("prayer", prayer) }
        ctx.startForegroundService(svc)
    }

    fun showDaily(ctx: Context, slot: Int) {
        ensureChannels(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val c = Content.forDailySlot(ctx, slot)

        val notif = Notification.Builder(ctx, CH_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(c.first)
            .setStyle(Notification.BigTextStyle().bigText(c.second))
            .setContentText(c.second)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent(ctx))
            .setAutoCancel(true)
            .build()

        nm.notify(2000 + slot, notif)
    }

    fun showFriday(ctx: Context) {
        ensureChannels(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notif = Notification.Builder(ctx, CH_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(Content.FRIDAY_TITLE)
            .setStyle(Notification.BigTextStyle().bigText(Content.fridayMessage()))
            .setContentText(Content.fridayMessage())
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentIntent(contentIntent(ctx))
            .setAutoCancel(true)
            .build()

        nm.notify(2500, notif)
    }

    fun showAdminBroadcast(ctx: Context, id: Long, title: String, body: String) {
        ensureChannels(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notif = Notification.Builder(ctx, CH_ADMIN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📢 $title")
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentText(body)
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentIntent(contentIntent(ctx))
            .setAutoCancel(true)
            .build()

        nm.notify(id.toInt(), notif)
    }
}
