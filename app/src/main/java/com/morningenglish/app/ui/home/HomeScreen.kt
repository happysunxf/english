package com.morningenglish.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.morningenglish.app.MorningEnglishApp
import com.morningenglish.app.data.db.VideoEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit
) {
    val app = MorningEnglishApp.instance
    val videoRepo = app.videoRepository
    val scope = rememberCoroutineScope()

    val availableVideos by videoRepo.observeAvailable().collectAsState(initial = emptyList())
    val recentHistory by videoRepo.observeSince(7).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌅  ")
                        Text(
                            "Morning English",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                TodaySection(
                    availableCount = availableVideos.size,
                    nextVideo = availableVideos.firstOrNull(),
                    onPlayNow = {
                        scope.launch {
                            val v = videoRepo.pickTodaysVideo()
                            // Trigger playback via intent
                        }
                    },
                    onShuffle = {
                        scope.launch { videoRepo.pickTodaysVideo() }
                    }
                )
            }

            item {
                Text(
                    "📅 本周播放",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (recentHistory.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "本周还没播放过。明天早上 6:30 自动开始第一段。",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                items(recentHistory) { entry ->
                    HistoryRow(entry = entry)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TodaySection(
    availableCount: Int,
    nextVideo: VideoEntity?,
    onPlayNow: () -> Unit,
    onShuffle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "今日播放",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (nextVideo != null) {
                Text(
                    nextVideo.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "时长 ${nextVideo.durationSec / 60}分${nextVideo.durationSec % 60}秒 | 难度 ${"⭐".repeat(nextVideo.difficulty)}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    if (availableCount == 0)
                        "还没有下载任何素材。请用 scripts/download_assets.py 下载 Peppa Pig。"
                    else
                        "请稍候,正在准备...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlayNow) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("立即播放")
                }
                Button(onClick = onShuffle) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("换一个")
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: com.morningenglish.app.data.db.PlayHistoryEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (entry.completed) "✅" else "⏸"
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(12.dp))
            Column {
                Text(entry.playDate, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "观看 ${entry.durationWatchedSec}秒",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}