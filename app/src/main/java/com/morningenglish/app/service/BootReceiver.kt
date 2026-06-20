package com.morningenglish.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.morningenglish.app.MorningEnglishApp
import com.morningenglish.app.scheduler.AlarmScheduler
import com.morningenglish.app.scheduler.DailyCheckWorker

/**
 * Re-registers the daily alarm after device reboot or app upgrade.
 *
 * AlarmManager loses all alarms when the device reboots. The system
 * broadcasts BOOT_COMPLETED to apps in the auto-start whitelist
 * (default for most launchers, but blocked on Xiaomi/Huawei/etc — see
 * RomHelper for guidance).
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) {
            Log.w(TAG, "Ignoring action: $action")
            return
        }

        Log.i(TAG, "Received $action — re-scheduling daily alarm")
        val app = context.applicationContext as MorningEnglishApp

        // Re-schedule the primary alarm
        AlarmScheduler.scheduleNextBlocking(context, app.settingsRepository)

        // Re-enqueue the periodic fallback worker
        DailyCheckWorker.enqueue(context)
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}