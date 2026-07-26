package dev.cinderhell.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StickNavigationTranslatorTest {
    @Test
    fun stickUsesDominantAxisAndDebouncedRepeats() {
        var now = 1_000L
        val translator = StickNavigationTranslator(clock = { now })

        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, translator.translateAxes(0.8f, 0.2f))
        assertNull(translator.translateAxes(0.9f, 0f))
        now += 181
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, translator.translateAxes(0.9f, 0f))
        assertNull(translator.translateAxes(0f, 0f))
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, translator.translateAxes(0.1f, -0.8f))
    }
}
