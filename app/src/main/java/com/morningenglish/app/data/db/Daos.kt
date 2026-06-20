package com.morningenglish.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(videos: List<VideoEntity>)

    @Update
    suspend fun update(video: VideoEntity)

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): VideoEntity?

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun count(): Int

    /**
     * Returns all videos that have a local file on disk.
     */
    @Query("SELECT * FROM videos WHERE filePath IS NOT NULL ORDER BY season, episode")
    fun observeAvailable(): Flow<List<VideoEntity>>

    /**
     * Pick a random available video not played in the last [excludeDays] days.
     * Falls back to any available video if all have been played recently.
     */
    @Query("""
        SELECT v.* FROM videos v
        WHERE v.filePath IS NOT NULL
          AND v.id NOT IN (
            SELECT DISTINCT ph.videoId FROM play_history ph
            WHERE ph.playTime > :sinceEpochMs
          )
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun pickRandomUnplayed(sinceEpochMs: Long): VideoEntity?

    @Query("""
        SELECT v.* FROM videos v
        WHERE v.filePath IS NOT NULL
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun pickAny(): VideoEntity?
}

@Dao
interface PlayHistoryDao {

    @Insert
    suspend fun insert(entry: PlayHistoryEntity): Long

    @Query("SELECT * FROM play_history ORDER BY playTime DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<PlayHistoryEntity>>

    @Query("""
        SELECT * FROM play_history
        WHERE playDate >= :sinceDate
        ORDER BY playTime DESC
    """)
    fun observeSince(sinceDate: String): Flow<List<PlayHistoryEntity>>

    @Query("SELECT COUNT(*) FROM play_history WHERE playDate = :date")
    suspend fun countByDate(date: String): Int
}