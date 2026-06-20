package com.morningenglish.app.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.morningenglish.app.MorningEnglishApp
import com.morningenglish.app.service.DailyPlayerService

/**
 * Fires when AlarmManager triggers at the user's configured daily time.
 *
 * Responsibility:
 *  1. Pick today's video via VideoRepository
 *  2. Start DailyPlayerService (foreground) with the chosen video
 *  3. Re-schedule tomorrow's alarm
 *
 * Note: BroadcastReceivers have a ~10s lifetime, so we do this work
 * synchronously and trust the service to continue playback.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_DAILY_PLAY) {
            Log.w(TAG, "Ignoring intent with action: ${intent.action}")
            return
        }

        Log.i(TAG, "Daily play alarm fired")

        val app = context.applicationContext as MorningEnglishApp
        val videoRepo = app.videoRepository
        val settingsRepo = app.settingsRepository

        // Pick today's video + start service (blocking; receiver has 10s budget)
        kotlinx.coroutines.runBlocking {
            val video = videoRepo.pickTodaysVideo()
            if (video == null) {
                Log.w(TAG, "No videos available; alarm fired but nothing to play")
                return@runBlocking
            }
            if (video.filePath == null) {
                Log.w(TAG, "Today's video '${video.title}' not yet downloaded")
                // Future: could trigger a one-shot download here
                return@runBlocking
            }

            val serviceIntent = Intent(context, DailyPlayerService::class.java).apply {
                putExtra(DailyPlayerService.EXTRA_VIDEO_PATH, video.filePath)
                putExtra(DailyPlayerService.EXTRA_VIDEO_TITLE, video.title)
                putExtra(DailyPlayerService.EXTRA_VIDEO_ID, video.id)
            }

            try {
                androidx.core.content.ContextCompat.startForegroundService(
                    context, serviceIntent
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service: ${e.message}")
            }

            // Schedule tomorrow's alarm (idempotent)
            AlarmScheduler.scheduleNext(context, settingsRepo)
        }
    }
}