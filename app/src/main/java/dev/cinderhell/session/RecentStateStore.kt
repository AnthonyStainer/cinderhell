package dev.cinderhell.session

import java.io.File
import java.io.RandomAccessFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class RecentStateStore(
    private val paths: AppPaths,
    private val now: () -> Long = System::currentTimeMillis,
    private val json: Json = Json { encodeDefaults = true },
) {
    fun capture(descriptor: GameSessionDescriptor): RecentStateRecord? {
        val latest = File(descriptor.saveDirectory)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && SAVE_PATTERN.matches(it.name) && it.length() > MAP_OFFSET }
            .maxWithOrNull(compareBy<File> { it.lastModified() }.thenBy { it.name })
            ?: return null
        val match = checkNotNull(SAVE_PATTERN.matchEntire(latest.name))
        val slot = checkNotNull(match.groupValues[1].toIntOrNull())
        val episode: Int
        val map: Int
        RandomAccessFile(latest, "r").use { save ->
            save.seek(EPISODE_OFFSET)
            episode = save.readUnsignedByte()
            map = save.readUnsignedByte()
        }
        if (episode !in 0..9 || map !in 1..99) return null
        val record = RecentStateRecord(
            sessionId = descriptor.sessionId,
            profileId = descriptor.profileId,
            gameContentId = descriptor.orderedContent
                .single { it.role == ContentRole.GAME }
                .contentId,
            saveSlot = slot,
            savePath = latest.canonicalPath,
            saveByteSize = latest.length(),
            saveLastModifiedEpochMillis = latest.lastModified(),
            episode = episode,
            map = map,
            recordedAtEpochMillis = now(),
        )
        AtomicFiles.writeUtf8(
            paths.recentState(descriptor.profileId),
            json.encodeToString(record),
        )
        return record
    }

    fun read(profileId: String): RecentStateRecord? {
        val file = paths.recentState(profileId)
        if (!file.isFile) return null
        return runCatching {
            json.decodeFromString(RecentStateRecord.serializer(), file.readText())
        }.getOrNull()?.takeIf(::isValid)
    }

    fun isValid(record: RecentStateRecord): Boolean {
        if (record.schemaVersion != SESSION_SCHEMA_VERSION ||
            record.saveSlot !in 0..79 ||
            record.episode !in 0..9 ||
            record.map !in 1..99
        ) {
            return false
        }
        val save = File(record.savePath)
        return paths.isPrivatePath(save) &&
            save.canonicalFile.parentFile == paths.profileSaves(record.profileId).canonicalFile &&
            save.isFile &&
            save.length() == record.saveByteSize &&
            save.lastModified() == record.saveLastModifiedEpochMillis
    }

    private companion object {
        val SAVE_PATTERN = Regex("(?i).{1,7}sav([0-9]{1,2})\\.dsg")
        const val EPISODE_OFFSET = 43L
        const val MAP_OFFSET = 44L
    }
}
