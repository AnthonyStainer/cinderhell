package dev.cinderhell.session

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class GameArgumentsBuilderTest {
    @Test
    fun buildsOnlyTheFixedSupportedArgumentSetInContentOrder() {
        val descriptor = GameSessionDescriptor(
            sessionId = "1".repeat(32),
            nonce = "2".repeat(64),
            createdAtEpochMillis = 1,
            expiresAtEpochMillis = 2,
            profileId = "profile",
            presetVersion = 1,
            orderedContent = listOf(
                SessionContent("game", ContentRole.GAME, "/private/doom2.wad", "a".repeat(64)),
                SessionContent("maps", ContentRole.MOD, "/private/maps.wad", "b".repeat(64)),
                SessionContent("patch", ContentRole.PATCH, "/private/patch.deh", "c".repeat(64)),
                SessionContent("extras", ContentRole.MOD, "/private/extras.pk3", "d".repeat(64)),
            ),
            configPath = "/private/woof.cfg",
            saveDirectory = "/private/saves",
            screenshotDirectory = "/private/shots",
            options = EngineLaunchOptions(
                skill = 3,
                warp = "MAP07",
                compatibility = "mbf21",
            ),
        )

        assertArrayEquals(
            arrayOf(
                "-iwad", "/private/doom2.wad",
                "-config", "/private/woof.cfg",
                "-save", "/private/saves",
                "-shotdir", "/private/shots",
                "-file", "/private/maps.wad",
                "-deh", "/private/patch.deh",
                "-file", "/private/extras.pk3",
                "-skill", "3",
                "-warp", "7",
                "-complevel", "mbf21",
            ),
            GameArgumentsBuilder.build(descriptor),
        )
    }

    @Test
    fun translatesEpisodeWarpToTwoNumericWoofArguments() {
        val descriptor = GameSessionDescriptor(
            sessionId = "1".repeat(32),
            nonce = "2".repeat(64),
            createdAtEpochMillis = 1,
            expiresAtEpochMillis = 2,
            profileId = "profile",
            presetVersion = 1,
            orderedContent = listOf(
                SessionContent("game", ContentRole.GAME, "/private/doom.wad", "a".repeat(64)),
            ),
            configPath = "/private/woof.cfg",
            saveDirectory = "/private/saves",
            screenshotDirectory = "/private/shots",
            options = EngineLaunchOptions(
                warp = "E2M3",
                compatibility = "vanilla",
            ),
        )

        org.junit.Assert.assertEquals(
            listOf("-warp", "2", "3", "-complevel", "vanilla"),
            GameArgumentsBuilder.build(descriptor).takeLast(5),
        )
    }

    @Test
    fun continueAddsOnlyTheValidatedSaveSlot() {
        val descriptor = GameSessionDescriptor(
            sessionId = "1".repeat(32),
            nonce = "2".repeat(64),
            createdAtEpochMillis = 1,
            expiresAtEpochMillis = 2,
            profileId = "profile",
            presetVersion = 1,
            orderedContent = listOf(
                SessionContent("game", ContentRole.GAME, "/private/doom2.wad", "a".repeat(64)),
            ),
            configPath = "/private/woof.cfg",
            saveDirectory = "/private/saves",
            screenshotDirectory = "/private/shots",
            options = EngineLaunchOptions(
                mode = LaunchMode.RESUME_SAVE,
                loadGameSlot = 4,
            ),
        )

        org.junit.Assert.assertEquals(
            listOf("-loadgame", "4"),
            GameArgumentsBuilder.build(descriptor).takeLast(2),
        )
    }
}
