package com.frametune.extension.vidy.player

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class VidyPlaybackRecord(
    val contentKey: String,
    val positionSeconds: Long,
    val durationSeconds: Long,
    val lastWatchedTime: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("contentKey", contentKey)
        obj.put("positionSeconds", positionSeconds)
        obj.put("durationSeconds", durationSeconds)
        obj.put("lastWatchedTime", lastWatchedTime)
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): VidyPlaybackRecord? {
            return try {
                val obj = JSONObject(jsonStr)
                VidyPlaybackRecord(
                    contentKey = obj.getString("contentKey"),
                    positionSeconds = obj.getLong("positionSeconds"),
                    durationSeconds = obj.optLong("durationSeconds", 0L),
                    lastWatchedTime = obj.optLong("lastWatchedTime", System.currentTimeMillis())
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * SharedPreferences and in-memory progress tracker for Vidy embed web player.
 */
class VidyProgressStore(
    private val prefs: SharedPreferences? = null
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    companion object {
        const val PREFS_NAME = "vidy_playback_progress"
        const val MIN_SAVE_SECONDS = 15L
        const val COMPLETION_THRESHOLD_SECONDS = 60L

        private var instance: VidyProgressStore? = null

        @Synchronized
        fun getInstance(context: Context): VidyProgressStore {
            return instance ?: VidyProgressStore(context.applicationContext).also {
                instance = it
            }
        }

        fun buildContentKey(type: String, tmdbId: Int, season: Int? = null, episode: Int? = null): String {
            return if (type.equals("tv", ignoreCase = true) || type.equals("series", ignoreCase = true)) {
                val s = season ?: 1
                val e = episode ?: 1
                "tv_${tmdbId}_${s}_${e}"
            } else {
                "movie_${tmdbId}"
            }
        }

        fun formatTime(seconds: Long): String {
            val totalSeconds = seconds.coerceAtLeast(0)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val secs = totalSeconds % 60

            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, secs)
            } else {
                String.format("%02d:%02d", minutes, secs)
            }
        }
    }

    private val inMemoryCache = ConcurrentHashMap<String, VidyPlaybackRecord>()

    init {
        loadFromPrefs()
    }

    @Synchronized
    private fun loadFromPrefs() {
        prefs?.let { sp ->
            for ((_, value) in sp.all) {
                if (value is String) {
                    val record = VidyPlaybackRecord.fromJson(value)
                    if (record != null) {
                        inMemoryCache[record.contentKey] = record
                    }
                }
            }
        }
    }

    @Synchronized
    fun saveProgress(key: String, positionSec: Long, durationSec: Long): Boolean {
        if (positionSec < MIN_SAVE_SECONDS) {
            return false
        }
        if (durationSec > 0 && (durationSec - positionSec) <= COMPLETION_THRESHOLD_SECONDS) {
            clearProgress(key)
            return false
        }

        val record = VidyPlaybackRecord(key, positionSec, durationSec)
        inMemoryCache[key] = record
        prefs?.edit()?.putString(key, record.toJson())?.apply()
        return true
    }

    @Synchronized
    fun getProgress(key: String): VidyPlaybackRecord? {
        return inMemoryCache[key]
    }

    @Synchronized
    fun clearProgress(key: String) {
        inMemoryCache.remove(key)
        prefs?.edit()?.remove(key)?.apply()
    }

    @Synchronized
    fun clearAll() {
        inMemoryCache.clear()
        prefs?.edit()?.clear()?.apply()
    }
}

