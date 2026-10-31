package com.frametune.extension.vidy.server

import android.util.Log
import com.frametune.extension.vidy.resolver.ImdbToTmdbResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Embedded Stremio HTTP Addon Server running locally on 127.0.0.1:8910 using ServerSocket.
 * Directly queried by FrameTune TV's PluginEngine when searching/picking movies.
 */
class LocalAddonServer(private val port: Int = 8910) {
    companion object {
        private const val TAG = "LocalAddonServer"

        fun buildManifestJson(): String {
            return JSONObject().apply {
                put("id", "org.frametune.extension.vidy")
                put("name", "Vidy Embed Player")
                put("version", "1.0.0")
                put("description", "Vidy embed streaming with multi-audio, subtitle selection & backup servers.")
                put("resources", JSONArray().apply { put("stream") })
                put("types", JSONArray().apply {
                    put("movie")
                    put("series")
                })
                put("idPrefixes", JSONArray().apply { put("tt") })
                put("catalogs", JSONArray())
            }.toString()
        }

        fun buildStreamResponse(
            type: String,
            tmdbId: Int,
            title: String,
            season: Int?,
            episode: Int?
        ): String {
            val playUri = if (type == "series" && season != null && episode != null) {
                "vidy://play?type=tv&tmdb=$tmdbId&season=$season&episode=$episode"
            } else {
                "vidy://play?type=movie&tmdb=$tmdbId"
            }

            val streams = JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "Vidy Player")
                    put("title", "⚡ Vidy Embed Player\n✨ Inbuilt Audio & Subs • Multiple Servers")
                    put("url", playUri)
                    put("behaviorHints", JSONObject().apply {
                        put("notWebReady", false)
                    })
                })
            }

            return JSONObject().apply {
                put("streams", streams)
            }.toString()
        }
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    val isRunning: Boolean
        get() = serverSocket?.isClosed == false && serverJob?.isActive == true

    @Synchronized
    fun start() {
        if (isRunning) return
        try {
            val socket = ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))
            serverSocket = socket
            serverJob = scope.launch {
                Log.i(TAG, "Local Addon Server listening on http://127.0.0.1:$port")
                while (isActive && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        launch { handleClient(client) }
                    } catch (e: Exception) {
                        if (!socket.isClosed) {
                            Log.w(TAG, "Accept error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start local addon server: ${e.message}")
        }
    }

    @Synchronized
    fun stop() {
        try {
            serverSocket?.close()
            serverJob?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping server: ${e.message}")
        } finally {
            serverSocket = null
            serverJob = null
        }
    }

    private suspend fun handleClient(client: Socket) {
        client.use { s ->
            s.soTimeout = 4000
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            val firstLine = reader.readLine() ?: return
            val parts = firstLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            if (method.equals("OPTIONS", ignoreCase = true)) {
                sendResponse(s.getOutputStream(), 200, "OK", "text/plain", "")
                return
            }

            when {
                path == "/manifest.json" -> {
                    val manifest = buildManifestJson()
                    sendResponse(s.getOutputStream(), 200, "application/json; charset=utf-8", "application/json", manifest)
                }
                path.startsWith("/stream/") -> {
                    handleStreamRequest(s.getOutputStream(), path)
                }
                else -> {
                    sendResponse(s.getOutputStream(), 404, "text/plain", "text/plain", "Not Found")
                }
            }
        }
    }

    private suspend fun handleStreamRequest(output: OutputStream, path: String) {
        val pathParts = path.removePrefix("/stream/").removeSuffix(".json").split("/")
        if (pathParts.size < 2) {
            sendResponse(output, 200, "application/json; charset=utf-8", "application/json", JSONObject().put("streams", JSONArray()).toString())
            return
        }

        val type = pathParts[0]
        val rawId = pathParts[1]
        val idParts = rawId.split(":")
        val imdbId = idParts[0]
        val season = idParts.getOrNull(1)?.toIntOrNull()
        val episode = idParts.getOrNull(2)?.toIntOrNull()

        val tmdbId = ImdbToTmdbResolver.resolve(imdbId, type)
        val responseJson = if (tmdbId != null) {
            buildStreamResponse(type, tmdbId, imdbId, season, episode)
        } else {
            JSONObject().put("streams", JSONArray()).toString()
        }

        sendResponse(output, 200, "application/json; charset=utf-8", "application/json", responseJson)
    }

    private fun sendResponse(
        output: OutputStream,
        statusCode: Int,
        contentType: String,
        statusText: String,
        body: String
    ) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, OPTIONS\r\n" +
                "Connection: close\r\n\r\n"

        output.write(headers.toByteArray(Charsets.US_ASCII))
        if (bytes.isNotEmpty()) {
            output.write(bytes)
        }
        output.flush()
    }
}
