package dev.cinderhell.session

import java.io.File

internal class AppPaths(root: File) {
    val root: File = root.canonicalFile
    val content = this.root.resolve("content/sha256")
    val importTasks = this.root.resolve("imports")
    val sessions = this.root.resolve("sessions")
    val pendingSessions = sessions.resolve("pending")
    val activeSessions = sessions.resolve("active")
    val sessionResults = sessions.resolve("results")
    val consumedResults = sessions.resolve("consumed")
    val diagnostics = this.root.resolve("diagnostics")
    val recentStates = this.root.resolve("recent-state")
    val runtime = this.root.resolve("runtime")
    val configs = this.root.resolve("configs")
    val saves = this.root.resolve("saves")
    val screenshots = this.root.resolve("screenshots")

    fun ensureDirectories() {
        listOf(
            content,
            importTasks,
            pendingSessions,
            activeSessions,
            sessionResults,
            consumedResults,
            diagnostics,
            recentStates,
            runtime,
            configs,
            saves,
            screenshots,
        ).forEach { directory ->
            check(directory.isDirectory || directory.mkdirs()) {
                "Could not create private application directory: ${directory.name}"
            }
        }
    }

    fun profileConfig(profileId: String): File =
        configs.resolve(safeSegment(profileId)).resolve("woof.cfg")

    fun profileSaves(profileId: String): File =
        saves.resolve(safeSegment(profileId))

    fun profileScreenshots(profileId: String): File =
        screenshots.resolve(safeSegment(profileId))

    fun isPrivatePath(file: File): Boolean {
        val candidate = file.canonicalFile.toPath()
        return candidate.startsWith(root.toPath()) && candidate != root.toPath()
    }

    fun pendingSession(sessionId: String): File =
        pendingSessions.resolve("${safeSessionId(sessionId)}.json")

    fun activeSession(sessionId: String): File =
        activeSessions.resolve("${safeSessionId(sessionId)}.json")

    fun sessionResult(sessionId: String): File =
        sessionResults.resolve("${safeSessionId(sessionId)}.json")

    fun recentState(profileId: String): File =
        recentStates.resolve("${safeSegment(profileId)}.json")

    companion object {
        private val safeSegmentPattern = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{0,95}")
        private val sessionIdPattern = Regex("[0-9a-f]{32}")

        fun safeSegment(value: String): String {
            require(safeSegmentPattern.matches(value)) { "Unsafe application path segment" }
            return value
        }

        fun safeSessionId(value: String): String {
            require(sessionIdPattern.matches(value)) { "Invalid session identifier" }
            return value
        }
    }
}
