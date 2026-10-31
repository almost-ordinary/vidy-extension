package com.frametune.extension.vidy.player

import org.junit.Assert.assertEquals
import org.junit.Test

class VidyPlayerUrlTest {

    @Test
    fun testBuildVidyUrl_movie() {
        val url = VidyPlayerActivity.buildVidyUrl(type = "movie", tmdbId = 27205)
        assertEquals("https://vidy.st/movie/27205?color=E50914&autoplay=true", url)
    }

    @Test
    fun testBuildVidyUrl_tvSeriesWithSeasonAndEpisode() {
        val url = VidyPlayerActivity.buildVidyUrl(type = "tv", tmdbId = 1396, season = 2, episode = 4)
        assertEquals(
            "https://vidy.st/tv/1396/2/4?color=E50914&autoplay=true&nextEpisode=true&episodeSelector=true&autoplayNextEpisode=true",
            url
        )
    }

    @Test
    fun testBuildVidyUrl_tvSeriesWithDefaults() {
        val url = VidyPlayerActivity.buildVidyUrl(type = "series", tmdbId = 1396, season = null, episode = null)
        assertEquals(
            "https://vidy.st/tv/1396/1/1?color=E50914&autoplay=true&nextEpisode=true&episodeSelector=true&autoplayNextEpisode=true",
            url
        )
    }
}

