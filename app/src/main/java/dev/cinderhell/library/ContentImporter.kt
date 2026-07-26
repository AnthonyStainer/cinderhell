package dev.cinderhell.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dev.cinderhell.session.AppPaths
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal sealed interface ContentImportResult {
    data class Imported(val item: ContentItemEntity) : ContentImportResult
    data class Duplicate(val existing: ContentItemEntity) : ContentImportResult
}

internal data class ImportProgress(
    val copiedBytes: Long,
    val totalBytes: Long?,
)

internal class ContentImporter(
    context: Context,
    private val repository: LibraryRepository = LibraryRepository(context),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val applicationContext = context.applicationContext
    private val resolver = applicationContext.contentResolver
    private val paths = AppPaths(applicationContext.filesDir)
    private val blobs = ContentBlobStore(paths.content)

    suspend fun import(
        uri: Uri,
        onProgress: (ImportProgress) -> Unit = {},
    ): ContentImportResult = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") {
            "Please select a document through Android's file picker."
        }
        val source = querySource(uri)
        val taskDirectory = paths.importTasks.resolve(UUID.randomUUID().toString())
        check(taskDirectory.mkdir()) { "Could not prepare the private import task." }
        val temporary = taskDirectory.resolve("payload.part")
        var committedBlob: File? = null
        var catalogueCommitted = false

        try {
            val fingerprint = copyAndFingerprint(uri, temporary, source.size, onProgress)
            val inspection = ContentInspector.inspect(temporary, fingerprint)
            repository.findContentByDigest(fingerprint.sha256)?.let { existing ->
                return@withContext ContentImportResult.Duplicate(existing)
            }

            committedBlob = blobs.commitTemporary(temporary, fingerprint.sha256)
            val item = ContentItemEntity(
                contentId = fingerprint.sha256,
                sha256 = fingerprint.sha256,
                displayName = inspection.suggestedName ?: source.displayName,
                blobPath = committedBlob.canonicalPath,
                byteSize = committedBlob.length(),
                contentType = inspection.contentType,
                gameIdentity = inspection.gameIdentity,
                engineRequirements = inspection.detectedRequirements
                    .sorted()
                    .joinToString(",")
                    .ifBlank { null },
                importedAtEpochMillis = now(),
                classificationVersion = KnownIwadCatalogue.VERSION,
            )
            if (repository.insertContent(item)) {
                catalogueCommitted = true
                ContentImportResult.Imported(item)
            } else {
                catalogueCommitted = true
                ContentImportResult.Duplicate(
                    checkNotNull(repository.findContentByDigest(fingerprint.sha256)) {
                        "A concurrent import could not be resolved."
                    },
                )
            }
        } catch (error: Throwable) {
            val blob = committedBlob
            if (blob != null && !catalogueCommitted) {
                withContext(NonCancellable) {
                    if (repository.findContentByDigest(blob.name) == null) {
                        blob.delete()
                    }
                }
            }
            throw error
        } finally {
            temporary.delete()
            taskDirectory.delete()
        }
    }

    private suspend fun copyAndFingerprint(
        uri: Uri,
        temporary: File,
        totalBytes: Long?,
        onProgress: (ImportProgress) -> Unit,
    ): ContentFingerprint {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val md5 = MessageDigest.getInstance("MD5")
        var copied = 0L
        val input = resolver.openInputStream(uri)
            ?: throw ContentInspectionException("Android could not open the selected document.")
        input.use {
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    copied += count
                    if (copied > ContentInspector.MAX_IMPORT_BYTES) {
                        throw ContentInspectionException(
                            "The selected document is larger than 512 MiB.",
                        )
                    }
                    output.write(buffer, 0, count)
                    sha256.update(buffer, 0, count)
                    md5.update(buffer, 0, count)
                    onProgress(ImportProgress(copied, totalBytes))
                }
                output.fd.sync()
            }
        }
        return ContentFingerprint(
            sha256 = sha256.digest().toHex(),
            md5 = md5.digest().toHex(),
        )
    }

    private fun querySource(uri: Uri): ImportSource {
        var displayName: String? = null
        var size: Long? = null
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { displayName = cursor.getString(it) }
                cursor.getColumnIndex(OpenableColumns.SIZE)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { size = cursor.getLong(it) }
            }
        }
        if (size != null && size!! > ContentInspector.MAX_IMPORT_BYTES) {
            throw ContentInspectionException("The selected document is larger than 512 MiB.")
        }
        return ImportSource(
            displayName = displayName
                ?.replace(Regex("[\\p{Cc}]"), "")
                ?.take(160)
                ?.ifBlank { null }
                ?: "Imported Doom content",
            size = size?.takeIf { it >= 0L },
        )
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") {
        "%02x".format(it)
    }

    private data class ImportSource(
        val displayName: String,
        val size: Long?,
    )
}
