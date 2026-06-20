package com.morningenglish.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single video asset (e.g. Peppa Pig S01E01).
 *
 * @param id            stable UUID, used for de-duplication
 * @param title         human-readable title (e.g. "Peppa Pig - Muddy Puddles")
 * @param filePath      absolute path inside filesDir, or null if not downloaded yet
 * @param remoteUrl     canonical YouTube URL (for re-download)
 * @param durationSec   expected duration in seconds
 * @param difficulty    1=初级 2=中级 3=高级
 * @param source        e.g. "peppa_pig"
 * @param tags          comma-separated tags for filtering
 * @param createTime    epoch millis
 * @param season        season number (1-based)
 * @param episode       episode number (1-based)
 */
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String?,
    val remoteUrl: String,
    val durationSec: Int,
    val difficulty: Int,
    val source: String,
    val tags: String,
    val createTime: Long,
    val season: Int,
    val episode: Int
)