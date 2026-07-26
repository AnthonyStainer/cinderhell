package dev.cinderhell.library

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.room.withTransaction
import dev.cinderhell.session.AppPaths
import java.io.File
import kotlinx.coroutines.flow.Flow

internal class LibraryRepository(
    context: Context,
    private val database: LibraryDatabase = LibraryDatabase.get(context),
) {
    private val applicationContext = context.applicationContext
    private val contentItems = database.contentItems()
    private val profiles = database.profiles()
    private val recentSessions = database.recentSessions()

    init {
        assertLauncherWriter()
    }

    fun observeContent(): Flow<List<ContentItemEntity>> = contentItems.observeAll()

    fun observeProfiles(): Flow<List<ProfileWithEntries>> = profiles.observeAllWithEntries()

    fun observeLatestSession(): Flow<RecentSessionEntity?> = recentSessions.observeLatest()

    suspend fun findContentByDigest(sha256: String): ContentItemEntity? =
        contentItems.getByDigest(sha256)

    suspend fun getContent(contentId: String): ContentItemEntity? =
        contentItems.getById(contentId)

    suspend fun getAllContent(): List<ContentItemEntity> = contentItems.getAll()

    suspend fun getAllProfiles(): List<ProfileWithEntries> =
        profiles.getAllWithEntries()

    suspend fun getSelectedProfile(): ProfileWithEntries? =
        profiles.getSelected()?.let { profiles.getWithEntries(it.profileId) }

    suspend fun getProfile(profileId: String): ProfileWithEntries? =
        profiles.getWithEntries(profileId)

    suspend fun insertContent(item: ContentItemEntity): Boolean {
        assertLauncherWriter()
        return contentItems.insert(item) != -1L
    }

    suspend fun saveProfile(
        profile: ProfileEntity,
        entries: List<ProfileEntryEntity>,
    ) {
        assertLauncherWriter()
        require(entries.map { it.loadPosition } == entries.indices.toList()) {
            "Profile entries must use contiguous deterministic load positions"
        }
        require(entries.all { it.profileId == profile.profileId }) {
            "Profile entry belongs to another profile"
        }
        require(entries.map { it.contentId }.distinct().size == entries.size) {
            "A profile cannot load the same content more than once"
        }

        database.withTransaction {
            if (profile.selected) profiles.clearSelection()
            val existing = profiles.getWithEntries(profile.profileId)
            if (existing == null) {
                profiles.insert(profile)
            } else {
                profiles.update(profile)
                profiles.deleteEntries(profile.profileId)
            }
            if (entries.isNotEmpty()) profiles.insertEntries(entries)
        }
    }

    suspend fun selectProfile(profileId: String) {
        assertLauncherWriter()
        database.withTransaction {
            val selected = requireNotNull(profiles.getWithEntries(profileId)) {
                "The selected profile is no longer available."
            }
            profiles.clearSelection()
            profiles.update(
                selected.profile.copy(
                    selected = true,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun deleteProfile(profileId: String) {
        assertLauncherWriter()
        database.withTransaction {
            val current = profiles.getWithEntries(profileId) ?: return@withTransaction
            profiles.deleteProfiles(listOf(profileId))
            if (current.profile.selected) {
                profiles.getAllWithEntries().firstOrNull()?.let { replacement ->
                    profiles.update(replacement.profile.copy(selected = true))
                }
            }
        }
    }

    suspend fun recordRecentSession(session: RecentSessionEntity) {
        assertLauncherWriter()
        recentSessions.upsert(session)
    }

    suspend fun getRecentSession(sessionId: String): RecentSessionEntity? =
        recentSessions.getById(sessionId)

    suspend fun getLatestSession(): RecentSessionEntity? = recentSessions.getLatest()

    suspend fun affectedProfiles(contentId: String): List<ProfileEntity> =
        profiles.getReferencing(contentId)

    suspend fun prepareRemoval(contentId: String): ContentRemovalPlan {
        val content = requireNotNull(contentItems.getById(contentId)) {
            "The selected content is no longer installed."
        }
        require(!content.bundled) { "The included Freedoom game cannot be removed." }
        return ContentRemovalPlan(
            content = content,
            affectedProfiles = profiles.getReferencing(contentId),
        )
    }

    suspend fun confirmRemoval(plan: ContentRemovalPlan): ContentRemovalResult {
        assertLauncherWriter()
        val removed = database.withTransaction {
            val current = requireNotNull(contentItems.getById(plan.content.contentId)) {
                "The selected content is no longer installed."
            }
            check(current.sha256 == plan.content.sha256) {
                "The content changed; review removal again."
            }
            val affected = profiles.getReferencing(current.contentId)
            check(affected.map { it.profileId } == plan.affectedProfiles.map { it.profileId }) {
                "Profile references changed; review removal again."
            }

            val gameProfiles = profiles.getUsingGame(current.contentId)
            if (gameProfiles.isNotEmpty()) {
                profiles.deleteProfiles(gameProfiles.map(ProfileEntity::profileId))
            }

            val modEntries = profiles.getEntriesUsingContent(current.contentId)
            val remainingProfileIds = modEntries
                .map(ProfileEntryEntity::profileId)
                .filterNot { id -> gameProfiles.any { it.profileId == id } }
                .distinct()
            profiles.deleteEntriesUsingContent(current.contentId)
            remainingProfileIds.forEach { profileId ->
                val normalized = profiles.getEntries(profileId)
                    .mapIndexed { position, entry ->
                        entry.copy(loadPosition = position)
                    }
                if (normalized.isNotEmpty()) profiles.updateEntries(normalized)
            }
            contentItems.delete(current)
            current
        }

        val remainingBlobUsers = contentItems.countUsingBlob(removed.blobPath)
        val blobDeleted = ContentBlobStore(
            AppPaths(applicationContext.filesDir).content,
        ).deleteIfUnused(File(removed.blobPath), remainingBlobUsers)
        return ContentRemovalResult(
            removedContent = removed,
            deletedProfiles = plan.affectedProfiles
                .filter { it.gameContentId == removed.contentId },
            updatedProfiles = plan.affectedProfiles
                .filterNot { it.gameContentId == removed.contentId },
            blobDeleted = blobDeleted,
        )
    }

    private fun assertLauncherWriter() {
        val processName = if (Build.VERSION.SDK_INT >= 28) {
            Application.getProcessName()
        } else {
            File("/proc/self/cmdline").readText().trimEnd('\u0000')
        }
        check(processName == applicationContext.packageName) {
            "The content database may only be opened by the launcher process"
        }
    }
}

internal data class ContentRemovalPlan(
    val content: ContentItemEntity,
    val affectedProfiles: List<ProfileEntity>,
)

internal data class ContentRemovalResult(
    val removedContent: ContentItemEntity,
    val deletedProfiles: List<ProfileEntity>,
    val updatedProfiles: List<ProfileEntity>,
    val blobDeleted: Boolean,
)
