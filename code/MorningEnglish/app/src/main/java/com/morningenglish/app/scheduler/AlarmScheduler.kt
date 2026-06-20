package com.morningenglish.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.morningenglish.app.data.repo.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar

/**
 * Manages the daily alarm that fires at the user's configured time.
 *
 * Two-tier strategy:
 *  - Primary: AlarmManager.setExactAndAllowWhileIdle (precise, bypasses Doze)
 *  - Fallback: WorkManager periodic 24h check (DailyCheckWorker)
 *
 * The alarm itself doesn't play the video — it broadcasts to
 * AlarmReceiver which starts DailyPlayerService (foreground).
 */
object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    private const val REQUEST_CODE = 1001
    const val ACTION_DAILY_PLAY = "com.morningenglish.app.ACTION_DAILY_PLAY"

    /**
     * Schedule the next occurrence of the daily alarm.
     * Idempotent: re-scheduling the same time replaces the previous alarm.
     *
     * MUST be called from a coroutine scope (Application.onCreate is fine)
     * because we read settings from DataStore.
     */
    suspend fun scheduleNext(context: Context, settingsRepo: SettingsRepository) {
        val settings = settingsRepo.settingsFlow.first()
        if (!settings.enabled) {
            Log.i(TAG, "Daily alarm disabled by user; cancelling")
            cancel(context)
            return
        }

        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMs = computeNextTriggerMs(
            settings.playHour,
            settings.playMinute,
            settings.weekendPlay
        )

        val pendingIntent = buildPendingIntent(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmMgr.canScheduleExactAlarms()) {
                    alarmMgr.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        pendingIntent
                    )
                    Log.i(TAG, "Exact alarm scheduled at $triggerAtMs")
                } else {
                    alarmMgr.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        pendingIntent
                    )
                    Log.w(TAG, "Exact alarm permission denied; using inexact alarm")
                }
            } else {
                alarmMgr.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
                Log.i(TAG, "Alarm scheduled at $triggerAtMs (pre-Android 12)")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}")
        }
    }

    /**
     * Synchronous variant for places without a coroutine scope (e.g. BootReceiver).
     * Reads the current settings snapshot via runBlocking.
     */
    fun scheduleNextBlocking(context: Context, settingsRepo: SettingsRepository) {
        runBlocking {
            scheduleNext(context, settingsRepo)
        }
    }

    /**
     * Compute the next time the alarm should fire.
     *
     * If [weekendPlay] is false and the next occurrence falls on Sat/Sun,
     * skip forward to Monday.
     */
    private fun computeNextTriggerMs(hour: Int, minute: Int, weekendPlay: Boolean): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If today's time has already passed, schedule for tomorrow
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        if (!weekendPlay) {
            while (target.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                target.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            ) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return target.timeInMillis
    }

    fun cancel(context: Context) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmMgr.cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DAILY_PLAY
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_IMMUTABLE
                else 0
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}