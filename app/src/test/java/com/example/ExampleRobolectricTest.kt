package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.MediaState
import com.example.model.StreamResolution
import com.example.model.WatchEcosystemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WristMirror", appName)
    }

    @Test
    fun `media state volume percentage calculation`() {
        val state = MediaState(currentVolume = 6, maxVolume = 15)
        assertEquals(40, state.volumePercent)

        val zeroState = MediaState(currentVolume = 0, maxVolume = 15)
        assertEquals(0, zeroState.volumePercent)
    }

    @Test
    fun `stream resolution dimensions verification`() {
        val res = StreamResolution.REDMI_AMOLED
        assertEquals(390, res.width)
        assertEquals(450, res.height)

        val circular = StreamResolution.CIRCULAR_AMOLED
        assertEquals(454, circular.width)
        assertEquals(454, circular.height)
    }
}
