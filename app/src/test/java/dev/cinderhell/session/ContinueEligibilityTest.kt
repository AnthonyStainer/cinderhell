package dev.cinderhell.session

import dev.cinderhell.library.ProfileEntity
import dev.cinderhell.library.RecentSessionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinueEligibilityTest {
    private val profile = ProfileEntity(
        profileId = "profile",
        name = "Profile",
        gameContentId = "game",
        presetId = "handheld",
        presetVersion = 1,
        selected = true,
        configPath = "/private/config",
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )
    private val recent = RecentSessionEntity(
        recentSessionId = "a".repeat(32),
        profileId = "profile",
        startedAtEpochMillis = 1,
        endedAtEpochMillis = 2,
        result = "CLEAN_EXIT",
        latestLevel = "MAP07",
        resumableStatePath = "/private/save",
    )
    private val state = RecentStateRecord(
        sessionId = recent.recentSessionId,
        profileId = "profile",
        gameContentId = "game",
        saveSlot = 2,
        savePath = "/private/save",
        saveByteSize = 10,
        saveLastModifiedEpochMillis = 2,
        episode = 1,
        map = 7,
        recordedAtEpochMillis = 2,
    )

    @Test
    fun exactProfileSessionGameAndSaveAreRequired() {
        assertTrue(ContinueEligibility.matches(recent, profile, state))
        assertFalse(
            ContinueEligibility.matches(
                recent.copy(resumableStatePath = null),
                profile,
                state,
            ),
        )
        assertFalse(
            ContinueEligibility.matches(
                recent,
                profile,
                state.copy(sessionId = "b".repeat(32)),
            ),
        )
        assertFalse(
            ContinueEligibility.matches(
                recent,
                profile,
                state.copy(gameContentId = "other"),
            ),
        )
        assertFalse(
            ContinueEligibility.matches(
                recent,
                profile,
                state.copy(savePath = "/private/other"),
            ),
        )
    }
}
