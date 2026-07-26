package dev.cinderhell.library

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import dev.cinderhell.profile.ProfileConfigService
import dev.cinderhell.profile.ProfilePresets
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databases = mutableListOf<LibraryDatabase>()
    private val createdMedia = mutableListOf<android.net.Uri>()
    private val createdBlobs = mutableListOf<File>()
    private val createdTrees = mutableListOf<File>()

    @get:Rule
    val migration = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LibraryDatabase::class.java,
    )

    @After
    fun tearDown() {
        databases.forEach(LibraryDatabase::close)
        createdMedia.forEach { context.contentResolver.delete(it, null, null) }
        createdBlobs.forEach(File::delete)
        createdTrees.forEach(File::deleteRecursively)
    }

    @Test
    fun migrationFromOneToTwoPreservesContentAndAddsRequirements() {
        val databaseName = "migration-${UUID.randomUUID()}"
        val profileId = "migration-profile-${UUID.randomUUID()}"
        val profileRoot = File(context.filesDir, "migration/$profileId")
        val config = profileRoot.resolve("config/woof.cfg").apply {
            parentFile?.mkdirs()
            writeText("fpslimit 77\nuser_custom_value 5\n")
        }
        val save = profileRoot.resolve("saves/woofsav0.dsg").apply {
            parentFile?.mkdirs()
            writeText("save bytes")
        }
        val screenshot = profileRoot.resolve("screenshots/shot.png").apply {
            parentFile?.mkdirs()
            writeText("screenshot bytes")
        }
        createdTrees += profileRoot
        migration.createDatabase(databaseName, 1).apply {
            execSQL(
                """
                INSERT INTO content_items (
                    contentId, sha256, displayName, blobPath, byteSize,
                    contentType, gameIdentity, importedAtEpochMillis,
                    classificationVersion, bundled
                ) VALUES (
                    '${"a".repeat(64)}', '${"a".repeat(64)}', 'Fixture',
                    '/private/fixture', 7, 'MOD_WAD', NULL, 1, 1, 0
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO profiles (
                    profileId, name, gameContentId, presetId, presetVersion,
                    selected, configPath, createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (
                    '$profileId', 'Migration profile', '${"a".repeat(64)}',
                    'handheld', 1, 1, '${config.canonicalPath}', 1, 2
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO recent_sessions (
                    recentSessionId, profileId, startedAtEpochMillis,
                    endedAtEpochMillis, result, latestLevel, resumableStatePath
                ) VALUES (
                    '${"b".repeat(32)}', '$profileId', 3, 4,
                    'CLEAN_EXIT', 'MAP01', '${save.canonicalPath}'
                )
                """.trimIndent(),
            )
            close()
        }

        migration.runMigrationsAndValidate(
            databaseName,
            2,
            true,
            LibraryDatabase.MIGRATION_1_2,
        ).apply {
            query("SELECT engineRequirements FROM content_items").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
            query(
                "SELECT name, presetId, presetVersion FROM profiles WHERE profileId = '$profileId'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Migration profile", cursor.getString(0))
                assertEquals("handheld", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
            }
            query(
                "SELECT latestLevel, resumableStatePath FROM recent_sessions WHERE profileId = '$profileId'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("MAP01", cursor.getString(0))
                assertEquals(save.canonicalPath, cursor.getString(1))
            }
            close()
        }
        val profile = ProfileEntity(
            profileId = profileId,
            name = "Migration profile",
            gameContentId = "a".repeat(64),
            presetId = "handheld",
            presetVersion = 1,
            selected = true,
            configPath = config.canonicalPath,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 2,
        )
        ProfileConfigService().ensureMaterialized(profile, ProfilePresets.handheld)
        assertEquals("fpslimit 77\nuser_custom_value 5\n", config.readText())
        assertEquals("save bytes", save.readText())
        assertEquals("screenshot bytes", screenshot.readText())
    }

    @Test
    fun renamedMisleadingDocumentIsDurableAndDuplicateBytesAreReused() = runBlocking {
        val repository = repository()
        val bytes = wadBytes(
            "PWAD",
            listOf("MAP01", "UMAPINFO", "TEST${System.nanoTime() % 10}"),
        )
        val uri = createDocument(
            displayName = "definitely-not-a-wad.txt",
            mimeType = "text/plain",
            bytes = bytes,
        )
        val importer = ContentImporter(context, repository)

        val first = importer.import(uri) as ContentImportResult.Imported
        val second = importer.import(uri) as ContentImportResult.Duplicate
        createdBlobs += File(first.item.blobPath)
        context.contentResolver.delete(uri, null, null)
        createdMedia.remove(uri)

        assertEquals(ContentType.MOD_WAD, first.item.contentType)
        assertEquals(first.item.contentId, second.existing.contentId)
        assertTrue(File(first.item.blobPath).isFile)
        assertEquals(bytes.toList(), File(first.item.blobPath).readBytes().toList())
        assertEquals(1, repository.getAllContent().count { it.sha256 == first.item.sha256 })
    }

    @Test
    fun cancelledImportLeavesNoCatalogueRowBlobOrTaskDirectory() = runBlocking {
        val repository = repository()
        val bytes = wadBytes("PWAD", listOf("MAP01", "CANCEL"))
        val digest = sha256(bytes)
        val uri = createDocument(
            displayName = "cancelled.wad",
            mimeType = "application/octet-stream",
            bytes = bytes,
        )
        val importer = ContentImporter(context, repository)

        val failure = runCatching {
            importer.import(uri) {
                throw CancellationException("Test interruption")
            }
        }.exceptionOrNull()

        assertTrue(failure is CancellationException)
        assertNull(repository.findContentByDigest(digest))
        assertFalse(
            File(context.filesDir, "content/sha256/$digest").exists(),
        )
        assertTrue(
            File(context.filesDir, "imports").listFiles().orEmpty().isEmpty(),
        )
    }

    @Test
    fun confirmedRemovalUpdatesModProfilesAndDeletesAnUnsharedBlob() = runBlocking {
        val repository = repository()
        val store = ContentBlobStore(File(context.filesDir, "content/sha256"))
        val game = contentFixture(store, "game-${UUID.randomUUID()}", ContentType.GAME_WAD)
        val firstMod = contentFixture(store, "first-${UUID.randomUUID()}", ContentType.MOD_WAD)
        val removedMod = contentFixture(store, "remove-${UUID.randomUUID()}", ContentType.MOD_WAD)
        createdBlobs += listOf(File(game.blobPath), File(firstMod.blobPath), File(removedMod.blobPath))
        listOf(game, firstMod, removedMod).forEach { repository.insertContent(it) }
        val profileId = "profile-${UUID.randomUUID()}"
        val profile = ProfileEntity(
            profileId = profileId,
            name = "Removal fixture",
            gameContentId = game.contentId,
            presetId = "handheld",
            presetVersion = 1,
            selected = true,
            configPath = File(context.filesDir, "configs/$profileId/woof.cfg").canonicalPath,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
        )
        repository.saveProfile(
            profile,
            listOf(
                entry(profileId, firstMod, 0),
                entry(profileId, removedMod, 1),
            ),
        )

        val plan = repository.prepareRemoval(removedMod.contentId)
        assertEquals(listOf("Removal fixture"), plan.affectedProfiles.map(ProfileEntity::name))
        val result = repository.confirmRemoval(plan)
        val updated = checkNotNull(repository.getProfile(profileId))

        assertTrue(result.blobDeleted)
        assertTrue(result.deletedProfiles.isEmpty())
        assertEquals(listOf(firstMod.contentId), updated.orderedEntries.map { it.content.contentId })
        assertEquals(listOf(0), updated.orderedEntries.map { it.entry.loadPosition })
        assertNull(repository.getContent(removedMod.contentId))
        assertFalse(File(removedMod.blobPath).exists())
    }

    private fun repository(): LibraryRepository {
        val database = Room.inMemoryDatabaseBuilder(
            context,
            LibraryDatabase::class.java,
        ).allowMainThreadQueries().build()
        databases += database
        return LibraryRepository(context, database)
    }

    private fun createDocument(
        displayName: String,
        mimeType: String,
        bytes: ByteArray,
    ): android.net.Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/CinderhellTests")
        }
        val uri = checkNotNull(
            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values,
            ),
        )
        createdMedia += uri
        context.contentResolver.openOutputStream(uri, "w")!!.use { it.write(bytes) }
        return uri
    }

    private fun contentFixture(
        store: ContentBlobStore,
        value: String,
        type: ContentType,
    ): ContentItemEntity {
        val source = File(context.cacheDir, "$value.fixture").apply {
            writeText(value)
        }
        val digest = ContentBlobStore.sha256(source)
        val blob = store.ensureCopied(source, digest)
        source.delete()
        return ContentItemEntity(
            contentId = digest,
            sha256 = digest,
            displayName = value,
            blobPath = blob.canonicalPath,
            byteSize = blob.length(),
            contentType = type,
            gameIdentity = if (type == ContentType.GAME_WAD) GameIdentity.DOOM2 else null,
            engineRequirements = null,
            importedAtEpochMillis = 1,
            classificationVersion = 1,
        )
    }

    private fun entry(
        profileId: String,
        content: ContentItemEntity,
        position: Int,
    ) = ProfileEntryEntity(
        entryId = UUID.randomUUID().toString(),
        profileId = profileId,
        contentId = content.contentId,
        kind = ProfileEntryKind.MOD,
        loadPosition = position,
    )

    private fun wadBytes(magic: String, lumps: List<String>): ByteArray {
        val bytes = ByteBuffer.allocate(12 + lumps.size * 16)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(magic.toByteArray(StandardCharsets.US_ASCII))
            .putInt(lumps.size)
            .putInt(12)
        lumps.forEach { name ->
            bytes.putInt(12)
            bytes.putInt(0)
            bytes.put(name.toByteArray(StandardCharsets.US_ASCII).copyOf(8))
        }
        return bytes.array()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it)
        }
}
