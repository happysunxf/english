package com.morningenglish.app.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.morningenglish.app.MorningEnglishApp
import com.morningenglish.app.scheduler.AlarmScheduler
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = MorningEnglishApp.instance
    val settingsRepo = app.settingsRepository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val settings by settingsRepo.settingsFlow.collectAsState(
        initial = null
    )
    val s = settings ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // === 每日播放时间 ===
            SettingRow(
                title = "每日播放时间",
                subtitle = String.format("%02d:%02d", s.playHour, s.playMinute)
            ) {
                Button(onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            scope.launch {
                                settingsRepo.updatePlayTime(hour, minute)
                                AlarmScheduler.scheduleNext(context, settingsRepo)
                            }
                        },
                        s.playHour, s.playMinute, true
                    ).show()
                }) { Text("修改") }
            }

            HorizontalDivider()

            // === 启用定时播放 ===
            SettingRow(
                title = "启用每日播放",
                subtitle = "关闭后闹钟不再响起"
            ) {
                Switch(
                    checked = s.enabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsRepo.setEnabled(enabled)
                            AlarmScheduler.scheduleNext(context, settingsRepo)
                        }
                    }
                )
            }

            // === 周末是否播放 ===
            SettingRow(
                title = "周末播放",
                subtitle = "周六周日也会播"
            ) {
                Switch(
                    checked = s.weekendPlay,
                    onCheckedChange = { weekend ->
                        scope.launch {
                            settingsRepo.setWeekendPlay(weekend)
                            AlarmScheduler.scheduleNext(context, settingsRepo)
                        }
                    }
                )
            }

            // === 锁屏覆盖 ===
            SettingRow(
                title = "锁屏覆盖播放",
                subtitle = "锁屏时也能看到视频画面"
            ) {
                Switch(
                    checked = s.lockScreenPlay,
                    onCheckedChange = { v ->
                        scope.launch { settingsRepo.setLockScreenPlay(v) }
                    }
                )
            }

            HorizontalDivider()

            // === 难度 ===
            Text(
                "难度筛选",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val options = listOf(1 to "初级", 2 to "中级", 3 to "高级")
                options.forEachIndexed { index, (level, label) ->
                    SegmentedButton(
                        selected = s.difficulty == level,
                        onClick = {
                            scope.launch { settingsRepo.updateDifficulty(level) }
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index, count = options.size
                        )
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // === 关于 ===
            Text(
                "MorningEnglish v0.1",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "素材:Peppa Pig (英音版)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        content()
    }
}