package dev.cinderhell.ui

import dev.cinderhell.launcher.LauncherSnapshot
import dev.cinderhell.library.ContentItemEntity
import dev.cinderhell.library.ContentType
import dev.cinderhell.library.GameIdentity
import dev.cinderhell.library.ProfileEntity
import dev.cinderhell.library.ProfileWithEntries
import dev.cinderhell.profile.ProfilePresets
import dev.cinderhell.session.ContinueSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherPresentationTest {
    @Test
    fun selectedProfileBecomesACompletePlayHero() {
        val snapshot = fixtureSnapshot(
            continueSummary = ContinueSummary(
                profileId = PROFILE_ID,
                profileName = "Freedoom",
                gameName = "Freedoom: Phase 2",
                latestLevel = "MAP07",
                saveSlot = 0,
            ),
        )

        assertEquals(
            HomePresentation(
                gameName = "Freedoom: Phase 2",
                profileName = "Freedoom",
                presetName = "Handheld",
                modSummary = "No added mods",
                playLabel = "Play Freedoom",
                continueTitle = "Freedoom: Phase 2 — MAP07",
                continueDetail = "Freedoom",
            ),
            snapshot.homePresentation(),
        )
    }

    @Test
    fun missingSelectionDoesNotInventAPlayableHero() {
        assertNull(fixtureSnapshot(selectedProfileId = null).homePresentation())
    }

    @Test
    fun contentMetadataUsesReadableStableLabels() {
        assertEquals("768 KiB", formatContentSize(768L * 1024L))
        assertEquals("2.5 MiB", formatContentSize(2_621_440L))
        assertEquals("Mod archive", contentTypeLabel("MOD_ARCHIVE"))
    }

    @Test
    fun noticesCarryToneWithoutParsingTheirCopy() {
        assertEquals(
            LauncherNoticeTone.SUCCESS,
            LauncherNotice.success("Imported.").tone,
        )
        assertEquals(
            LauncherNoticeTone.ERROR,
            LauncherNotice.error("Import failed.").tone,
        )
    }

    private fun fixtureSnapshot(
        selectedProfileId: String? = PROFILE_ID,
        continueSummary: ContinueSummary? = null,
    ): LauncherSnapshot {
        val game = ContentItemEntity(
            contentId = GAME_ID,
            sha256 = "0".repeat(64),
            displayName = "Freedoom: Phase 2",
            blobPath = "/private/freedoom2.wad",
            byteSize = 30_000_000L,
            contentType = ContentType.GAME_WAD,
            gameIdentity = GameIdentity.FREEDOOM_PHASE2,
            engineRequirements = null,
            importedAtEpochMillis = 1L,
            classificationVersion = 1,
            bundled = true,
        )
        val profile = ProfileEntity(
            profileId = PROFILE_ID,
            name = "Freedoom",
            gameContentId = GAME_ID,
            presetId = ProfilePresets.handheld.id.wireValue,
            presetVersion = ProfilePresets.handheld.version,
            selected = selectedProfileId == PROFILE_ID,
            configPath = "/private/freedoom.cfg",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
        return LauncherSnapshot(
            content = listOf(game),
            profiles = listOf(ProfileWithEntries(profile, emptyList())),
            selectedProfileId = selectedProfileId,
            continueSummary = continueSummary,
        )
    }

    private companion object {
        const val GAME_ID = "freedoom2"
        const val PROFILE_ID = "freedoom-handheld"
    }
}
