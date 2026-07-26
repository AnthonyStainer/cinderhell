package dev.cinderhell.launcher

import android.content.Context
import dev.cinderhell.library.ContentItemEntity
import dev.cinderhell.library.ContentRemovalPlan
import dev.cinderhell.library.ContentRemovalResult
import dev.cinderhell.library.ContentType
import dev.cinderhell.library.LibraryRepository
import dev.cinderhell.library.ProfileWithEntries
import dev.cinderhell.profile.PresetId
import dev.cinderhell.profile.PresetReapplyPreview
import dev.cinderhell.profile.ProfileDraft
import dev.cinderhell.profile.ProfileEntryDraft
import dev.cinderhell.profile.ProfileManager
import dev.cinderhell.session.AppPaths
import dev.cinderhell.session.BundledSessionCoordinator
import dev.cinderhell.session.ContinueSummary

internal data class LauncherSnapshot(
    val content: List<ContentItemEntity>,
    val profiles: List<ProfileWithEntries>,
    val selectedProfileId: String?,
    val continueSummary: ContinueSummary?,
) {
    val games: List<ContentItemEntity>
        get() = content.filter { it.contentType == ContentType.GAME_WAD }
    val additions: List<ContentItemEntity>
        get() = content.filter { it.contentType != ContentType.GAME_WAD }
    val firstRun: Boolean
        get() = content.none { !it.bundled }
}

internal class LauncherService(context: Context) {
    private val applicationContext = context.applicationContext
    private val repository = LibraryRepository(applicationContext)
    private val sessions = BundledSessionCoordinator(applicationContext)
    private val profiles = ProfileManager(
        paths = AppPaths(applicationContext.filesDir),
        repository = repository,
    )

    suspend fun load(): LauncherSnapshot {
        sessions.ensureBundledLibrary()
        val allProfiles = repository.getAllProfiles()
        return LauncherSnapshot(
            content = repository.getAllContent(),
            profiles = allProfiles,
            selectedProfileId = allProfiles.singleOrNull { it.profile.selected }
                ?.profile?.profileId,
            continueSummary = sessions.continueSummary(),
        )
    }

    suspend fun selectProfile(profileId: String) {
        repository.selectProfile(profileId)
    }

    suspend fun selectGame(gameContentId: String): ProfileWithEntries {
        val existing = repository.getAllProfiles()
            .firstOrNull { it.profile.gameContentId == gameContentId }
        if (existing != null) {
            repository.selectProfile(existing.profile.profileId)
            return checkNotNull(repository.getProfile(existing.profile.profileId))
        }
        val game = requireNotNull(repository.getContent(gameContentId)) {
            "The selected game is no longer installed."
        }
        require(game.contentType == ContentType.GAME_WAD) {
            "Choose an installed game."
        }
        return profiles.save(
            ProfileDraft(
                name = game.displayName,
                gameContentId = gameContentId,
                presetId = PresetId.HANDHELD,
            ),
        )
    }

    suspend fun saveProfile(
        profileId: String?,
        name: String,
        gameContentId: String,
        presetId: PresetId,
        orderedContentIds: List<String>,
    ): ProfileWithEntries = profiles.save(
        ProfileDraft(
            profileId = profileId,
            name = name,
            gameContentId = gameContentId,
            presetId = presetId,
            entries = orderedContentIds.map { ProfileEntryDraft(contentId = it) },
        ),
    )

    fun reorder(
        entries: List<ProfileEntryDraft>,
        fromIndex: Int,
        toIndex: Int,
    ): List<ProfileEntryDraft> = profiles.reorder(entries, fromIndex, toIndex)

    suspend fun previewPresetChange(
        profileId: String,
        presetId: PresetId,
    ): PresetReapplyPreview = profiles.previewPresetChange(profileId, presetId)

    suspend fun applyPresetChange(preview: PresetReapplyPreview): ProfileWithEntries =
        profiles.applyPresetChange(preview)

    suspend fun prepareRemoval(contentId: String): ContentRemovalPlan =
        repository.prepareRemoval(contentId)

    suspend fun confirmRemoval(plan: ContentRemovalPlan): ContentRemovalResult =
        repository.confirmRemoval(plan)

    suspend fun deleteProfile(profileId: String) {
        repository.deleteProfile(profileId)
    }
}
