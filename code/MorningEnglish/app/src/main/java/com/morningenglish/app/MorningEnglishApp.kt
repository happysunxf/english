package com.morningenglish.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.morningenglish.app.data.db.AppDatabase
import com.morningenglish.app.data.repo.SettingsRepository
import com.morningenglish.app.data.repo.VideoRepository
import com.morningenglish.app.scheduler.AlarmScheduler
import com.morningenglish.app.util.VideoSeeder

/**
 * Application entry point.
 *
 * Responsibilities:
 *  - Initialize Room database
 *  - Seed Peppa Pig asset manifest on first launch
 *  - Create notification channel
 *  - Schedule the daily alarm (idempotent)
 */
class MorningEnglishApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val videoRepository: VideoRepository by lazy {
        VideoRepository(database.videoDao(), database.playHistoryDao())
    }
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()

        // Seed Peppa Pig catalog on first run
        VideoSeeder.seedIfNeeded(this, videoRepository)

        // Register daily alarm (idempotent: if already scheduled, replaces)
        AlarmScheduler.scheduleNext(this, settingsRepository)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_PLAYBACK,
                getString(R.string.channel_playback_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_playback_desc)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_PLAYBACK = "playback_channel"
        lateinit var instance: MorningEnglishApp
            private set
    }
}