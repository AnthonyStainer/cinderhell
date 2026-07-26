package dev.cinderhell.library

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

internal class ContentBlobStore(
    private val directory: File,
) {
    init {
        check(directory.isDirectory || directory.mkdirs()) {
            "Could not create the private content store"
        }
    }

    fun blobForDigest(sha256: String): File {
        require(SHA256_PATTERN.matches(sha256)) { "Invalid content digest" }
        return directory.resolve(sha256)
    }

    fun ensureCopied(source: File, expectedSha256: String): File {
        require(source.isFile) { "Content source is missing" }
        val target = blobForDigest(expectedSha256)
        if (target.isFile) {
            check(sha256(target) == expectedSha256) {
                "An immutable content blob failed integrity verification"
            }
            return target
        }

        val temporary = directory.resolve(".$expectedSha256.${UUID.randomUUID()}.part")
        try {
            val actual = copyAndHash(source, temporary)
            check(actual == expectedSha256) { "Content digest changed while copying" }
            moveWithoutReplacing(temporary, target)
        } catch (_: FileAlreadyExistsException) {
            check(target.isFile && sha256(target) == expectedSha256) {
                "A concurrent content commit produced invalid bytes"
            }
        } finally {
            temporary.delete()
        }
        return target
    }

    fun commitTemporary(temporary: File, expectedSha256: String): File {
        require(temporary.isFile) { "Import temporary file is missing" }
        val target = blobForDigest(expectedSha256)
        check(sha256(temporary) == expectedSha256) {
            "Import temporary file failed integrity verification"
        }
        if (target.isFile) {
            check(sha256(target) == expectedSha256) {
                "An immutable content blob failed integrity verification"
            }
            temporary.delete()
            return target
        }
        try {
            moveWithoutReplacing(temporary, target)
        } catch (_: FileAlreadyExistsException) {
            check(target.isFile && sha256(target) == expectedSha256) {
                "A concurrent content commit produced invalid bytes"
            }
            temporary.delete()
        }
        return target
    }

    fun deleteIfUnused(blob: File, usageCount: Int): Boolean {
        require(blob.parentFile?.canonicalFile == directory.canonicalFile) {
            "Blob is outside the content store"
        }
        return usageCount == 0 && (!blob.exists() || blob.delete())
    }

    fun cleanupTemporaryFiles(): Int =
        directory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(".part") }
            .count { it.delete() }

    fun listBlobs(): List<File> =
        directory.listFiles()
            .orEmpty()
            .filter { it.isFile && SHA256_PATTERN.matches(it.name) }

    private fun copyAndHash(source: File, destination: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    digest.update(buffer, 0, count)
                }
                output.fd.sync()
            }
        }
        return digest.digest().toHex()
    }

    private fun moveWithoutReplacing(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath())
        }
    }

    companion object {
        private val SHA256_PATTERN = Regex("[0-9a-f]{64}")

        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().toHex()
        }

        private fun ByteArray.toHex(): String = joinToString(separator = "") {
            "%02x".format(it)
        }
    }
}
