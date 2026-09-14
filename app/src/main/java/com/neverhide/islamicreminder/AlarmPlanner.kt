package com.neverhide.islamicreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Schedules every alarm in the app:
 *  - 5 daily azan alarms (Fajr, Dhuhr, Asr, Maghrib, Isha) — exact
 *  - 4 daily reminders (Qur'an / hadith / dhikr) — exact
 *  - 1 special Friday reminder — exact
 *  - midnight reschedule — keeps the schedule rolling daily
 *  - 30-min sync heartbeat — picks up admin broadcasts + updates last_seen
 */
object AlarmPlanner {

    const val ACTION_AZAN = "com.neverhide.AZAN"
    const val ACTION_DAILY = "com.neverhide.DAILY"
    const val ACTION_FRIDAY = "com.neverhide.FRIDAY"
    const val ACTION_RESCHEDULE = "com.neverhide.RESCHEDULE"
    const val ACTION_SYNC = "com.neverhide.SYNC"

    private val DAILY_SLOTS = doubleArrayOf(6.0, 12.15, 16.0, 20.0) // 4 reminders/day
    private const val FRIDAY_SLOT = 8.0

    fun scheduleAll(ctx: Context) {
        scheduleAllWithRetry(ctx, 0)
    }

    private fun scheduleAllWithRetry(ctx: Context, attempt: Int) {
        try {
            doScheduleAll(ctx)
        } catch (e: SecurityException) {
            // Exact alarm permission revoked — fall back to inexact
            try { doScheduleAllInexact(ctx) } catch (ignored: Exception) {}
        } catch (e: Exception) {
            if (attempt < 2) scheduleAllWithRetry(ctx, attempt + 1)
        }
    }

    private fun doScheduleAll(ctx: Context) {
        val am = alarmManager(ctx)
        val canExact = canExact(ctx)

        val (today, tomorrow) = PrayerEngine.todayTimes(
            Prefs.get(ctx, Prefs.LAT, 5.6037),
            Prefs.get(ctx, Prefs.LNG, -0.1870),
            tzOffsetHours(),
            Prefs.get(ctx, Prefs.FAJR_ANGLE, 15.0),
            Prefs.get(ctx, Prefs.ISHA_ANGLE, 15.0),
            Prefs.get(ctx, Prefs.ASR_FACTOR, 1.0)
        )

        if (Prefs.get(ctx, Prefs.AZAN_ON, true)) {
            schedulePrayerAlarm(ctx, am, canExact, "Fajr", today.fajr)
            schedulePrayerAlarm(ctx, am, canExact, "Dhuhr", today.dhuhr)
            schedulePrayerAlarm(ctx, am, canExact, "Asr", today.asr)
            schedulePrayerAlarm(ctx, am, canExact, "Maghrib", today.maghrib)
            schedulePrayerAlarm(ctx, am, canExact, "Isha", today.isha)
            schedulePrayerAlarm(ctx, am, canExact, "Fajr", tomorrow.fajr)
        } else {
            cancelAlarms(ctx, am, ACTION_AZAN)
        }

        if (Prefs.get(ctx, Prefs.DAILY_ON, true)) {
            for (i in DAILY_SLOTS.indices) {
                val cal = timeToday(DAILY_SLOTS[i])
                if (cal.timeInMillis > System.currentTimeMillis()) {
                    val pi = pending(ctx, ACTION_DAILY, "daily$i", i + 1000)
                    setExactOrInexact(am, canExact, cal.timeInMillis, pi)
                }
            }
        } else {
            cancelAlarms(ctx, am, ACTION_DAILY)
        }

        if (Prefs.get(ctx, Prefs.FRIDAY_ON, true)) {
            val friday = nextFriday(FRIDAY_SLOT)
            val pi = pending(ctx, ACTION_FRIDAY, "friday", 2000)
            setExactOrInexact(am, canExact, friday.timeInMillis, pi)
        } else {
            cancelAlarms(ctx, am, ACTION_FRIDAY)
        }

        // Midnight reschedule (keeps the day rolling)
        val midnight = timeToday(0.0167) // 00:01
        val midCal = midnight.clone() as Calendar
        if (midCal.timeInMillis <= System.currentTimeMillis()) midCal.add(Calendar.DAY_OF_YEAR, 1)
        val midPi = pending(ctx, ACTION_RESCHEDULE, "resched", 3000)
        setExactOrInexact(am, canExact, midCal.timeInMillis, midPi)

        // 30-min sync heartbeat (inexact is fine; battery-friendly)
        val syncPi = pending(ctx, ACTION_SYNC, "sync", 4000)
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 5 * 60 * 1000,
            30 * 60 * 1000,
            syncPi
        )
    }

    private fun doScheduleAllInexact(ctx: Context) {
        // Same schedule but without exact flag
        // Simplest: schedule a reschedule every 30 min + prayer via inexact
        val am = alarmManager(ctx)
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 1)
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, 30 * 60 * 1000,
            pending(ctx, ACTION_SYNC, "sync", 4000))
    }

    private fun schedulePrayerAlarm(ctx: Context, am: AlarmManager, canExact: Boolean, prayer: String, hours: Double) {
        val cal = hoursToCal(hours)
        if (cal.timeInMillis <= System.currentTimeMillis()) return
        val pi = pending(ctx, ACTION_AZAN, prayer, prayer.hashCode())
        setExactOrInexact(am, canExact, cal.timeInMillis, pi)
    }

    private fun cancelAlarms(ctx: Context, am: AlarmManager, action: String) {
        if (action == ACTION_AZAN) {
            for (p in listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")) {
                am.cancel(pending(ctx, ACTION_AZAN, p, p.hashCode()))
            }
        } else if (action == ACTION_DAILY) {
            for (i in 0..3) am.cancel(pending(ctx, ACTION_DAILY, "daily$i", i + 1000))
        } else if (action == ACTION_FRIDAY) {
            am.cancel(pending(ctx, ACTION_FRIDAY, "friday", 2000))
        }
    }

    private fun setExactOrInexact(am: AlarmManager, canExact: Boolean, at: Long, pi: PendingIntent) {
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun canExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = alarmManager(ctx)
        return try { am.canScheduleExactAlarms() } catch (e: Exception) { true }
    }

    private fun alarmManager(ctx: Context): AlarmManager =
        ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pending(ctx: Context, action: String, extra: String, requestCode: Int): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra("name", extra)
        }
        return PendingIntent.getBroadcast(
            ctx, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Converts hour-of-day (double, local) to a Calendar today. */
    private fun hoursToCal(hours: Double): Calendar {
        val cal = Calendar.getInstance()
        var total = Math.round(hours * 60.0)
        cal.set(Calendar.HOUR_OF_DAY, (total / 60).toInt() % 24)
        cal.set(Calendar.MINUTE, (total % 60).toInt())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // If the time already passed today, it's tomorrow's slot
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal
    }

    private fun timeToday(hours: Double): Calendar {
        val cal = Calendar.getInstance()
        var total = Math.round(hours * 60.0)
        cal.set(Calendar.HOUR_OF_DAY, (total / 60).toInt() % 24)
        cal.set(Calendar.MINUTE, (total % 60).toInt())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    private fun nextFriday(hours: Double): Calendar {
        val cal = timeToday(hours)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY ||
               cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal
    }

    private fun tzOffsetHours(): Double {
        val tz = java.util.TimeZone.getDefault()
        return tz.getOffset(System.currentTimeMillis()) / 3600000.0
    }
}
