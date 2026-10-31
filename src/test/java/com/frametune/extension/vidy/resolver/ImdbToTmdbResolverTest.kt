package com.frametune.extension.vidy.resolver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImdbToTmdbResolverTest {

    @Test
    fun testParseCinemetaJson_extractsTmdbIdFromMoviedbId() {
        val cinemetaResponse = """
            {
                "meta": {
                    "id": "tt1375666",
                    "type": "movie",
                    "name": "Inception",
                    "year": "2010",
                    "moviedb_id": 27205
                }
            }
        """.trimIndent()

        val tmdbId = ImdbToTmdbResolver.parseTmdbIdFromCinemeta(cinemetaResponse)
        assertEquals(27205, tmdbId)
    }

    @Test
    fun testParseCinemetaJson_extractsTmdbIdFromTmdbIdField() {
        val cinemetaResponse = """
            {
                "meta": {
                    "id": "tt0903747",
                    "type": "series",
                    "name": "Breaking Bad",
                    "year": "2008-2013",
                    "tmdb_id": 1396
                }
            }
        """.trimIndent()

        val tmdbId = ImdbToTmdbResolver.parseTmdbIdFromCinemeta(cinemetaResponse)
        assertEquals(1396, tmdbId)
    }

    @Test
    fun testParseCinemetaJson_handlesNullGracefully() {
        val emptyResponse = "{}"
        val tmdbId = ImdbToTmdbResolver.parseTmdbIdFromCinemeta(emptyResponse)
        assertNull(tmdbId)
    }

    @Test
    fun testParseCinemetaJson_handlesInvalidJsonGracefully() {
        val invalidResponse = "<html>Error 500</html>"
        val tmdbId = ImdbToTmdbResolver.parseTmdbIdFromCinemeta(invalidResponse)
        assertNull(tmdbId)
    }
}
