package dev.cinderhell.session

import android.content.Context
import dev.cinderhell.library.BundledLibraryBootstrap
import dev.cinderhell.library.LibraryRepository
import dev.cinderhell.library.LibraryRecovery
import dev.cinderhell.library.ProfileEntryKind
import dev.cinderhell.library.RecentSessionEntity
import dev.cinderhell.profile.ProfilePreflight
import dev.cinderhell.profile.ProfilePreflightResult

internal class BundledSessionCoordinator(context: Context) {
    private val applicationContext = context.applicationContext
    private val paths = AppPaths(applicationContext.filesDir)
    private val diagnostics = DiagnosticStore(paths)
    private val library = LibraryRepository(applicationContext)
    private val libraryRecovery = LibraryRecovery(applicationContext)
    private val bundledLibrary = BundledLibraryBootstrap(applicationContext)
    private val preflight = ProfilePreflight(applicationContext, library)
    val store = SessionStore(paths)

    suspend fun ensureBundledLibrary() {
        libraryRecovery.recover()
        bundledLibrary.ensureInstalled()
    }

    suspend fun createFreedoomSession(): GameSessionDescriptor {
        return createSelectedSession()
    }

    suspend fun createSelectedSession(
        launchMode: LaunchMode = LaunchMode.NORMAL,
        loadGameSlot: Int? = null,
    ): GameSessionDescriptor {
        check(!GameProcessGate.isRunning(applicationContext)) {
            "The previous game process is still closing."
        }
        ensureBundledLibrary()
        val selected = requireNotNull(library.getSelectedProfile()) {
            "Choose a profile before pressing Play."
        }
        return createProfileSession(
            profileId = selected.profile.profileId,
            launchMode = launchMode,
            loadGameSlot = loadGameSlot,
        )
    }

    suspend fun createContinueSession(): GameSessionDescriptor {
        val summary = requireNotNull(continueSummary()) {
            "There is no current save to continue."
        }
        check(!GameProcessGate.isRunning(applicationContext)) {
            "The previous game process is still closing."
        }
        return createProfileSession(
            profileId = summary.profileId,
            launchMode = LaunchMode.RESUME_SAVE,
            loadGameSlot = summary.saveSlot,
        )
    }

    suspend fun continueSummary(): ContinueSummary? {
        ensureBundledLibrary()
        val recent = library.getLatestSession() ?: return null
        val profile = library.getProfile(recent.profileId) ?: return null
        val state = RecentStateStore(paths).read(recent.profileId) ?: return null
        if (!ContinueEligibility.matches(recent, profile.profile, state)) {
            return null
        }
        val preflightResult = preflight.check(profile.profile.profileId)
        if (preflightResult !is ProfilePreflightResult.Ready) return null
        return ContinueSummary(
            profileId = profile.profile.profileId,
            profileName = profile.profile.name,
            gameName = preflightResult.profile.game.displayName,
            latestLevel = recent.latestLevel ?: formatLevel(
                preflightResult.profile.game.gameIdentity,
                state.episode,
                state.map,
            ),
            saveSlot = state.saveSlot,
        )
    }

    private suspend fun createProfileSession(
        profileId: String,
        launchMode: LaunchMode,
        loadGameSlot: Int?,
    ): GameSessionDescriptor {
        val prepared = when (val result = preflight.check(profileId)) {
            is ProfilePreflightResult.Ready -> result.profile
            is ProfilePreflightResult.Blocked -> {
                throw IllegalStateException(result.reasons.joinToString("\n"))
            }
        }
        if (launchMode == LaunchMode.RESUME_SAVE) {
            val state = RecentStateStore(paths).read(profileId)
            check(state != null && state.saveSlot == loadGameSlot) {
                "The recent save is no longer usable."
            }
        }
        val config = paths.profileConfig(profileId)
        val saves = paths.profileSaves(profileId)
        val screenshots = paths.profileScreenshots(profileId)
        listOf(checkNotNull(config.parentFile), saves, screenshots).forEach {
            check(it.isDirectory || it.mkdirs())
        }

        val descriptor = store.createPending(
            SessionRequest(
                profileId = profileId,
                presetVersion = prepared.record.profile.presetVersion,
                orderedContent = buildList {
                    add(
                    SessionContent(
                        contentId = prepared.game.contentId,
                        role = ContentRole.GAME,
                        path = prepared.game.blobPath,
                        sha256 = prepared.game.sha256,
                    ),
                    )
                    prepared.record.orderedEntries.forEach { entry ->
                        add(
                            SessionContent(
                                contentId = entry.content.contentId,
                                role = if (entry.entry.kind == ProfileEntryKind.PATCH) {
                                    ContentRole.PATCH
                                } else {
                                    ContentRole.MOD
                                },
                                path = entry.content.blobPath,
                                sha256 = entry.content.sha256,
                            ),
                        )
                    }
                },
                configPath = config.canonicalPath,
                saveDirectory = saves.canonicalPath,
                screenshotDirectory = screenshots.canonicalPath,
                options = EngineLaunchOptions(
                    mode = launchMode,
                    targetRefreshRate = prepared.preset.targetRefreshRate,
                    compatibility = prepared.preset.compatibility,
                    loadGameSlot = loadGameSlot,
                ),
            ),
        )
        library.recordRecentSession(
            RecentSessionEntity(
                recentSessionId = descriptor.sessionId,
                profileId = profileId,
                startedAtEpochMillis = descriptor.createdAtEpochMillis,
                endedAtEpochMillis = null,
                result = null,
                latestLevel = null,
                resumableStatePath = null,
            ),
        )
        return descriptor
    }

    suspend fun recoverAndConsumeResults(): List<GameSessionResult> {
        store.cleanupTemporaryAndExpiredFiles()
        store.recoverOrphanedActiveSessions(
            gameProcessRunning = GameProcessGate.isRunning(applicationContext),
        )
        return store.consumeResults().also { results ->
            results.filter { it.outcome != SessionOutcome.CLEAN_EXIT }
                .forEach(diagnostics::record)
            results.forEach { result ->
                val profileId = result.profileId ?: return@forEach
                val current = library.getRecentSession(result.sessionId) ?: return@forEach
                val profile = library.getProfile(profileId)
                val state = RecentStateStore(paths).read(profileId)
                    ?.takeIf {
                        it.sessionId == result.sessionId &&
                            it.gameContentId == profile?.profile?.gameContentId
                    }
                val game = profile?.let {
                    library.getContent(it.profile.gameContentId)
                }
                library.recordRecentSession(
                    current.copy(
                        endedAtEpochMillis = result.completedAtEpochMillis,
                        result = result.outcome.name,
                        latestLevel = state?.let {
                            formatLevel(game?.gameIdentity, it.episode, it.map)
                        },
                        resumableStatePath = state?.savePath,
                    ),
                )
            }
        }
    }

    fun cancelPendingSession(descriptor: GameSessionDescriptor, message: String) {
        store.recordStartupFailure(
            sessionId = descriptor.sessionId,
            nonce = descriptor.nonce,
            profileId = descriptor.profileId,
            code = SessionFailureCode.NATIVE_STARTUP_FAILURE,
            userMessage = message,
        )
    }

    companion object {
        const val SESSION_ID_EXTRA = "dev.cinderhell.extra.SESSION_ID"

        private fun formatLevel(
            identity: dev.cinderhell.library.GameIdentity?,
            episode: Int,
            map: Int,
        ): String =
            if (identity == dev.cinderhell.library.GameIdentity.DOOM ||
                identity == dev.cinderhell.library.GameIdentity.FREEDOOM_PHASE1
            ) {
                "E${episode}M$map"
            } else {
                "MAP${map.toString().padStart(2, '0')}"
            }
    }
}

internal data class ContinueSummary(
    val profileId: String,
    val profileName: String,
    val gameName: String,
    val latestLevel: String,
    val saveSlot: Int,
)

internal object ContinueEligibility {
    fun matches(
        recent: RecentSessionEntity,
        profile: dev.cinderhell.library.ProfileEntity,
        state: RecentStateRecord,
    ): Boolean =
        recent.profileId == profile.profileId &&
            state.profileId == profile.profileId &&
            recent.resumableStatePath != null &&
            state.sessionId == recent.recentSessionId &&
            state.gameContentId == profile.gameContentId &&
            state.savePath == recent.resumableStatePath
}
