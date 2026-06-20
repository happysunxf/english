package com.morningenglish.app.data.repo

import com.morningenglish.app.data.db.PlayHistoryDao
import com.morningenglish.app.data.db.PlayHistoryEntity
import com.morningenglish.app.data.db.VideoDao
import com.morningenglish.app.data.db.VideoEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Single source of truth for video data + playback history.
 *
 * The "pick today's video" algorithm:
 *  1. Find all downloaded videos
 *  2. Filter out videos played in the last 7 days
 *  3. If all were played recently, pick any (round-robin)
 *  4. Otherwise pick a random one from the remaining pool
 *
 * This avoids replaying the same episode twice in a week while ensuring
 * the alarm always has something to play (even on first launch).
 */
class VideoRepository(
    private val videoDao: VideoDao,
    private val historyDao: PlayHistoryDao
) {

    fun observeAvailable(): Flow<List<VideoEntity>> = videoDao.observeAvailable()

    suspend fun ensureSeeded(): Int = videoDao.count()

    suspend fun insertManifest(videos: List<VideoEntity>) {
        videoDao.insertAll(videos)
    }

    suspend fun updateFilePath(videoId: String, filePath: String?) {
        val existing = videoDao.findById(videoId) ?: return
        videoDao.update(existing.copy(filePath = filePath))
    }

    /**
     * Pick today's video. Uses randomness + 7-day exclusion window.
     */
    suspend fun pickTodaysVideo(): VideoEntity? {
        val sevenDaysAgoMs = System.currentTimeMillis() -
                TimeUnit.DAYS.toMillis(EXCLUDE_DAYS)
        return videoDao.pickRandomUnplayed(sevenDaysAgoMs) ?: videoDao.pickAny()
    }

    suspend fun logPlayback(
        videoId: String,
        completed: Boolean,
        durationWatchedSec: Int
    ) {
        val now = System.currentTimeMillis()
        historyDao.insert(
            PlayHistoryEntity(
                videoId = videoId,
                playDate = todayString(now),
                playTime = now,
                completed = completed,
                durationWatchedSec = durationWatchedSec
            )
        )
    }

    fun observeRecentHistory(limit: Int = 50): Flow<List<PlayHistoryEntity>> =
        historyDao.observeRecent(limit)

    fun observeSince(daysBack: Int): Flow<List<PlayHistoryEntity>> {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysBack.toLong())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return historyDao.observeSince(dateFormat.format(Date(cutoff)))
    }

    private fun todayString(epochMs: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(Date(epochMs))
    }

    companion object {
        private const val EXCLUDE_DAYS = 7L
    }
}