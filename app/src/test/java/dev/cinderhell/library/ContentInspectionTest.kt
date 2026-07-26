package dev.cinderhell.library

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ContentInspectionTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun renamedFreedoomIwadIsIdentifiedFromContents() {
        val file = wad(
            name = "totally-renamed.bin",
            magic = "IWAD",
            lumps = listOf("PLAYPAL", "COLORMAP", "FREEDOOM", "MAP01"),
        )

        val result = ContentInspector.inspect(file, fingerprint())

        assertEquals(ContentType.GAME_WAD, result.contentType)
        assertEquals(GameIdentity.FREEDOOM_PHASE2, result.gameIdentity)
    }

    @Test
    fun knownHashOverridesAmbiguousCommercialMapLayout() {
        val file = wad(
            name = "unknown-name",
            magic = "IWAD",
            lumps = listOf("PLAYPAL", "COLORMAP", "MAP01"),
        )

        val result = ContentInspector.inspect(
            file,
            fingerprint(md5 = "75c8cf89566741fa9d22447604053bd7"),
        )

        assertEquals(GameIdentity.PLUTONIA, result.gameIdentity)
        assertEquals("The Plutonia Experiment", result.suggestedName)
    }

    @Test
    fun knownCataloguePinsEverySupportedGameIdentity() {
        val known = mapOf(
            "1cd63c5ddff1bf8ce844237f580e9cf3" to GameIdentity.DOOM,
            "c4fe9fd920207691a9f493668e0a2083" to GameIdentity.DOOM,
            "25e1459ca71d321525f84628f45ca8cd" to GameIdentity.DOOM2,
            "4e158d9953c79ccf97bd0663244cc6b6" to GameIdentity.TNT,
            "75c8cf89566741fa9d22447604053bd7" to GameIdentity.PLUTONIA,
            "b93be13d05148dd01614bc205a03648e" to GameIdentity.FREEDOOM_PHASE1,
            "cd666466759b5e5f63af93c5f0ffd0a1" to GameIdentity.FREEDOOM_PHASE2,
        )

        known.forEach { (md5, identity) ->
            assertEquals(identity, KnownIwadCatalogue.identify(fingerprint(md5))?.first)
        }
    }

    @Test
    fun validPwadIsAModWithoutTrustingItsExtension() {
        val file = wad(
            name = "readme.txt",
            magic = "PWAD",
            lumps = listOf("MAP01", "UMAPINFO"),
        )

        val result = ContentInspector.inspect(file, fingerprint())

        assertEquals(ContentType.MOD_WAD, result.contentType)
        assertNull(result.gameIdentity)
    }

    @Test
    fun misleadingWadExtensionAndHostileDirectoryOffsetAreRejected() {
        val misleading = temporaryFolder.newFile("looks-valid.wad").apply {
            writeText("ordinary text")
        }
        assertTrue(
            runCatching { ContentInspector.inspect(misleading, fingerprint()) }
                .exceptionOrNull() is ContentInspectionException,
        )

        val hostile = temporaryFolder.newFile("hostile.wad")
        hostile.writeBytes(
            ByteBuffer.allocate(12)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put("IWAD".toByteArray(StandardCharsets.US_ASCII))
                .putInt(1)
                .putInt(Int.MAX_VALUE)
                .array(),
        )
        assertTrue(
            runCatching { ContentInspector.inspect(hostile, fingerprint()) }
                .exceptionOrNull() is ContentInspectionException,
        )
    }

    @Test
    fun boundedArchiveInspectionRecognizesModsAndRejectsMalformedOrUnsafeArchives() {
        val valid = zip("valid.pk3", mapOf("maps/MAP01.wad" to byteArrayOf(1, 2, 3)))
        assertEquals(
            ContentType.MOD_ARCHIVE,
            ContentInspector.inspect(valid, fingerprint()).contentType,
        )

        val traversal = zip("../unsafe.pk3", mapOf("../MAP01.wad" to byteArrayOf(1)))
        assertTrue(
            runCatching { ContentInspector.inspect(traversal, fingerprint()) }
                .exceptionOrNull() is ContentInspectionException,
        )

        val malformed = temporaryFolder.newFile("malformed.zip").apply {
            writeBytes(byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4, 1, 2))
        }
        assertTrue(
            runCatching { ContentInspector.inspect(malformed, fingerprint()) }
                .exceptionOrNull() is ContentInspectionException,
        )
    }

    @Test
    fun dehAndBexTextAreRecognizedButArbitraryTextIsRejected() {
        val deh = temporaryFolder.newFile("patch.data").apply {
            writeText("Patch File for DeHackEd v3.0\nDoom version = 21\n")
        }
        val bex = temporaryFolder.newFile("another.data").apply {
            writeText("[STRINGS]\nHUSTR_1 = Test\n")
        }
        val arbitrary = temporaryFolder.newFile("notes.deh").apply {
            writeText("this is not a patch")
        }

        assertEquals(ContentType.PATCH, ContentInspector.inspect(deh, fingerprint()).contentType)
        assertEquals(ContentType.PATCH, ContentInspector.inspect(bex, fingerprint()).contentType)
        assertTrue(
            runCatching { ContentInspector.inspect(arbitrary, fingerprint()) }
                .exceptionOrNull() is ContentInspectionException,
        )
    }

    private fun wad(
        name: String,
        magic: String,
        lumps: List<String>,
    ): File {
        val file = temporaryFolder.newFile(name)
        val bytes = ByteBuffer.allocate(12 + lumps.size * 16)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(magic.toByteArray(StandardCharsets.US_ASCII))
            .putInt(lumps.size)
            .putInt(12)
        lumps.forEach { nameValue ->
            bytes.putInt(12)
            bytes.putInt(0)
            bytes.put(
                nameValue.toByteArray(StandardCharsets.US_ASCII)
                    .copyOf(8),
            )
        }
        file.writeBytes(bytes.array())
        return file
    }

    private fun zip(name: String, entries: Map<String, ByteArray>): File {
        val file = temporaryFolder.newFile(name.substringAfterLast('/'))
        ZipOutputStream(file.outputStream()).use { output ->
            entries.forEach { (entryName, bytes) ->
                output.putNextEntry(ZipEntry(entryName))
                output.write(bytes)
                output.closeEntry()
            }
        }
        return file
    }

    private fun fingerprint(
        md5: String = "0".repeat(32),
    ) = ContentFingerprint(
        sha256 = "0".repeat(64),
        md5 = md5,
    )
}
