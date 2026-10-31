package com.frametune.extension.vidy.resolver

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves IMDb IDs (e.g. tt1375666) to TMDB IDs directly on the TV device.
 * Queries public Cinemeta metadata with memory caching.
 */
object ImdbToTmdbResolver {
    private const val TAG = "ImdbToTmdbResolver"
    private val cache = ConcurrentHashMap<String, Int>()

    fun parseTmdbIdFromCinemeta(jsonStr: String): Int? {
        return try {
            val json = JSONObject(jsonStr)
            val meta = json.optJSONObject("meta") ?: return null
            when {
                meta.has("moviedb_id") -> meta.optInt("moviedb_id").takeIf { it > 0 }
                meta.has("tmdb_id") -> meta.optInt("tmdb_id").takeIf { it > 0 }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun resolve(imdbId: String, type: String = "movie"): Int? = withContext(Dispatchers.IO) {
        cache[imdbId]?.let { return@withContext it }

        val endpoint = "https://v3-cinemeta.strem.io/meta/$type/$imdbId.json"
        try {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500
                readTimeout = 4500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV)")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val tmdbId = parseTmdbIdFromCinemeta(body)
                if (tmdbId != null) {
                    cache[imdbId] = tmdbId
                    return@withContext tmdbId
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve TMDB id for $imdbId: ${e.message}")
        }
        null
    }
}

