package dev.cinderhell.session

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionProtocolInstrumentedTest {
    private val roots = mutableListOf<File>()

    @After
    fun tearDown() {
        roots.forEach(File::deleteRecursively)
    }

    @Test
    fun repeatedLaunchAndQuitCyclesUseFreshSingleConsumptionEnvelopes() {
        val fixture = fixture()
        val sessionIds = buildList {
            repeat(3) {
                val descriptor = fixture.store.createPending(fixture.request("profile-$it"))
                add(descriptor.sessionId)
                assertEquals(descriptor, fixture.store.consumePending(descriptor.sessionId))
                fixture.store.recordResult(cleanResult(descriptor))
                assertEquals(
                    SessionOutcome.CLEAN_EXIT,
                    fixture.store.consumeResults().single().outcome,
                )
                assertFalse(fixture.store.hasPendingOrActiveSession())
            }
        }

        assertEquals(3, sessionIds.distinct().size)
        assertTrue(fixture.store.consumeResults().isEmpty())
    }

    @Test
    fun invalidAndReplayedSessionIdsAreRejected() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request("profile-invalid"))

        listOf("", "../active", "not-a-real-session").forEach { invalidId ->
            assertTrue(
                runCatching { fixture.store.consumePending(invalidId) }.exceptionOrNull()
                    is SessionValidationException,
            )
        }

        fixture.store.consumePending(descriptor.sessionId)
        assertTrue(
            runCatching { fixture.store.consumePending(descriptor.sessionId) }.exceptionOrNull()
                is SessionValidationException,
        )
    }

    @Test
    fun nativeStartupFailureProducesOneRecoverableResult() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request("profile-startup"))
        fixture.store.consumePending(descriptor.sessionId)

        fixture.store.recordStartupFailure(
            sessionId = descriptor.sessionId,
            nonce = descriptor.nonce,
            code = SessionFailureCode.NATIVE_STARTUP_FAILURE,
            userMessage = "The game could not start. Your saves are safe.",
        )

        val result = fixture.store.consumeResults().single()
        assertEquals(SessionOutcome.STARTUP_FAILURE, result.outcome)
        assertEquals(SessionFailureCode.NATIVE_STARTUP_FAILURE, result.failureCode)
        assertFalse(fixture.store.hasPendingOrActiveSession())
        assertTrue(fixture.store.consumeResults().isEmpty())
    }

    @Test
    fun processDeathRecoversAnActiveSessionWithoutReusingIt() {
        val fixture = fixture()
        val descriptor = fixture.store.createPending(fixture.request("profile-death"))
        fixture.store.consumePending(descriptor.sessionId)

        fixture.store.recoverOrphanedActiveSessions(gameProcessRunning = false)

        val result = fixture.store.consumeResults().single()
        assertEquals(SessionOutcome.INTERRUPTED, result.outcome)
        assertEquals(SessionFailureCode.PROCESS_LOST, result.failureCode)
        assertFalse(fixture.store.hasPendingOrActiveSession())
    }

    @Test
    fun switchingProfilesBetweenSessionsChangesOnlyTheNewSnapshot() {
        val fixture = fixture()
        val first = fixture.store.createPending(fixture.request("profile-one"))
        fixture.store.consumePending(first.sessionId)
        fixture.store.recordResult(cleanResult(first))
        fixture.store.consumeResults()

        val second = fixture.store.createPending(fixture.request("profile-two"))
        val consumed = fixture.store.consumePending(second.sessionId)

        assertNotEquals(first.sessionId, consumed.sessionId)
        assertEquals("profile-two", consumed.profileId)
        assertTrue(consumed.configPath.contains("/profile-two/"))
        assertEquals("profile-two", File(consumed.saveDirectory).name)
        assertFalse(consumed.configPath.contains("/profile-one/"))
    }

    private fun fixture(): Fixture {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = context.cacheDir.resolve("session-test-${UUID.randomUUID()}").also(roots::add)
        val paths = AppPaths(root)
        paths.ensureDirectories()
        val content = paths.runtime.resolve("freedoom2.wad").apply {
            parentFile?.mkdirs()
            writeText("instrumented IWAD fixture")
        }
        return Fixture(
            paths = paths,
            store = SessionStore(paths),
            content = SessionContent(
                contentId = "freedoom-instrumented",
                role = ContentRole.GAME,
                path = content.canonicalPath,
                sha256 = "b".repeat(64),
            ),
        )
    }

    private fun cleanResult(descriptor: GameSessionDescriptor) = GameSessionResult(
        sessionId = descriptor.sessionId,
        nonce = descriptor.nonce,
        completedAtEpochMillis = System.currentTimeMillis(),
        outcome = SessionOutcome.CLEAN_EXIT,
    )

    private data class Fixture(
        val paths: AppPaths,
        val store: SessionStore,
        val content: SessionContent,
    ) {
        fun request(profileId: String): SessionRequest {
            val config = paths.profileConfig(profileId).also {
                checkNotNull(it.parentFile).mkdirs()
            }
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
