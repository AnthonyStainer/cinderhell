package dev.cinderhell.session

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentStateStoreTest {
    private val root = Files.createTempDirectory("cinderhell-recent").toFile()
    private val paths = AppPaths(root).also(AppPaths::ensureDirectories)

    @Test
    fun newestManualSaveIsCapturedAndInvalidatedWhenBytesChange() {
        val descriptor = descriptor()
        val saves = paths.profileSaves(descriptor.profileId).also { it.mkdirs() }
        saves.resolve("woofsav0.dsg").writeBytes(saveBytes(episode = 1, map = 3))
        val newest = saves.resolve("woofsav2.dsg").apply {
            writeBytes(saveBytes(episode = 2, map = 7))
            setLastModified(System.currentTimeMillis() + 1_000)
        }

        val store = RecentStateStore(paths, now = { 100 })
        val captured = checkNotNull(store.capture(descriptor))
        assertEquals(2, captured.saveSlot)
        assertEquals(2, captured.episode)
        assertEquals(7, captured.map)
        assertEquals(newest.canonicalPath, captured.savePath)
        assertEquals(captured, store.read(descriptor.profileId))

        newest.appendBytes(byteArrayOf(1))
        assertNull(store.read(descriptor.profileId))
    }

    @Test
    fun noManualSaveProducesNoContinueState() {
        paths.profileSaves("profile-one").mkdirs()
        paths.profileSaves("profile-one").resolve("autosave.dsg")
            .writeBytes(saveBytes(1, 1))

        assertNull(RecentStateStore(paths).capture(descriptor()))
        assertTrue(paths.recentState("profile-one").let { !it.exists() })
    }

    private fun descriptor(): GameSessionDescriptor = GameSessionDescriptor(
        sessionId = "a".repeat(32),
        nonce = "nonce",
        profileId = "profile-one",
        createdAtEpochMillis = 1,
        expiresAtEpochMillis = Long.MAX_VALUE,
        presetVersion = 1,
        orderedContent = listOf(
            SessionContent(
                contentId = "game-one",
                role = ContentRole.GAME,
                path = paths.content.resolve("b".repeat(64)).canonicalPath,
                sha256 = "b".repeat(64),
            ),
        ),
        configPath = paths.profileConfig("profile-one").canonicalPath,
        saveDirectory = paths.profileSaves("profile-one").canonicalPath,
        screenshotDirectory = paths.profileScreenshots("profile-one").canonicalPath,
        options = EngineLaunchOptions(),
    )

    private fun saveBytes(episode: Int, map: Int): ByteArray =
        ByteArray(80).also {
            it[43] = episode.toByte()
            it[44] = map.toByte()
        }
}
