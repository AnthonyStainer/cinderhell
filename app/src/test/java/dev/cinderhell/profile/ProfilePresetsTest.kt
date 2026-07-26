package dev.cinderhell.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfilePresetsTest {
    @Test
    fun presetsPinThePromisedPresentationAndControllerValues() {
        with(ProfilePresets.original) {
            assertEquals(PresetId.ORIGINAL, id)
            assertEquals(200, settings["current_video_height"])
            assertEquals(0, settings["widescreen"])
            assertEquals(0, settings["uncapped"])
            assertEquals(35, settings["fpslimit"])
            assertEquals(0, settings["freelook"])
            assertEquals("vanilla", compatibility)
        }

        with(ProfilePresets.enhanced) {
            assertEquals(600, settings["current_video_height"])
            assertEquals(1, settings["widescreen"])
            assertEquals(1, settings["uncapped"])
            assertEquals(1, settings["smooth_scaling"])
            assertEquals(1, settings["snd_hrtf"])
            assertEquals(1, settings["freelook"])
            assertNull(compatibility)
        }

        with(ProfilePresets.handheld) {
            assertEquals(400, settings["current_video_height"])
            assertEquals(120, settings["fpslimit"])
            assertEquals(1, settings["autorun"])
            assertEquals(1, settings["joy_stick_layout"])
            assertEquals(240, settings["joy_turn_speed"])
            assertEquals(150, settings["joy_look_speed"])
            assertEquals(15, settings["joy_trigger_deadzone"])
            assertEquals(120, targetRefreshRate)
        }
    }

    @Test
    fun presetLookupRequiresThePinnedVersion() {
        ProfilePresets.all.forEach { preset ->
            assertEquals(
                preset,
                ProfilePresets.require(preset.id.wireValue, ProfilePresets.VERSION),
            )
        }

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            ProfilePresets.require("handheld", ProfilePresets.VERSION + 1)
        }
    }
}
