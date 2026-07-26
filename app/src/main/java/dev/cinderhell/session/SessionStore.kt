package dev.cinderhell.session

import java.io.File
import java.security.SecureRandom
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class SessionStore(
    private val paths: AppPaths,
    private val now: () -> Long = System::currentTimeMillis,
    private val secureRandom: SecureRandom = SecureRandom(),
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    },
) {
    init {
        paths.ensureDirectories()
    }

    @Synchronized
    fun createPending(request: SessionRequest): GameSessionDescriptor {
        validateRequest(request)
        check(!hasPendingOrActiveSession()) { "Another game session is already pending or active" }

        val createdAt = now()
        val descriptor = GameSessionDescriptor(
            sessionId = randomHex(16),
            nonce = randomHex(32),
            createdAtEpochMillis = createdAt,
            expiresAtEpochMillis = createdAt + SESSION_TTL_MILLIS,
            profileId = request.profileId,
            presetVersion = request.presetVersion,
            orderedContent = request.orderedContent,
            configPath = request.configPath,
            saveDirectory = request.saveDirectory,
            screenshotDirectory = request.screenshotDirectory,
            options = request.options,
        )
        validateDescriptor(descriptor, requireExistingContent = true)
        AtomicFiles.writeUtf8(
            paths.pendingSession(descriptor.sessionId),
            json.encodeToString(descriptor),
        )
        return descriptor
    }

    @Synchronized
    fun consumePending(sessionId: String): GameSessionDescriptor {
        val safeId = safeSessionIdOrThrow(sessionId)
        val pending = paths.pendingSession(safeId)
        if (!pending.isFile) {
            throw SessionValidationException(
                SessionFailureCode.INVALID_REQUEST,
                "This play request is no longer available.",
            )
        }

        val descriptor = decodeDescriptor(pending)
        validateDescriptor(descriptor, requireExistingContent = true)
        if (descriptor.sessionId != safeId) {
            throw SessionValidationException(
                SessionFailureCode.INVALID_REQUEST,
                "The play request identifier did not match its descriptor.",
            )
        }
        if (now() > descriptor.expiresAtEpochMillis) {
            pending.delete()
            throw SessionValidationException(
                SessionFailureCode.EXPIRED_REQUEST,
                "This play request expired. Please press Play again.",
            )
        }

        try {
            AtomicFiles.moveWithoutReplacing(pending, paths.activeSession(safeId))
        } catch (error: Exception) {
            throw SessionValidationException(
                SessionFailureCode.INVALID_REQUEST,
                "This play request has already been consumed.",
            )
        }
        return descriptor
    }

    @Synchronized
    fun recordResult(result: GameSessionResult) {
        validateResult(result)
        val active = paths.activeSession(result.sessionId)
        if (active.isFile) {
            val descriptor = decodeDescriptor(active)
            check(descriptor.nonce == result.nonce) { "Session result nonce mismatch" }
        }
        AtomicFiles.writeUtf8(
            paths.sessionResult(result.sessionId),
            json.encodeToString(result),
        )
        active.delete()
    }

    @Synchronized
    fun recordStartupFailure(
        sessionId: String,
        nonce: String,
        profileId: String? = null,
        code: SessionFailureCode,
        userMessage: String,
    ) {
        val safeId = safeSessionIdOrThrow(sessionId)
        val result = GameSessionResult(
            sessionId = safeId,
            nonce = nonce.takeIf { NONCE_PATTERN.matches(it) } ?: "",
            profileId = profileId,
            completedAtEpochMillis = now(),
            outcome = SessionOutcome.STARTUP_FAILURE,
            failureCode = code,
            userMessage = userMessage.take(240),
        )
        AtomicFiles.writeUtf8(paths.sessionResult(safeId), json.encodeToString(result))
        paths.pendingSession(safeId).delete()
        paths.activeSession(safeId).delete()
    }

    @Synchronized
    fun consumeResults(): List<GameSessionResult> {
        val results = mutableListOf<GameSessionResult>()
        paths.sessionResults.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "json" }
            .sortedBy { it.name }
            .forEach { resultFile ->
                val sessionId = resultFile.nameWithoutExtension
                val consuming = paths.consumedResults.resolve(
                    "$sessionId.${randomHex(8)}.json",
                )
                try {
                    AtomicFiles.moveWithoutReplacing(resultFile, consuming)
                    val result = decodeResult(consuming)
                    validateResult(result)
                    results += result
                } finally {
                    consuming.delete()
                }
            }
        return results
    }

    @Synchronized
    fun hasPendingOrActiveSession(): Boolean =
        paths.pendingSessions.hasJsonFiles() || paths.activeSessions.hasJsonFiles()

    @Synchronized
    fun recoverOrphanedActiveSessions(gameProcessRunning: Boolean) {
        if (gameProcessRunning) return
        paths.activeSessions.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "json" }
            .forEach { active ->
                val descriptor = runCatching { decodeDescriptor(active) }.getOrNull()
                if (descriptor == null) {
                    active.delete()
                } else {
                    recordResult(
                        GameSessionResult(
                            sessionId = descriptor.sessionId,
                            nonce = descriptor.nonce,
                            profileId = descriptor.profileId,
                            completedAtEpochMillis = now(),
                            outcome = SessionOutcome.INTERRUPTED,
                            failureCode = SessionFailureCode.PROCESS_LOST,
                            userMessage = "The previous game session stopped unexpectedly. Your saves are still available.",
                        ),
                    )
                }
            }
    }

    @Synchronized
    fun cleanupTemporaryAndExpiredFiles() {
        val cutoff = now() - SESSION_TTL_MILLIS
        listOf(
            paths.pendingSessions,
            paths.activeSessions,
            paths.sessionResults,
            paths.consumedResults,
            paths.importTasks,
        ).forEach { directory ->
            directory.listFiles()
                .orEmpty()
                .filter { it.isFile && it.name.endsWith(".part") && it.lastModified() < cutoff }
                .forEach(File::delete)
        }

        paths.pendingSessions.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "json" }
            .forEach { pending ->
                val descriptor = runCatching { decodeDescriptor(pending) }.getOrNull()
                if (descriptor == null || now() > descriptor.expiresAtEpochMillis) {
                    pending.delete()
                }
            }
    }

    private fun validateRequest(request: SessionRequest) {
        AppPaths.safeSegment(request.profileId)
        require(request.presetVersion > 0) { "Preset version must be positive" }
        require(request.orderedContent.count { it.role == ContentRole.GAME } == 1) {
            "A session must contain exactly one game"
        }
        validateOptions(request.options)
        request.orderedContent.forEach(::validateContent)
        validatePrivateFile(request.configPath, mustExist = false)
        validatePrivateDirectory(request.saveDirectory)
        validatePrivateDirectory(request.screenshotDirectory)
    }

    private fun validateDescriptor(
        descriptor: GameSessionDescriptor,
        requireExistingContent: Boolean,
    ) {
        if (descriptor.schemaVersion != SESSION_SCHEMA_VERSION) invalid("Unsupported session version")
        safeSessionIdOrThrow(descriptor.sessionId)
        if (!NONCE_PATTERN.matches(descriptor.nonce)) invalid("Invalid session nonce")
        if (descriptor.createdAtEpochMillis <= 0L ||
            descriptor.expiresAtEpochMillis <= descriptor.createdAtEpochMillis ||
            descriptor.expiresAtEpochMillis - descriptor.createdAtEpochMillis > SESSION_TTL_MILLIS
        ) {
            invalid("Invalid session lifetime")
        }
        AppPaths.safeSegment(descriptor.profileId)
        if (descriptor.presetVersion <= 0) invalid("Invalid preset version")
        if (descriptor.orderedContent.count { it.role == ContentRole.GAME } != 1) {
            invalid("A session must contain exactly one game")
        }
        descriptor.orderedContent.forEach { content ->
            validateContent(content)
            validatePrivateFile(content.path, requireExistingContent)
        }
        validatePrivateFile(descriptor.configPath, mustExist = false)
        validatePrivateDirectory(descriptor.saveDirectory)
        validatePrivateDirectory(descriptor.screenshotDirectory)
        validateOptions(descriptor.options)
    }

    private fun validateContent(content: SessionContent) {
        AppPaths.safeSegment(content.contentId)
        if (!SHA256_PATTERN.matches(content.sha256)) invalid("Invalid content digest")
        validatePrivateFile(content.path, mustExist = true)
    }

    private fun validateOptions(options: EngineLaunchOptions) {
        if (options.skill != null && options.skill !in 1..5) invalid("Invalid skill")
        if (options.targetRefreshRate !in REFRESH_RATE_ALLOWLIST) {
            invalid("Unsupported refresh target")
        }
        if (options.warp != null && !WARP_PATTERN.matches(options.warp)) invalid("Invalid warp")
        if (options.compatibility != null &&
            options.compatibility !in COMPATIBILITY_ALLOWLIST
        ) {
            invalid("Unsupported compatibility mode")
        }
        if (options.loadGameSlot != null && options.loadGameSlot !in 0..79) {
            invalid("Invalid save slot")
        }
        if (options.mode == LaunchMode.RESUME_SAVE && options.loadGameSlot == null) {
            invalid("Resume mode requires a save slot")
        }
        if (options.mode == LaunchMode.NORMAL && options.loadGameSlot != null) {
            invalid("Normal mode cannot load a save slot")
        }
    }

    private fun validatePrivateFile(path: String, mustExist: Boolean) {
        val file = File(path)
        if (!file.isAbsolute || !paths.isPrivatePath(file)) invalid("Path is outside private storage")
        if (mustExist && !file.isFile) {
            throw SessionValidationException(
                SessionFailureCode.MISSING_CONTENT,
                "Required game content is missing.",
            )
        }
    }

    private fun validatePrivateDirectory(path: String) {
        val directory = File(path)
        if (!directory.isAbsolute || !paths.isPrivatePath(directory)) {
            invalid("Directory is outside private storage")
        }
        if (!directory.isDirectory && !directory.mkdirs()) invalid("Private directory is unavailable")
    }

    private fun validateResult(result: GameSessionResult) {
        if (result.schemaVersion != SESSION_SCHEMA_VERSION) invalid("Unsupported result version")
        safeSessionIdOrThrow(result.sessionId)
        if (result.nonce.isNotEmpty() && !NONCE_PATTERN.matches(result.nonce)) {
            invalid("Invalid result nonce")
        }
        result.profileId?.let {
            runCatching { AppPaths.safeSegment(it) }
                .getOrElse { invalid("Invalid result profile") }
        }
        if (result.completedAtEpochMillis <= 0L) invalid("Invalid result time")
        if (result.outcome == SessionOutcome.CLEAN_EXIT && result.failureCode != null) {
            invalid("Clean results cannot contain failure codes")
        }
    }

    private fun decodeDescriptor(file: File): GameSessionDescriptor =
        try {
            json.decodeFromString(GameSessionDescriptor.serializer(), file.readText())
        } catch (error: SerializationException) {
            throw SessionValidationException(
                SessionFailureCode.INVALID_REQUEST,
                "The play request could not be read.",
            )
        }

    private fun decodeResult(file: File): GameSessionResult =
        try {
            json.decodeFromString(GameSessionResult.serializer(), file.readText())
        } catch (error: SerializationException) {
            throw SessionValidationException(
                SessionFailureCode.INVALID_REQUEST,
                "The game result could not be read.",
            )
        }

    private fun randomHex(byteCount: Int): String =
        ByteArray(byteCount).also(secureRandom::nextBytes).toHex()

    private fun safeSessionIdOrThrow(value: String): String =
        try {
            AppPaths.safeSessionId(value)
        } catch (_: IllegalArgumentException) {
            throw SessionValidationException(
                SessionFailureCode.INVALID_REQUEST,
                "The play request was invalid.",
            )
        }

    private fun invalid(message: String): Nothing =
        throw SessionValidationException(SessionFailureCode.INVALID_REQUEST, message)

    private fun File.hasJsonFiles(): Boolean =
        listFiles().orEmpty().any { it.isFile && it.extension == "json" }

    private fun ByteArray.toHex(): String = joinToString(separator = "") {
        (it.toInt() and 0xff).toString(16).padStart(2, '0')
    }

    private companion object {
        val NONCE_PATTERN = Regex("[0-9a-f]{64}")
        val SHA256_PATTERN = Regex("[0-9a-f]{64}")
        val WARP_PATTERN = Regex("(E[1-9]M[1-9]|MAP[0-9]{2})")
        val COMPATIBILITY_ALLOWLIST = setOf(
            "vanilla",
            "limitremoving",
            "boom",
            "mbf",
            "mbf21",
        )
        val REFRESH_RATE_ALLOWLIST = setOf(60, 90, 120, 144, 165)
    }
}
