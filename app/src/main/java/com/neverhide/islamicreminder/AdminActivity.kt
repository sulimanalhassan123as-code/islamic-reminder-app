package com.neverhide.islamicreminder

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** In-app info screen pointing to the web admin panel. */
class AdminActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val registered = Prefs.get(this, "registered", false)
        val id = SupabaseApi.deviceId(this)
        findViewById<TextView>(R.id.tvAdminInfo).text =
            "Admin panel (for Never Hide only):\n" +
            "https://islamic-reminder-admin.vercel.app\n\n" +
            "Log in with your admin password to see the number of users " +
            "and send notifications to every device.\n\n" +
            "This device:\n• Device ID: ${id.take(8)}…\n" +
            "• Status: ${if (registered) "registered ✅" else "pending registration ⏳"}"
    }
}
