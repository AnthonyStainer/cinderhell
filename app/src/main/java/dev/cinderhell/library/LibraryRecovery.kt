package dev.cinderhell.library

import android.content.Context
import dev.cinderhell.session.AppPaths

internal data class LibraryRecoveryResult(
    val removedImportTasks: Int,
    val removedTemporaryBlobs: Int,
    val removedOrphanBlobs: Int,
    val missingContent: List<ContentItemEntity>,
)

internal class LibraryRecovery(
    context: Context,
    private val repository: LibraryRepository = LibraryRepository(context),
) {
    private val paths = AppPaths(context.applicationContext.filesDir)
    private val blobs = ContentBlobStore(paths.content)

    suspend fun recover(): LibraryRecoveryResult {
        val removedTasks = paths.importTasks.listFiles()
            .orEmpty()
            .count { it.deleteRecursively() }
        val removedTemporary = blobs.cleanupTemporaryFiles()
        val items = repository.getAllContent()
        val referencedPaths = items.map { it.blobPath }.toSet()
        val removedOrphans = blobs.listBlobs()
            .filterNot { it.canonicalPath in referencedPaths }
            .count { it.delete() }
        val missing = items.filterNot { item ->
            val blob = java.io.File(item.blobPath)
            blob.isFile && blob.length() == item.byteSize
        }
        return LibraryRecoveryResult(
            removedImportTasks = removedTasks,
            removedTemporaryBlobs = removedTemporary,
            removedOrphanBlobs = removedOrphans,
            missingContent = missing,
        )
    }
}
