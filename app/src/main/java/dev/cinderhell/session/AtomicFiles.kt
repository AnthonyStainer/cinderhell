package dev.cinderhell.session

import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

internal object AtomicFiles {
    fun writeUtf8(target: File, value: String) {
        val parent = checkNotNull(target.parentFile)
        check(parent.isDirectory || parent.mkdirs())
        val temporary = parent.resolve(".${target.name}.${UUID.randomUUID()}.part")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(value.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            moveReplacing(temporary, target)
        } finally {
            temporary.delete()
        }
    }

    fun moveWithoutReplacing(source: File, target: File) {
        val options = arrayOf(StandardCopyOption.ATOMIC_MOVE)
        try {
            Files.move(source.toPath(), target.toPath(), *options)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath())
        }
    }

    private fun moveReplacing(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }
}
