package dev.cinderhell.library

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityCorpusTest {
    private val corpus = sequenceOf(
        File("../test-corpus/generated"),
        File("test-corpus/generated"),
    ).first { it.isDirectory }

    @Test
    fun everyGeneratedContainerPassesTheSameBoundedImporterInspection() {
        listOf(
            "vanilla-map.wad",
            "boom-map.wad",
            "mbf-map.wad",
            "mbf21-map.wad",
        ).forEach { name ->
            assertEquals(
                ContentType.MOD_WAD,
                ContentInspector.inspect(corpus.resolve(name), fingerprint(name)).contentType,
            )
        }
        listOf("maps.pk3", "maps.zip").forEach { name ->
            assertEquals(
                ContentType.MOD_ARCHIVE,
                ContentInspector.inspect(corpus.resolve(name), fingerprint(name)).contentType,
            )
        }
        listOf("noop.deh", "strings.bex", "mbf21.deh").forEach { name ->
            assertEquals(
                ContentType.PATCH,
                ContentInspector.inspect(corpus.resolve(name), fingerprint(name)).contentType,
            )
        }
        assertTrue(corpus.resolve("SHA256SUMS").isFile)
    }

    private fun fingerprint(name: String): ContentFingerprint {
        val bytes = corpus.resolve(name).readBytes()
        return ContentFingerprint(
            sha256 = digest("SHA-256", bytes),
            md5 = digest("MD5", bytes),
        )
    }

    private fun digest(algorithm: String, bytes: ByteArray): String =
        MessageDigest.getInstance(algorithm).digest(bytes).joinToString("") {
            "%02x".format(it)
        }
}
