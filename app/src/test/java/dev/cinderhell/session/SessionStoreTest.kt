package dev.cinderhell.session

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun pendingSessionIsConsumedExactlyOnceAndResultIsConsumedExactlyOnce() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request())

        assertEquals(descriptor, fixture.store.consumePending(descriptor.sessionId))
        val replay = runCatching {
            fixture.store.consumePending(descriptor.sessionId)
        }.exceptionOrNull()
        assertTrue(replay is SessionValidationException)

        fixture.store.recordResult(
            cleanResult(descriptor, fixture.now),
        )
        assertEquals(
            listOf(SessionOutcome.CLEAN_EXIT),
            fixture.store.consumeResults().map(GameSessionResult::outcome),
        )
        assertTrue(fixture.store.consumeResults().isEmpty())
        assertFalse(fixture.store.hasPendingOrActiveSession())
    }

    @Test
    fun expiredAndForgedRequestsAreRejected() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request())
        fixture.now += SESSION_TTL_MILLIS + 1

        val expired = runCatching {
            fixture.store.consumePending(descriptor.sessionId)
        }.exceptionOrNull() as SessionValidationException
        assertEquals(SessionFailureCode.EXPIRED_REQUEST, expired.code)

        val outside = temporaryFolder.newFile("outside.wad").apply {
            writeText("not private")
        }
        val forged = fixture.request().copy(
            orderedContent = listOf(
                fixture.content.copy(path = outside.absolutePath),
            ),
        )
        assertTrue(
            runCatching { fixture.store.createPending(forged) }.exceptionOrNull()
                is SessionValidationException,
        )
    }

    @Test
    fun orphanedActiveSessionBecomesARecoverableInterruptedResult() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request())
        fixture.store.consumePending(descriptor.sessionId)

        fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)

        val result = fixture.store.consumeResults().single()
        assertEquals(SessionOutcome.INTERRUPTED, result.outcome)
        assertEquals(SessionFailureCode.PROCESS_LOST, result.failureCode)
        assertFalse(fixture.store.hasPendingOrActiveSession())
    }

    @Test
    fun taskSpecificOldTemporaryFilesAreCleaned() {
        val fixture = fixture()
        val part = fixture.paths.pendingSessions.resolve(".session.part").apply {
            writeText("incomplete")
            setLastModified(fixture.now - SESSION_TTL_MILLIS - 1)
        }

        fixture.store.cleanupTemporaryAndExpiredFiles()

        assertFalse(part.exists())
    }

    private fun fixture(): Fixture {
        val root = temporaryFolder.newFolder("app-${System.nanoTime()}")
        val paths = AppPaths(root)
        paths.ensureDirectories()
        val game = paths.runtime.resolve("freedoom2.wad").apply {
            parentFile?.mkdirs()
            writeText("IWAD fixture")
        }
        var currentTime = 1_700_000_000_000L
        val store = SessionStore(paths, now = { currentTime })
        return Fixture(
            paths = paths,
            store = store,
            content = SessionContent(
                contentId = "freedoom-fixture",
                role = ContentRole.GAME,
                path = game.canonicalPath,
                sha256 = "a".repeat(64),
            ),
            getNow = { currentTime },
            setNow = { currentTime = it },
        )
    }

    private fun cleanResult(
        descriptor: GameSessionDescriptor,
        now: Long,
    ) = GameSessionResult(
        sessionId = descriptor.sessionId,
        nonce = descriptor.nonce,
        completedAtEpochMillis = now,
        outcome = SessionOutcome.CLEAN_EXIT,
    )

    private class Fixture(
        val paths: AppPaths,
        val store: SessionStore,
        val content: SessionContent,
        private val getNow: () -> Long,
        private val setNow: (Long) -> Unit,
    ) {
        var now: Long
            get() = getNow()
            set(value) = setNow(value)

        fun request(profileId: String = "profile-one"): SessionRequest {
            val config = paths.profileConfig(profileId)
            checkNotNull(config.parentFile).mkdirs()
            val saves = paths.profileSaves(profileId).apply(File::mkdirs)
            val screenshots = paths.profileScreenshots(profileId).apply(File::mkdirs)
            return SessionRequest(
                profileId = profileId,
                presetVersion = 1,
                orderedContent = listOf(content),
                configPath = config.canonicalPath,
                saveDirectory = saves.canonicalPath,
                screenshotDirectory = screenshots.canonicalPath,
            )
        }
    }
}
