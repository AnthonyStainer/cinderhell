package dev.cinderhell.library

import android.content.Context
import dev.cinderhell.RuntimeAssetInstaller
import dev.cinderhell.session.AppPaths

internal data class BundledLibraryState(
    val content: ContentItemEntity,
    val profile: ProfileWithEntries,
)

internal class BundledLibraryBootstrap(
    context: Context,
    private val repository: LibraryRepository = LibraryRepository(context),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val applicationContext = context.applicationContext
    private val paths = AppPaths(applicationContext.filesDir)
    private val blobs = ContentBlobStore(paths.content)

    suspend fun ensureInstalled(): BundledLibraryState {
        val runtime = RuntimeAssetInstaller.ensureInstalled(applicationContext)
        val blob = blobs.ensureCopied(runtime.iwad, RuntimeAssetInstaller.FREEDOOM_SHA256)
        val existingContent =
            repository.findContentByDigest(RuntimeAssetInstaller.FREEDOOM_SHA256)
        val content = existingContent ?: ContentItemEntity(
            contentId = RuntimeAssetInstaller.FREEDOOM_SHA256,
            sha256 = RuntimeAssetInstaller.FREEDOOM_SHA256,
            displayName = "Freedoom: Phase 2",
            blobPath = blob.canonicalPath,
            byteSize = blob.length(),
            contentType = ContentType.GAME_WAD,
            gameIdentity = GameIdentity.FREEDOOM_PHASE2,
            engineRequirements = null,
            importedAtEpochMillis = now(),
            classificationVersion = CLASSIFICATION_VERSION,
            bundled = true,
        ).also { repository.insertContent(it) }

        val existingProfile = repository.getProfile(BUNDLED_PROFILE_ID)
        if (existingProfile != null) {
            return BundledLibraryState(content, existingProfile)
        }

        val timestamp = now()
        val profile = ProfileEntity(
            profileId = BUNDLED_PROFILE_ID,
            name = "Freedoom",
            gameContentId = content.contentId,
            presetId = "handheld",
            presetVersion = 1,
            selected = repository.getSelectedProfile() == null,
            configPath = paths.profileConfig(BUNDLED_PROFILE_ID).canonicalPath,
            createdAtEpochMillis = timestamp,
            updatedAtEpochMillis = timestamp,
        )
        checkNotNull(paths.profileConfig(BUNDLED_PROFILE_ID).parentFile).mkdirs()
        paths.profileSaves(BUNDLED_PROFILE_ID).mkdirs()
        paths.profileScreenshots(BUNDLED_PROFILE_ID).mkdirs()
        repository.saveProfile(profile, emptyList())
        return BundledLibraryState(
            content = content,
            profile = checkNotNull(repository.getProfile(BUNDLED_PROFILE_ID)),
        )
    }

    companion object {
        const val BUNDLED_PROFILE_ID = "bundled-freedoom-handheld"
        const val CLASSIFICATION_VERSION = 1
    }
}
