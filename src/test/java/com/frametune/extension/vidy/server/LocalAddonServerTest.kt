package com.frametune.extension.vidy.server

import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAddonServerTest {

    @Test
    fun testGenerateManifestJson_returnsValidStremioSchema() {
        val manifest: String = LocalAddonServer.buildManifestJson()
        val json = JSONObject(JSONTokener(manifest))

        assertEquals("org.frametune.extension.vidy", json.getString("id"))
        assertEquals("Vidy Embed Player", json.getString("name"))

        val resources = json.getJSONArray("resources")
        var hasStream = false
        for (i in 0 until resources.length()) {
            if (resources.getString(i) == "stream") hasStream = true
        }
        assertTrue("Manifest must include 'stream' in resources", hasStream)

        val types = json.getJSONArray("types")
        assertTrue("Types must include movie", types.toString().contains("movie"))
        assertTrue("Types must include series", types.toString().contains("series"))
    }

    @Test
    fun testBuildStreamResponse_constructsPlayIntentUriForMovie() {
        val streamJson: String = LocalAddonServer.buildStreamResponse(
            type = "movie",
            tmdbId = 27205,
            title = "Inception",
            season = null,
            episode = null
        )
        val json = JSONObject(JSONTokener(streamJson))
        val streams = json.getJSONArray("streams")
        assertEquals(1, streams.length())

        val stream = streams.getJSONObject(0)
        assertEquals("Vidy Player", stream.getString("name"))
        assertTrue(stream.getString("title").contains("Inbuilt Audio & Subs"))
        assertTrue(stream.getString("url").contains("vidy://play?type=movie&tmdb=27205"))
    }

    @Test
    fun testBuildStreamResponse_constructsPlayIntentUriForSeries() {
        val streamJson: String = LocalAddonServer.buildStreamResponse(
            type = "series",
            tmdbId = 1396,
            title = "Breaking Bad",
            season = 2,
            episode = 4
        )
        val json = JSONObject(JSONTokener(streamJson))
        val streams = json.getJSONArray("streams")
        assertEquals(1, streams.length())

        val stream = streams.getJSONObject(0)
        assertEquals("Vidy Player", stream.getString("name"))
        assertTrue(stream.getString("url").contains("vidy://play?type=tv&tmdb=1396&season=2&episode=4"))
    }
}
