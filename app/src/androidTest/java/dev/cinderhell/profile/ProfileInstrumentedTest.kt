package dev.cinderhell.profile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.cinderhell.library.ContentBlobStore
import dev.cinderhell.library.ContentItemEntity
import dev.cinderhell.library.ContentType
import dev.cinderhell.library.GameIdentity
import dev.cinderhell.library.LibraryDatabase
import dev.cinderhell.library.LibraryRepository
import dev.cinderhell.session.AppPaths
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "profiles-${UUID.randomUUID()}.db"
    private val paths = AppPaths(context.filesDir)
    private val createdFiles = mutableListOf<File>()
    private var database: LibraryDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
        createdFiles.forEach(File::delete)
    }

    @Test
    fun exactOrderAndStableEntryIdsPersistAcrossDatabaseRestart() = runBlocking {
        var repository = openRepository()
        val game = content("game", ContentType.GAME_WAD)
        val maps = content("maps", ContentType.MOD_WAD)
        val patch = content("patch", ContentType.PATCH)
        listOf(game, maps, patch).forEach { repository.insertContent(it) }
        var manager = ProfileManager(paths, repository, now = { 100 })

        val created = manager.save(
            ProfileDraft(
                name = "Ordered profile",
                gameContentId = game.contentId,
                presetId = PresetId.HANDHELD,
                entries = listOf(
                    ProfileEntryDraft(contentId = maps.contentId),
                    ProfileEntryDraft(contentId = patch.contentId),
                ),
            ),
        )
        val originalIds = created.orderedEntries.associate {
            it.content.contentId to it.entry.entryId
        }
        database?.close()
        database = null

        repository = openRepository()
        manager = ProfileManager(paths, repository, now = { 200 })
        val restored = checkNotNull(repository.getProfile(created.profile.profileId))
        val reorderedDrafts = manager.reorder(
            restored.orderedEntries.map {
                ProfileEntryDraft(it.entry.entryId, it.content.contentId)
            },
            1,
            0,
        )
        val updated = manager.save(
            ProfileDraft(
                profileId = restored.profile.profileId,
                name = restored.profile.name,
                gameContentId = restored.profile.gameContentId,
                presetId = PresetId.HANDHELD,
                entries = reorderedDrafts,
            ),
        )

        assertEquals(
            listOf(patch.contentId, maps.contentId),
            updated.orderedEntries.map { it.content.contentId },
        )
        assertEquals(listOf(0, 1), updated.orderedEntries.map { it.entry.loadPosition })
        assertEquals(
            originalIds,
            updated.orderedEntries.associate { it.content.contentId to it.entry.entryId },
        )
        assertTrue(
            ProfilePreflight(context, repository).check(updated.profile.profileId)
                is ProfilePreflightResult.Ready,
        )

        File(maps.blobPath).appendText("damage")
        val blocked = ProfilePreflight(context, repository)
            .check(updated.profile.profileId) as ProfilePreflightResult.Blocked
        assertTrue(blocked.reasons.any { it.contains("missing or damaged") })
    }

    private fun openRepository(): LibraryRepository {
        val opened = Room.databaseBuilder(
            context,
            LibraryDatabase::class.java,
            databaseName,
        ).build()
        database = opened
        return LibraryRepository(context, opened)
    }

    private fun content(label: String, type: ContentType): ContentItemEntity {
        val unique = "$label-${UUID.randomUUID()}"
        val source = File(context.cacheDir, "$unique.bin").apply { writeText(unique) }
        val digest = ContentBlobStore.sha256(source)
        val blob = ContentBlobStore(paths.content).ensureCopied(source, digest)
        source.delete()
        createdFiles += blob
        return ContentItemEntity(
            contentId = digest,
            sha256 = digest,
            displayName = label,
            blobPath = blob.canonicalPath,
            byteSize = blob.length(),
            contentType = type,
            gameIdentity = if (type == ContentType.GAME_WAD) GameIdentity.DOOM2 else null,
            engineRequirements = null,
            importedAtEpochMillis = 1,
            classificationVersion = 1,
        )
    }
}
