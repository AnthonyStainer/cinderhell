package dev.cinderhell.profile

import android.content.Context
import dev.cinderhell.RuntimeAssetInstaller
import dev.cinderhell.library.ContentItemEntity
import dev.cinderhell.library.ContentType
import dev.cinderhell.library.LibraryRepository
import dev.cinderhell.library.ProfileEntryKind
import dev.cinderhell.library.ProfileWithEntries
import dev.cinderhell.session.AppPaths
import java.io.File

internal sealed interface ProfilePreflightResult {
    data class Ready(val profile: PreparedProfile) : ProfilePreflightResult
    data class Blocked(val reasons: List<String>) : ProfilePreflightResult
}

internal data class PreparedProfile(
    val record: ProfileWithEntries,
    val game: ContentItemEntity,
    val preset: ProfilePreset,
)

internal class ProfilePreflight(
    context: Context,
    private val repository: LibraryRepository = LibraryRepository(context),
    private val configs: ProfileConfigService = ProfileConfigService(),
) {
    private val applicationContext = context.applicationContext
    private val paths = AppPaths(applicationContext.filesDir)

    suspend fun check(profileId: String): ProfilePreflightResult {
        val profile = repository.getProfile(profileId)
            ?: return ProfilePreflightResult.Blocked(
                listOf("The selected profile is no longer available."),
            )
        val reasons = mutableListOf<String>()
        val preset = runCatching {
            ProfilePresets.require(
                profile.profile.presetId,
                profile.profile.presetVersion,
            )
        }.getOrElse {
            reasons += it.message ?: "The profile preset is unavailable."
            null
        }
        val game = repository.getContent(profile.profile.gameContentId)
        if (game == null || game.contentType != ContentType.GAME_WAD) {
            reasons += "Choose one installed Doom game for ${profile.profile.name}."
        } else {
            validateContent(game, reasons)
            if (game.gameIdentity == null) {
                reasons += "${game.displayName} is not a recognized supported game."
            }
        }

        val ordered = profile.orderedEntries
        if (ordered.map { it.entry.loadPosition } != ordered.indices.toList()) {
            reasons += "The mod load order is invalid; open the profile and save it again."
        }
        ordered.forEach { entry ->
            validateContent(entry.content, reasons)
            val expectedKind = if (entry.content.contentType == ContentType.PATCH) {
                ProfileEntryKind.PATCH
            } else {
                ProfileEntryKind.MOD
            }
            if (entry.entry.kind != expectedKind ||
                entry.content.contentType == ContentType.GAME_WAD
            ) {
                reasons += "${entry.content.displayName} is in an invalid profile position."
            }
        }

        val unsupported = buildSet {
            listOfNotNull(game).plus(ordered.map { it.content }).forEach { item ->
                item.engineRequirements
                    ?.split(',')
                    ?.filter(String::isNotBlank)
                    ?.forEach(::add)
            }
        }.intersect(UNSUPPORTED_REQUIREMENTS)
        if (unsupported.isNotEmpty()) {
            reasons +=
                "This profile requires ${unsupported.sorted().joinToString()}, which Cinderhell does not support."
        }

        runCatching { RuntimeAssetInstaller.ensureInstalled(applicationContext) }
            .onFailure {
                reasons += "The included game runtime could not be prepared."
            }
        if (preset != null) {
            runCatching {
                configs.ensureMaterialized(profile.profile, preset)
                check(paths.profileSaves(profileId).isDirectory || paths.profileSaves(profileId).mkdirs())
                check(
                    paths.profileScreenshots(profileId).isDirectory ||
                        paths.profileScreenshots(profileId).mkdirs(),
                )
            }.onFailure {
                reasons += "The profile's private settings or save folder is unavailable."
            }
        }

        return if (reasons.isEmpty()) {
            ProfilePreflightResult.Ready(
                PreparedProfile(
                    record = profile,
                    game = checkNotNull(game),
                    preset = checkNotNull(preset),
                ),
            )
        } else {
            ProfilePreflightResult.Blocked(reasons.distinct())
        }
    }

    private fun validateContent(
        item: ContentItemEntity,
        reasons: MutableList<String>,
    ) {
        val file = File(item.blobPath)
        if (!paths.isPrivatePath(file) ||
            file.parentFile?.canonicalFile != paths.content.canonicalFile ||
            file.name != item.sha256 ||
            !file.isFile ||
            file.length() != item.byteSize
        ) {
            reasons += "${item.displayName} is missing or damaged; import it again."
        }
    }

    private companion object {
        val UNSUPPORTED_REQUIREMENTS = setOf("ZScript", "DECORATE")
    }
}
