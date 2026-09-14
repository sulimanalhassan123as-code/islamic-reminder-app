package com.neverhide.islamicreminder

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

/**
 * Foreground service that plays the azan out loud with a persistent
 * notification so Android never kills it mid-call.
 */
class AzanService : Service() {

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Stop button pressed from the notification
        if (intent?.action == "STOP") {
            startForeground(9999, buildNotification("Stop"))
            onDestroy()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        val prayer = intent?.getStringExtra("prayer") ?: "Prayer"

        // Must call startForeground within 5s
        startForeground(9999, buildNotification(prayer))

        // Hold a wake lock so audio keeps playing with screen off
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IslamicReminder:Azan").apply {
            setReferenceCounted(false)
            acquire(5 * 60 * 1000L) // max 5 minutes
        }

        try {
            // Route to speaker even if device is on vibrate/silent for media
            val audio = getSystemService(AUDIO_SERVICE) as AudioManager
            try {
                audio.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    0
                )
            } catch (e: Exception) { }

            player = MediaPlayer.create(this, R.raw.azan)
            player?.setOnCompletionListener {
                stopSelf()
            }
            player?.setOnErrorListener { _, _, _ ->
                stopSelf()
                true
            }
            player?.start()
        } catch (e: Exception) {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(prayer: String): Notification {
        Notifications.ensureChannels(this)
        val stopPi = PendingIntent.getService(
            this, 5001,
            Intent(this, AzanService::class.java).setAction("STOP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, Notifications.CH_AZAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🕌 Azan playing — $prayer")
            .setContentText("Tap stop to silence the azan.")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, "Stop", stopPi).build())
            .build()
    }

    override fun onDestroy() {
        try { player?.stop() } catch (e: Exception) { }
        try { player?.release() } catch (e: Exception) { }
        player = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }
}
