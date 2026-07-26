package dev.cinderhell.profile

import dev.cinderhell.library.ProfileEntity
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileConfigServiceTest {
    private val root = Files.createTempDirectory("cinderhell-profile-config").toFile()
    private val service = ProfileConfigService()

    @Test
    fun materializationIsIdempotentAndPreservesInGameEdits() {
        val profile = profile()
        val config = service.ensureMaterialized(profile, ProfilePresets.handheld)
        assertTrue(config.readText().contains("fpslimit 120"))

        config.appendText("user_custom_value 77\n")
        val edited = config.readText()
        service.ensureMaterialized(profile, ProfilePresets.handheld)

        assertEquals(edited, config.readText())
    }

    @Test
    fun explicitReapplyPreviewsAndOnlyChangesCuratedKeys() {
        val profile = profile()
        val config = service.ensureMaterialized(profile, ProfilePresets.original)
        config.writeText(
            """
            # retained comment
            widescreen 0
            fpslimit 35
            user_custom_value 77
            """.trimIndent() + "\n",
        )

        val preview = service.previewReapply(profile, ProfilePresets.enhanced)
        assertTrue(preview.changes.any { it.key == "widescreen" && it.presetValue == 1 })
        assertTrue(preview.changes.any { it.key == "fpslimit" && it.presetValue == 0 })

        service.applyPreview(preview)
        val updated = config.readText()
        assertTrue(updated.contains("# retained comment"))
        assertTrue(updated.contains("user_custom_value 77"))
        assertTrue(updated.contains("widescreen 1"))
        assertTrue(updated.contains("fpslimit 0"))
        assertFalse(updated.contains("widescreen 0"))
    }

    @Test
    fun reapplyRejectsAStalePreview() {
        val profile = profile()
        val config = service.ensureMaterialized(profile, ProfilePresets.original)
        val preview = service.previewReapply(profile, ProfilePresets.handheld)
        config.appendText("# changed in game\n")

        assertThrows(IllegalStateException::class.java) {
            service.applyPreview(preview)
        }
    }

    private fun profile(): ProfileEntity = ProfileEntity(
        profileId = "profile-one",
        name = "Test profile",
        gameContentId = "game",
        presetId = PresetId.HANDHELD.wireValue,
        presetVersion = ProfilePresets.VERSION,
        selected = true,
        configPath = File(root, "configs/profile-one/woof.cfg").canonicalPath,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )
}
