package com.morningenglish.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.morningenglish.app.MorningEnglishApp
import com.morningenglish.app.R
import com.morningenglish.app.ui.PlayerActivity

/**
 * Foreground service that hosts the daily Peppa Pig video playback.
 *
 * Lifecycle:
 *  1. AlarmReceiver fires at 6:30
 *  2. DailyPlayerService starts (foreground with notification)
 *  3. Service acquires a WakeLock to keep CPU alive
 *  4. Service launches PlayerActivity in fullscreen
 *  5. PlayerActivity uses ExoPlayer to render the video
 *  6. When playback ends or user exits, service stops
 *
 * Important: We do NOT call ExoPlayer.prepare() here. The activity owns
 * the player. The service only holds the WakeLock and notification.
 */
class DailyPlayerService : Service() {

    companion object {
        private const val TAG = "DailyPlayerService"
        private const val NOTIFICATION_ID = 1001

        const val EXTRA_VIDEO_PATH = "extra_video_path"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
        const val EXTRA_VIDEO_ID = "extra_video_id"

        const val ACTION_STOP = "com.morningenglish.app.ACTION_STOP_PLAYBACK"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var startTimeMs: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startTimeMs = System.currentTimeMillis()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "Stop action received")
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val videoPath = intent?.getStringExtra(EXTRA_VIDEO_PATH)
        val videoTitle = intent?.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Morning English"

        if (videoPath.isNullOrBlank()) {
            Log.w(TAG, "No video path provided; stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        // 1. Promote to foreground (mandatory on Android 8+)
        startForeground(NOTIFICATION_ID, buildNotification(videoTitle))

        // 2. Launch the fullscreen player
        val playerIntent = Intent(this, PlayerActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra(EXTRA_VIDEO_PATH, videoPath)
            putExtra(EXTRA_VIDEO_TITLE, videoTitle)
            putExtra(EXTRA_VIDEO_ID, intent.getStringExtra(EXTRA_VIDEO_ID) ?: "")
        }
        try {
            startActivity(playerIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch PlayerActivity: ${e.message}")
            stopSelf()
        }

        // Do NOT use START_STICKY — if killed, we want WorkManager to retry
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        Log.i(TAG, "Service destroyed (lifetime: ${System.currentTimeMillis() - startTimeMs}ms)")
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MorningEnglish:PlaybackWakeLock"
        ).apply {
            // Hard cap: 10 minutes — service should not run longer than the video
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun buildNotification(videoTitle: String): Notification {
        val stopIntent = Intent(this, DailyPlayerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE
                    else 0
        )

        val openIntent = Intent(this, PlayerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE
                    else 0
        )

        return NotificationCompat.Builder(this, MorningEnglishApp.CHANNEL_PLAYBACK)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(videoTitle)
            .setSmallIcon(R.drawable.ic_play_notification)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.action_stop),
                stopPendingIntent
            )
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}