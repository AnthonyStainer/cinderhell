package dev.cinderhell.session

import kotlinx.serialization.Serializable

internal const val SESSION_SCHEMA_VERSION = 1
internal const val SESSION_TTL_MILLIS = 5 * 60 * 1000L

@Serializable
internal enum class ContentRole {
    GAME,
    MOD,
    PATCH,
}

@Serializable
internal data class SessionContent(
    val contentId: String,
    val role: ContentRole,
    val path: String,
    val sha256: String,
)

@Serializable
internal enum class LaunchMode {
    NORMAL,
    RESUME_SAVE,
}

@Serializable
internal data class EngineLaunchOptions(
    val mode: LaunchMode = LaunchMode.NORMAL,
    val targetRefreshRate: Int = 120,
    val skill: Int? = null,
    val warp: String? = null,
    val compatibility: String? = null,
    val loadGameSlot: Int? = null,
)

@Serializable
internal data class GameSessionDescriptor(
    val schemaVersion: Int = SESSION_SCHEMA_VERSION,
    val sessionId: String,
    val nonce: String,
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    val profileId: String,
    val presetVersion: Int,
    val orderedContent: List<SessionContent>,
    val configPath: String,
    val saveDirectory: String,
    val screenshotDirectory: String,
    val options: EngineLaunchOptions,
)

internal data class SessionRequest(
    val profileId: String,
    val presetVersion: Int,
    val orderedContent: List<SessionContent>,
    val configPath: String,
    val saveDirectory: String,
    val screenshotDirectory: String,
    val options: EngineLaunchOptions = EngineLaunchOptions(),
)

@Serializable
internal enum class SessionOutcome {
    CLEAN_EXIT,
    STARTUP_FAILURE,
    INTERRUPTED,
}

@Serializable
internal enum class SessionFailureCode {
    INVALID_REQUEST,
    EXPIRED_REQUEST,
    MISSING_CONTENT,
    RUNTIME_ASSET_FAILURE,
    NATIVE_STARTUP_FAILURE,
    PROCESS_LOST,
}

@Serializable
internal data class GameSessionResult(
    val schemaVersion: Int = SESSION_SCHEMA_VERSION,
    val sessionId: String,
    val nonce: String,
    val profileId: String? = null,
    val completedAtEpochMillis: Long,
    val outcome: SessionOutcome,
    val failureCode: SessionFailureCode? = null,
    val userMessage: String? = null,
)

@Serializable
internal data class RecentStateRecord(
    val schemaVersion: Int = SESSION_SCHEMA_VERSION,
    val sessionId: String,
    val profileId: String,
    val gameContentId: String,
    val saveSlot: Int,
    val savePath: String,
    val saveByteSize: Long,
    val saveLastModifiedEpochMillis: Long,
    val episode: Int,
    val map: Int,
    val recordedAtEpochMillis: Long,
)

@Serializable
internal data class DiagnosticRecord(
    val schemaVersion: Int = SESSION_SCHEMA_VERSION,
    val recordedAtEpochMillis: Long,
    val sessionId: String,
    val failureCode: SessionFailureCode,
    val userMessage: String,
)

internal class SessionValidationException(
    val code: SessionFailureCode,
    message: String,
) : IllegalStateException(message)
