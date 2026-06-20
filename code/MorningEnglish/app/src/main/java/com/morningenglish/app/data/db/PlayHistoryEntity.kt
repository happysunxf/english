package com.morningenglish.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Log entry written each time a video plays to completion (or is interrupted).
 *
 * Used by HomeScreen to show "this week's playback history".
 */
@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val playDate: String,         // "2026-06-21"
    val playTime: Long,           // epoch millis
    val completed: Boolean,       // true if video played to end
    val durationWatchedSec: Int   // how many seconds the user actually watched
)