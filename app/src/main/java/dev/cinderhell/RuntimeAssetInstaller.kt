package dev.cinderhell

import android.content.Context
import android.content.res.AssetManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

internal data class InstalledRuntime(
    val iwad: File,
    val config: File,
    val saves: File,
    val screenshots: File,
)

internal object RuntimeAssetInstaller {
    internal const val FREEDOOM_SHA256 =
        "a8772e088847032510d97ba2312406a6998f21cbab44d4ff10696faa9c0ecd4b"
    internal const val WOOF_PK3_SHA256 =
        "ebd6dac1b3761b468d7d2982e9b641e98975c4f3e05e06f0f9767d9967ba6a31"

    private data class AssetSpec(
        val source: String,
        val destination: String,
        val sha256: String,
    )

    private val requiredAssets = listOf(
        AssetSpec("runtime/woof.pk3", "woof.pk3", WOOF_PK3_SHA256),
        AssetSpec(
            "runtime/freedoom2.wad",
            "runtime/freedoom2.wad",
            FREEDOOM_SHA256,
        ),
    )

    @Synchronized
    fun ensureInstalled(context: Context): InstalledRuntime {
        val root = context.filesDir.canonicalFile
        requiredAssets.forEach { spec ->
            installVerified(context.assets, root, spec)
        }

        val configDirectory = root.resolve("configs/freedoom")
        val saveDirectory = root.resolve("saves/freedoom")
        val screenshotDirectory = root.resolve("screenshots")
        listOf(configDirectory, saveDirectory, screenshotDirectory).forEach(::ensureDirectory)

        return InstalledRuntime(
            iwad = root.resolve("runtime/freedoom2.wad"),
            config = configDirectory.resolve("woof.cfg"),
            saves = saveDirectory,
            screenshots = screenshotDirectory,
        )
    }

    private fun installVerified(
        assets: AssetManager,
        root: File,
        spec: AssetSpec,
    ) {
        val target = root.resolve(spec.destination).canonicalFile
        check(target.toPath().startsWith(root.toPath())) {
            "Runtime asset escaped application storage"
        }
        val parent = checkNotNull(target.parentFile)
        ensureDirectory(parent)

        if (target.isFile && sha256(target) == spec.sha256) {
            return
        }

        val temporary = parent.resolve(
            ".${target.name}.${UUID.randomUUID()}.part",
        )
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            assets.open(spec.source, AssetManager.ACCESS_STREAMING).use { input ->
                FileOutputStream(temporary).use { output ->
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

            val actual = digest.digest().toHex()
            check(actual == spec.sha256) {
                "Packaged runtime asset failed integrity verification: ${spec.source}"
            }

            try {
                Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }

            check(sha256(target) == spec.sha256) {
                "Installed runtime asset failed integrity verification: ${spec.source}"
            }
        } finally {
            temporary.delete()
        }
    }

    private fun ensureDirectory(directory: File) {
        check(directory.isDirectory || directory.mkdirs()) {
            "Could not create private runtime directory: ${directory.name}"
        }
    }

    private fun sha256(file: File): String {
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
