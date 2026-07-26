package dev.cinderhell.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ContentBlobStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun duplicateBytesReuseOneImmutableBlob() {
        val directory = temporaryFolder.newFolder("content")
        val store = ContentBlobStore(directory)
        val source = temporaryFolder.newFile("source.wad").apply {
            writeText("identical content")
        }
        val digest = ContentBlobStore.sha256(source)

        val first = store.ensureCopied(source, digest)
        val secondTemporary = temporaryFolder.newFile("second.part").apply {
            writeBytes(source.readBytes())
        }
        val second = store.commitTemporary(secondTemporary, digest)

        assertEquals(first.canonicalPath, second.canonicalPath)
        assertEquals(1, store.listBlobs().size)
        assertFalse(secondTemporary.exists())
    }

    @Test
    fun temporaryCleanupDoesNotDeleteCommittedBlobs() {
        val directory = temporaryFolder.newFolder("content")
        val store = ContentBlobStore(directory)
        val source = temporaryFolder.newFile("source").apply { writeText("content") }
        val digest = ContentBlobStore.sha256(source)
        val blob = store.ensureCopied(source, digest)
        val partial = directory.resolve(".$digest.interrupted.part").apply {
            writeText("partial")
        }

        assertEquals(1, store.cleanupTemporaryFiles())
        assertFalse(partial.exists())
        assertTrue(blob.isFile)
    }
}
