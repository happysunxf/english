package com.morningenglish.app.scheduler

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.morningenglish.app.MorningEnglishApp
import java.util.concurrent.TimeUnit

/**
 * Secondary safety net: every 24h, verify that the AlarmManager alarm is
 * still scheduled. If the system killed the alarm (rare on stock Android,
 * common on Chinese ROMs), re-schedule it.
 *
 * This is the fallback layer behind AlarmManager — the alarm itself is the
 * primary trigger; this worker is just a heartbeat.
 */
class DailyCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MorningEnglishApp
        AlarmScheduler.scheduleNext(applicationContext, app.settingsRepository)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_check_work"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}