package com.frametune.extension.vidy.player

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VidyProgressStoreTest {

    private lateinit var store: VidyProgressStore

    @Before
    fun setUp() {
        store = VidyProgressStore()
    }

    @Test
    fun testBuildContentKey() {
        val movieKey = VidyProgressStore.buildContentKey("movie", 550, null, null)
        assertEquals("movie_550", movieKey)

        val seriesKey = VidyProgressStore.buildContentKey("tv", 1399, 2, 4)
        assertEquals("tv_1399_2_4", seriesKey)

        val seriesDefaultKey = VidyProgressStore.buildContentKey("series", 1399, null, null)
        assertEquals("tv_1399_1_1", seriesDefaultKey)
    }

    @Test
    fun testSaveAndRetrieveProgress() {
        val key = VidyProgressStore.buildContentKey("movie", 550, null, null)
        val saved = store.saveProgress(key, positionSec = 300L, durationSec = 7200L)
        assertTrue(saved)

        val record = store.getProgress(key)
        assertNotNull(record)
        assertEquals(300L, record?.positionSeconds)
        assertEquals(7200L, record?.durationSeconds)
    }

    @Test
    fun testIgnorePositionUnder15Seconds() {
        val key = VidyProgressStore.buildContentKey("movie", 550, null, null)
        val saved = store.saveProgress(key, positionSec = 14L, durationSec = 7200L)
        assertFalse(saved)
        assertNull(store.getProgress(key))
    }

    @Test
    fun testClearProgressNearEnd() {
        val key = VidyProgressStore.buildContentKey("movie", 550, null, null)
        store.saveProgress(key, positionSec = 3000L, durationSec = 7200L)
        assertNotNull(store.getProgress(key))

        // Within 60s of end (duration 7200, pos 7150)
        val saved = store.saveProgress(key, positionSec = 7150L, durationSec = 7200L)
        assertFalse(saved)
        assertNull("Near end stream should be cleared", store.getProgress(key))
    }

    @Test
    fun testFormatTime() {
        assertEquals("00:45", VidyProgressStore.formatTime(45L))
        assertEquals("12:30", VidyProgressStore.formatTime(750L))
        assertEquals("01:30:00", VidyProgressStore.formatTime(5400L))
    }
}

