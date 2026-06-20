package com.morningenglish.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.morningenglish.app.MorningEnglishApp
import com.morningenglish.app.service.DailyPlayerService
import com.morningenglish.app.ui.theme.MorningEnglishTheme
import com.morningenglish.app.util.PermissionHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Fullscreen video player. Launched by DailyPlayerService when the alarm fires.
 *
 * Behavior:
 *  - Turns screen on + shows on lock screen
 *  - Plays the video to completion
 *  - On end, logs the play to history + finishes the activity
 *  - User can press Back to exit early
 */
class PlayerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PlayerActivity"
    }

    private var player: ExoPlayer? = null
    private var videoId: String = ""
    private var startTimeMs: Long = 0L
    private var finishedNaturally: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PermissionHelper.canShowOverLockscreen(this)) {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val videoPath = intent.getStringExtra(DailyPlayerService.EXTRA_VIDEO_PATH)
        val videoTitle = intent.getStringExtra(DailyPlayerService.EXTRA_VIDEO_TITLE) ?: ""
        videoId = intent.getStringExtra(DailyPlayerService.EXTRA_VIDEO_ID) ?: ""
        startTimeMs = System.currentTimeMillis()

        if (videoPath.isNullOrBlank()) {
            Log.w(TAG, "No video path; finishing")
            finish()
            return
        }

        setContent {
            MorningEnglishTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    VideoPlayer(
                        videoPath = videoPath,
                        videoTitle = videoTitle,
                        onPlayerReady = { exo -> player = exo },
                        onPlaybackEnded = {
                            Log.i(TAG, "Playback ended naturally")
                            finishedNaturally = true
                            logHistory(completed = true)
                            stopServiceAndFinish()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val watched = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
        if (watched > 0 && !finishedNaturally) {
            logHistory(completed = false, duration = watched)
        }
        player?.release()
        player = null
        stopService()
    }

    private fun logHistory(completed: Boolean, duration: Int = -1) {
        if (videoId.isBlank()) return
        val app = applicationContext as MorningEnglishApp
        val repo = app.videoRepository
        GlobalScope.launch {
            val dur = if (duration > 0) duration else
                ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
            repo.logPlayback(videoId, completed, dur)
        }
    }

    private fun stopServiceAndFinish() {
        finishedNaturally = true
        stopService()
        finish()
    }

    private fun stopService() {
        val intent = Intent(this, DailyPlayerService::class.java).apply {
            action = DailyPlayerService.ACTION_STOP
        }
        startService(intent)
    }
}

/**
 * Composable that hosts ExoPlayer via AndroidView.
 *
 * Notes:
 *  - We remember the ExoPlayer instance so it's not recreated on recomposition.
 *  - DisposableEffect releases the player when the composable leaves.
 *  - Listener fires onPlaybackEnded callback when state reaches STATE_ENDED.
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    videoPath: String,
    videoTitle: String,
    onPlayerReady: (ExoPlayer) -> Unit,
    onPlaybackEnded: () -> Unit
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(java.io.File(videoPath))))
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        onPlaybackEnded()
                    }
                }
            })
            prepare()
        }
    }

    LaunchedEffect(player) {
        onPlayerReady(player)
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowSubtitleButton(false)
                    controllerHideOnTouch = true
                    controllerAutoShow = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}