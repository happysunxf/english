package com.morningenglish.app.ui.permission

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.morningenglish.app.MorningEnglishApp
import com.morningenglish.app.scheduler.DailyCheckWorker
import com.morningenglish.app.util.PermissionHelper
import com.morningenglish.app.util.RomHelper
import kotlinx.coroutines.launch

/**
 * First-launch setup wizard.
 *
 * Guides the user through granting permissions needed for the 6:30 alarm
 * to actually fire on most Android devices (especially Chinese ROMs).
 *
 * Steps:
 *  1. POST_NOTIFICATIONS (Android 13+)
 *  2. SCHEDULE_EXACT_ALARM (Android 12+)
 *  3. Battery optimization whitelist
 *  4. ROM-specific auto-start whitelist (Xiaomi/Huawei/OPPO/vivo)
 */
@Composable
fun PermissionScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val app = MorningEnglishApp.instance
    val scope = rememberCoroutineScope()

    var notificationsGranted by remember {
        mutableStateOf(PermissionHelper.hasNotificationPermission(context))
    }
    var exactAlarmGranted by remember {
        mutableStateOf(PermissionHelper.canScheduleExactAlarms(context))
    }
    var batteryWhitelisted by remember {
        mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context))
    }
    var romInfo by remember { mutableStateOf(RomHelper.detect(context)) }

    // Recompute states when user returns from settings
    LaunchedEffect(Unit) {
        // Initial computation
        recomputeStates(context).let { (n, e, b) ->
            notificationsGranted = n
            exactAlarmGranted = e
            batteryWhitelisted = b
        }
        checkAllGranted()
    }

    fun checkAllGranted() {
        if (notificationsGranted && exactAlarmGranted && batteryWhitelisted) {
            scope.launch {
                app.settingsRepository.setPermissionsGranted(true)
                DailyCheckWorker.enqueue(context)
                onAllGranted()
            }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "🌅 MorningEnglish",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "为了保证每天早上 6:30 能准时播放,需要授予以下权限:",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))

            PermissionCard(
                title = "1. 通知权限",
                description = "播放时显示通知,防止后台被杀",
                granted = notificationsGranted,
                actionLabel = "授予",
                onGrant = { PermissionHelper.requestNotificationPermission(context) },
                onSettings = { PermissionHelper.openAppSettings(context) }
            )

            PermissionCard(
                title = "2. 精准闹钟",
                description = "Android 12+ 需要单独授权才能精确触发 6:30",
                granted = exactAlarmGranted,
                actionLabel = "去设置",
                onGrant = { PermissionHelper.openExactAlarmSettings(context) },
                onSettings = { PermissionHelper.openAppSettings(context) }
            )

            PermissionCard(
                title = "3. 电池优化白名单",
                description = "把 MorningEnglish 加入白名单,避免被系统杀后台",
                granted = batteryWhitelisted,
                actionLabel = "请求白名单",
                onGrant = { PermissionHelper.requestIgnoreBatteryOptimizations(context) },
                onSettings = { PermissionHelper.openBatteryOptimizationSettings(context) }
            )

            if (romInfo != RomHelper.RomType.STOCK) {
                PermissionCard(
                    title = "4. ${romInfo.displayName} 自启管理",
                    description = romHelperHint(romInfo),
                    granted = false, // Cannot detect programmatically
                    actionLabel = "去设置",
                    onGrant = { RomHelper.openAutoStartSettings(context, romInfo) },
                    onSettings = { RomHelper.openAutoStartSettings(context, romInfo) }
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { checkAllGranted() },
                modifier = Modifier.fillMaxWidth(),
                enabled = notificationsGranted && exactAlarmGranted && batteryWhitelisted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("完成设置,进入主页")
            }

            Text(
                "提示:完成设置后,可以按 Home 键退出。app 会在后台等待每天 6:30 自动播放。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onGrant: () -> Unit,
    onSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                (if (granted) "✅ " else "⬜ ") + title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = if (granted) onSettings else onGrant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (granted) "查看" else actionLabel)
            }
        }
    }
}

private fun recomputeStates(context: android.content.Context): Triple<Boolean, Boolean, Boolean> {
    return Triple(
        PermissionHelper.hasNotificationPermission(context),
        PermissionHelper.canScheduleExactAlarms(context),
        PermissionHelper.isIgnoringBatteryOptimizations(context)
    )
}

private fun romHelperHint(rom: RomHelper.RomType): String = when (rom) {
    RomHelper.RomType.XIAOMI -> "MIUI 默认禁自启,需在 设置→电池→后台耗电管理 中改为\"无限制\""
    RomHelper.RomType.HUAWEI -> "EMUI/HarmonyOS 需在 设置→电池→启动管理 中改为\"手动管理(全开)\""
    RomHelper.RomType.OPPO -> "ColorOS 需在 设置→电池→更多电池设置 中关闭\"睡眠待机优化\""
    RomHelper.RomType.VIVO -> "FuntouchOS 需在 设置→电池→后台高耗电 中允许"
    RomHelper.RomType.ONEPLUS -> "HydrogenOS 需在 设置→电池→电池优化 中选\"不优化\""
    RomHelper.RomType.SAMSUNG -> "One UI 需在 设置→电池→后台使用限制 中选\"从不\""
    else -> "请将该 app 加入自启动白名单"
}