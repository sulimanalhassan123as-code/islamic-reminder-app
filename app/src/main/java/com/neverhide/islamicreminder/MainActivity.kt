package com.neverhide.islamicreminder

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            renderNextPrayer()
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Notifications.ensureChannels(this)

        findViewById<Button>(R.id.btnNotif).setOnClickListener { requestNotifPermission() }
        findViewById<Button>(R.id.btnExact).setOnClickListener { openExactAlarmSettings() }
        findViewById<Button>(R.id.btnBattery).setOnClickListener { requestBatteryExemption() }
        findViewById<Button>(R.id.btnTestNotif).setOnClickListener {
            Notifications.showDaily(this, 1)
        }
        findViewById<Button>(R.id.btnTestAzan).setOnClickListener {
            Notifications.showAzan(this, "Test")
        }

        findViewById<Switch>(R.id.swAzan).setOnCheckedChangeListener { _, on ->
            Prefs.set(this, Prefs.AZAN_ON, on); AlarmPlanner.scheduleAll(this)
        }
        findViewById<Switch>(R.id.swDaily).setOnCheckedChangeListener { _, on ->
            Prefs.set(this, Prefs.DAILY_ON, on); AlarmPlanner.scheduleAll(this)
        }
        findViewById<Switch>(R.id.swFriday).setOnCheckedChangeListener { _, on ->
            Prefs.set(this, Prefs.FRIDAY_ON, on); AlarmPlanner.scheduleAll(this)
        }

        findViewById<Button>(R.id.btnAdmin).setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        renderStatus()
        renderNextPrayer()

        // First run: run the full permission flow once automatically
        if (!Prefs.get(this, Prefs.SETUP_DONE, false)) {
            Prefs.set(this, Prefs.SETUP_DONE, true)
            requestNotifPermission()
        }

        // Schedule + register on every open
        AlarmPlanner.scheduleAll(this)
        SupabaseApi.registerAsync(this)
        SupabaseApi.syncAsync(this)

        handler.postDelayed(tick, 1000)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    // ---------- Permission flow ----------

    private fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
            renderStatus()
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")))
            }
        }
    }

    private fun requestBatteryExemption() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            @Suppress("DEPRECATION")
            startActivity(Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            ))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        renderStatus()
    }

    // ---------- Rendering ----------

    private fun renderStatus() {
        val notifOk = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        findViewById<TextView>(R.id.stNotif).text = if (notifOk) "✅ Granted" else "❌ Not granted"
        findViewById<Button>(R.id.btnNotif).isEnabled = !notifOk

        val exactOk = AlarmPlanner.canExact(this)
        findViewById<TextView>(R.id.stExact).text = if (exactOk) "✅ Granted" else "❌ Not granted"
        findViewById<Button>(R.id.btnExact).isEnabled = !exactOk

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val battOk = pm.isIgnoringBatteryOptimizations(packageName)
        findViewById<TextView>(R.id.stBattery).text = if (battOk) "✅ Enabled" else "❌ Restricted"
        findViewById<Button>(R.id.btnBattery).isEnabled = !battOk

        findViewById<Switch>(R.id.swAzan).isChecked = Prefs.get(this, Prefs.AZAN_ON, true)
        findViewById<Switch>(R.id.swDaily).isChecked = Prefs.get(this, Prefs.DAILY_ON, true)
        findViewById<Switch>(R.id.swFriday).isChecked = Prefs.get(this, Prefs.FRIDAY_ON, true)

        val registered = Prefs.get(this, "registered", false)
        findViewById<TextView>(R.id.stRegistered).text =
            if (registered) "✅ Registered — you'll receive admin announcements"
            else "⏳ Registering… (needs internet)"
    }

    private fun renderNextPrayer() {
        try {
            val (today, tomorrow) = PrayerEngine.todayTimes(
                Prefs.get(this, Prefs.LAT, 5.6037),
                Prefs.get(this, Prefs.LNG, -0.1870),
                tzOffset(),
                Prefs.get(this, Prefs.FAJR_ANGLE, 15.0),
                Prefs.get(this, Prefs.ISHA_ANGLE, 15.0),
                Prefs.get(this, Prefs.ASR_FACTOR, 1.0)
            )

            val nowH = nowHours()
            val list = listOf(
                "Fajr" to today.fajr, "Dhuhr" to today.dhuhr, "Asr" to today.asr,
                "Maghrib" to today.maghrib, "Isha" to today.isha, "Fajr" to tomorrow.fajr + 24.0
            )
            val next = list.firstOrNull { it.second > nowH } ?: ("Fajr" to today.fajr + 24.0)

            findViewById<TextView>(R.id.tvNext).text =
                "Next: ${next.first} at ${PrayerEngine.format12(next.second % 24.0)}"

            findViewById<TextView>(R.id.tvTimes).text =
                "Fajr: ${PrayerEngine.format12(today.fajr)}  |  Dhuhr: ${PrayerEngine.format12(today.dhuhr)}\n" +
                "Asr: ${PrayerEngine.format12(today.asr)}  |  Maghrib: ${PrayerEngine.format12(today.maghrib)}\n" +
                "Isha: ${PrayerEngine.format12(today.isha)}   (Accra)"

            val diffMin = ((next.second - nowH) * 60).toInt()
            findViewById<TextView>(R.id.tvCountdown).text =
                "in ${diffMin / 60}h ${diffMin % 60}m"
        } catch (e: Exception) {
            findViewById<TextView>(R.id.tvTimes).text = "Prayer times unavailable"
        }
    }

    private fun nowHours(): Double {
        val cal = java.util.Calendar.getInstance()
        return cal.get(java.util.Calendar.HOUR_OF_DAY) +
            cal.get(java.util.Calendar.MINUTE) / 60.0 +
            cal.get(java.util.Calendar.SECOND) / 3600.0
    }

    private fun tzOffset(): Double =
        java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0
}
