package com.morningenglish.app.util

import android.content.Context
import android.util.Log
import com.morningenglish.app.data.db.VideoEntity
import com.morningenglish.app.data.repo.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Seeds the Room database with the Peppa Pig asset catalog on first launch.
 *
 * The catalog lives at `assets/manifest/peppa_pig_manifest.json` — a JSON
 * file containing 30 curated episodes. This avoids hardcoding URLs in the
 * Kotlin source (which would force a recompile to update).
 *
 * Actual mp4 download is done OUT-OF-BAND via `scripts/download_assets.py`
 * (which uses yt-dlp). After downloading, the user re-runs the seeder
 * (or manually updates `filePath` via Room) to point each row at its
 * local file. Until then, `filePath = null` means "not yet downloaded".
 *
 * Path convention: files stored at
 *   {context.filesDir}/videos/{video.id}.mp4
 */
object VideoSeeder {

    private const val TAG = "VideoSeeder"
    private const val MANIFEST_PATH = "manifest/peppa_pig_manifest.json"

    suspend fun seedIfNeeded(context: Context, repo: VideoRepository) {
        val existing = repo.ensureSeeded()
        if (existing > 0) {
            Log.i(TAG, "Database already has $existing videos, skipping seed")
            return
        }

        val videos = loadManifest(context)
        if (videos.isEmpty()) {
            Log.w(TAG, "Manifest empty or missing; no videos seeded")
            return
        }

        repo.insertManifest(videos)
        Log.i(TAG, "Seeded ${videos.size} Peppa Pig videos")
    }

    private suspend fun loadManifest(context: Context): List<VideoEntity> =
        withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open(MANIFEST_PATH).bufferedReader()
                    .use { it.readText() }
                parseManifest(json)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load manifest: ${e.message}")
                emptyList()
            }
        }

    private fun parseManifest(json: String): List<VideoEntity> {
        val root = JSONObject(json)
        val arr: JSONArray = root.getJSONArray("videos")
        val now = System.currentTimeMillis()
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            VideoEntity(
                id = obj.getString("id"),
                title = obj.getString("title"),
                filePath = null,  // populated by download step
                remoteUrl = obj.getString("url"),
                durationSec = obj.getInt("duration_sec"),
                difficulty = obj.getInt("difficulty"),
                source = obj.optString("source", "peppa_pig"),
                tags = obj.optString("tags", ""),
                createTime = now,
                season = obj.getInt("season"),
                episode = obj.getInt("episode")
            )
        }
    }
}