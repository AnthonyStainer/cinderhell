package dev.cinderhell.profile

import dev.cinderhell.library.ContentType
import dev.cinderhell.library.LibraryRepository
import dev.cinderhell.library.ProfileEntity
import dev.cinderhell.library.ProfileEntryEntity
import dev.cinderhell.library.ProfileEntryKind
import dev.cinderhell.library.ProfileWithEntries
import dev.cinderhell.session.AppPaths
import java.util.UUID

internal data class ProfileEntryDraft(
    val entryId: String? = null,
    val contentId: String,
)

internal data class ProfileDraft(
    val profileId: String? = null,
    val name: String,
    val gameContentId: String,
    val presetId: PresetId,
    val entries: List<ProfileEntryDraft> = emptyList(),
    val selected: Boolean = true,
)

internal class ProfileManager(
    private val paths: AppPaths,
    private val repository: LibraryRepository,
    private val configs: ProfileConfigService = ProfileConfigService(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun save(draft: ProfileDraft): ProfileWithEntries {
        val name = draft.name.trim()
        require(name.isNotBlank() && name.length <= 80) {
            "Profile names must contain 1–80 characters."
        }
        val game = requireNotNull(repository.getContent(draft.gameContentId)) {
            "Choose an installed game."
        }
        require(game.contentType == ContentType.GAME_WAD) {
            "A profile must contain exactly one installed game."
        }
        require(draft.entries.map(ProfileEntryDraft::contentId).distinct().size == draft.entries.size) {
            "A profile cannot load the same item twice."
        }
        val content = draft.entries.map { entry ->
            requireNotNull(repository.getContent(entry.contentId)) {
                "A selected mod or patch is no longer installed."
            }
        }
        require(content.none { it.contentType == ContentType.GAME_WAD }) {
            "Additional games cannot be added as profile mods."
        }

        val profileId = draft.profileId ?: UUID.randomUUID().toString()
        val existing = repository.getProfile(profileId)
        val timestamp = now()
        val preset = ProfilePresets.require(draft.presetId.wireValue, ProfilePresets.VERSION)
        if (existing != null) {
            require(
                existing.profile.presetId == preset.id.wireValue &&
                    existing.profile.presetVersion == preset.version,
            ) {
                "Changing a preset requires an explicit preview and reapply."
            }
        }
        val profile = ProfileEntity(
            profileId = profileId,
            name = name,
            gameContentId = game.contentId,
            presetId = preset.id.wireValue,
            presetVersion = preset.version,
            selected = draft.selected,
            configPath = paths.profileConfig(profileId).canonicalPath,
            createdAtEpochMillis = existing?.profile?.createdAtEpochMillis ?: timestamp,
            updatedAtEpochMillis = timestamp,
        )
        paths.profileSaves(profileId).mkdirs()
        paths.profileScreenshots(profileId).mkdirs()
        configs.ensureMaterialized(profile, preset)

        val existingIds = existing?.entries
            .orEmpty()
            .associate { it.entry.contentId to it.entry.entryId }
        val entries = draft.entries.mapIndexed { position, entry ->
            val item = content[position]
            ProfileEntryEntity(
                entryId = entry.entryId
                    ?: existingIds[entry.contentId]
                    ?: UUID.randomUUID().toString(),
                profileId = profileId,
                contentId = entry.contentId,
                kind = if (item.contentType == ContentType.PATCH) {
                    ProfileEntryKind.PATCH
                } else {
                    ProfileEntryKind.MOD
                },
                loadPosition = position,
            )
        }
        repository.saveProfile(profile, entries)
        return checkNotNull(repository.getProfile(profileId))
    }

    fun reorder(
        entries: List<ProfileEntryDraft>,
        fromIndex: Int,
        toIndex: Int,
    ): List<ProfileEntryDraft> {
        require(fromIndex in entries.indices && toIndex in entries.indices) {
            "Invalid profile reorder position."
        }
        if (fromIndex == toIndex) return entries
        return entries.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    suspend fun previewPresetChange(
        profileId: String,
        presetId: PresetId,
    ): PresetReapplyPreview {
        val profile = requireNotNull(repository.getProfile(profileId)) {
            "The profile is no longer available."
        }
        return configs.previewReapply(
            profile.profile,
            ProfilePresets.require(presetId.wireValue, ProfilePresets.VERSION),
        )
    }

    suspend fun applyPresetChange(preview: PresetReapplyPreview): ProfileWithEntries {
        val current = requireNotNull(repository.getProfile(preview.profileId)) {
            "The profile is no longer available."
        }
        check(current.profile.configPath == preview.configPath) {
            "The profile configuration changed; preview the preset again."
        }
        configs.applyPreview(preview)
        repository.saveProfile(
            current.profile.copy(
                presetId = preview.preset.id.wireValue,
                presetVersion = preview.preset.version,
                updatedAtEpochMillis = now(),
            ),
            current.orderedEntries.map { it.entry },
        )
        return checkNotNull(repository.getProfile(preview.profileId))
    }
}
