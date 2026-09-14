package com.neverhide.islamicreminder

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

/** Full-screen azan screen — shows over the lock screen at prayer time. */
class AzanActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show over lock screen, keep screen on
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setContentView(R.layout.activity_azan)

        val prayer = intent.getStringExtra("prayer") ?: "Prayer"
        findViewById<TextView>(R.id.tvAzanPrayer).text = prayer
        findViewById<TextView>(R.id.tvAzanMsg).text =
            "It is time for $prayer prayer.\nAllahu Akbar — come to success."

        findViewById<Button>(R.id.btnAzanStop).setOnClickListener {
            stopService(android.content.Intent(this, AzanService::class.java))
            finish()
        }
    }
}
