package dev.cinderhell.session

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class DiagnosticStore(
    private val paths: AppPaths,
    private val json: Json = Json { encodeDefaults = true },
) {
    private val latest = paths.diagnostics.resolve("latest.json")

    fun record(result: GameSessionResult) {
        val code = result.failureCode ?: return
        val diagnostic = DiagnosticRecord(
            recordedAtEpochMillis = result.completedAtEpochMillis,
            sessionId = result.sessionId,
            failureCode = code,
            userMessage = result.userMessage
                ?: "The game stopped unexpectedly. Your library and saves are safe.",
        )
        AtomicFiles.writeUtf8(latest, json.encodeToString(diagnostic))
    }

    fun readLatest(): DiagnosticRecord? =
        if (!latest.isFile) {
            null
        } else {
            runCatching {
                json.decodeFromString(DiagnosticRecord.serializer(), latest.readText())
            }.getOrNull()
        }
}
