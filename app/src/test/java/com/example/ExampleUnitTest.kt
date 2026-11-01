package com.example

import com.example.model.MediaState
import com.example.model.MirrorStreamConfig
import com.example.model.StreamResolution
import com.example.model.WatchEcosystemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `media state volume calculation`() {
        val state = MediaState(currentVolume = 12, maxVolume = 15)
        assertEquals(80, state.volumePercent)
    }

    @Test
    fun `stream config default values`() {
        val config = MirrorStreamConfig()
        assertEquals(StreamResolution.STANDARD, config.resolution)
        assertEquals(10, config.targetFps)
        assertFalse(config.isGrayscale)
        assertTrue(config.pauseOnLockScreen)
    }
}
