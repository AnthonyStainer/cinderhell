package dev.cinderhell.library

import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipException
import java.util.zip.ZipFile

internal data class ContentFingerprint(
    val sha256: String,
    val md5: String,
)

internal data class InspectedContent(
    val contentType: ContentType,
    val gameIdentity: GameIdentity? = null,
    val suggestedName: String? = null,
    val detectedRequirements: Set<String> = emptySet(),
)

internal class ContentInspectionException(message: String) : IllegalArgumentException(message)

internal object ContentInspector {
    const val MAX_IMPORT_BYTES = 512L * 1024L * 1024L

    fun inspect(file: File, fingerprint: ContentFingerprint): InspectedContent {
        if (!file.isFile || file.length() == 0L) {
            throw ContentInspectionException("The selected document is empty.")
        }
        if (file.length() > MAX_IMPORT_BYTES) {
            throw ContentInspectionException("The selected document is larger than 512 MiB.")
        }

        val signature = ByteArray(minOf(4, file.length().toInt()))
        file.inputStream().use { it.read(signature) }
        val four = signature.toString(StandardCharsets.US_ASCII)
        return when {
            four == "IWAD" || four == "PWAD" ->
                WadInspector.inspect(file, fingerprint)

            signature.isZipSignature() ->
                ArchiveInspector.inspect(file)

            else -> PatchInspector.inspect(file)
        }
    }

    private fun ByteArray.isZipSignature(): Boolean =
        size == 4 &&
            this[0] == 'P'.code.toByte() &&
            this[1] == 'K'.code.toByte() &&
            (
                this[2] == 3.toByte() && this[3] == 4.toByte() ||
                    this[2] == 5.toByte() && this[3] == 6.toByte() ||
                    this[2] == 7.toByte() && this[3] == 8.toByte()
                )
}

internal object KnownIwadCatalogue {
    const val VERSION = 1

    private data class Record(
        val md5: String,
        val identity: GameIdentity,
        val name: String,
    )

    private val records = listOf(
        Record("1cd63c5ddff1bf8ce844237f580e9cf3", GameIdentity.DOOM, "Doom"),
        Record("c4fe9fd920207691a9f493668e0a2083", GameIdentity.DOOM, "The Ultimate Doom"),
        Record("25e1459ca71d321525f84628f45ca8cd", GameIdentity.DOOM2, "Doom II"),
        Record("4e158d9953c79ccf97bd0663244cc6b6", GameIdentity.TNT, "TNT: Evilution"),
        Record(
            "75c8cf89566741fa9d22447604053bd7",
            GameIdentity.PLUTONIA,
            "The Plutonia Experiment",
        ),
        Record(
            "b93be13d05148dd01614bc205a03648e",
            GameIdentity.FREEDOOM_PHASE1,
            "Freedoom: Phase 1",
        ),
        Record(
            "cd666466759b5e5f63af93c5f0ffd0a1",
            GameIdentity.FREEDOOM_PHASE2,
            "Freedoom: Phase 2",
        ),
    ).associateBy(Record::md5)

    fun identify(fingerprint: ContentFingerprint): Pair<GameIdentity, String>? {
        val record = records[fingerprint.md5.lowercase(Locale.ROOT)] ?: return null
        return record.identity to record.name
    }
}

internal object WadInspector {
    private const val HEADER_SIZE = 12L
    private const val DIRECTORY_ENTRY_SIZE = 16L
    private const val MAX_LUMPS = 200_000

    private data class Lump(
        val name: String,
        val offset: Long,
        val size: Long,
    )

    fun inspect(file: File, fingerprint: ContentFingerprint): InspectedContent {
        if (file.length() < HEADER_SIZE) {
            throw ContentInspectionException("This is not a complete Doom WAD.")
        }
        RandomAccessFile(file, "r").use { input ->
            val magicBytes = ByteArray(4)
            input.readFully(magicBytes)
            val magic = magicBytes.toString(StandardCharsets.US_ASCII)
            if (magic != "IWAD" && magic != "PWAD") {
                throw ContentInspectionException("The Doom WAD signature is invalid.")
            }
            val lumpCount = input.readLittleEndianInt().toLong()
            val directoryOffset = input.readLittleEndianInt().toLong()
            if (lumpCount !in 1..MAX_LUMPS.toLong()) {
                throw ContentInspectionException("The Doom WAD contains an invalid lump count.")
            }
            val directorySize = multiplyExact(lumpCount, DIRECTORY_ENTRY_SIZE)
            if (directoryOffset < HEADER_SIZE ||
                directoryOffset > file.length() ||
                directorySize > file.length() - directoryOffset
            ) {
                throw ContentInspectionException("The Doom WAD directory is outside the file.")
            }

            input.seek(directoryOffset)
            val lumps = ArrayList<Lump>(lumpCount.toInt())
            repeat(lumpCount.toInt()) {
                val offset = input.readLittleEndianInt().toLong()
                val size = input.readLittleEndianInt().toLong()
                val nameBytes = ByteArray(8)
                input.readFully(nameBytes)
                if (offset < 0L || size < 0L || offset > file.length() ||
                    size > file.length() - offset
                ) {
                    throw ContentInspectionException(
                        "A Doom WAD lump points outside the selected document.",
                    )
                }
                val name = decodeLumpName(nameBytes)
                lumps += Lump(name, offset, size)
            }
            return classify(magic, lumps, fingerprint)
        }
    }

    private fun classify(
        magic: String,
        lumps: List<Lump>,
        fingerprint: ContentFingerprint,
    ): InspectedContent {
        val names = lumps.asSequence().map(Lump::name).toHashSet()
        val requirements = buildSet {
            if ("ZSCRIPT" in names) add("ZScript")
            if ("DECORATE" in names) add("DECORATE")
        }
        if (magic == "PWAD") {
            return InspectedContent(
                contentType = ContentType.MOD_WAD,
                detectedRequirements = requirements,
            )
        }

        val hasCorePalette = "PLAYPAL" in names && "COLORMAP" in names
        val hasEpisodeMaps = names.any { EPISODE_MAP_PATTERN.matches(it) }
        val hasCommercialMaps = names.any { COMMERCIAL_MAP_PATTERN.matches(it) }
        if (!hasCorePalette || (!hasEpisodeMaps && !hasCommercialMaps)) {
            throw ContentInspectionException(
                "This IWAD does not contain a supported Doom game layout.",
            )
        }

        val known = KnownIwadCatalogue.identify(fingerprint)
        val identity = known?.first ?: when {
            "FREEDOOM" in names && hasEpisodeMaps -> GameIdentity.FREEDOOM_PHASE1
            "FREEDOOM" in names && hasCommercialMaps -> GameIdentity.FREEDOOM_PHASE2
            hasEpisodeMaps -> GameIdentity.DOOM
            else -> GameIdentity.DOOM2
        }
        val suggestedName = known?.second ?: when (identity) {
            GameIdentity.DOOM -> "Doom"
            GameIdentity.DOOM2 -> "Doom II-compatible game"
            GameIdentity.TNT -> "TNT: Evilution"
            GameIdentity.PLUTONIA -> "The Plutonia Experiment"
            GameIdentity.FREEDOOM_PHASE1 -> "Freedoom: Phase 1"
            GameIdentity.FREEDOOM_PHASE2 -> "Freedoom: Phase 2"
        }
        return InspectedContent(
            contentType = ContentType.GAME_WAD,
            gameIdentity = identity,
            suggestedName = suggestedName,
            detectedRequirements = requirements,
        )
    }

    private fun decodeLumpName(bytes: ByteArray): String {
        val end = bytes.indexOf(0).let { if (it < 0) bytes.size else it }
        if (end == 0 || bytes.take(end).any { (it.toInt() and 0xff) !in 32..126 }) {
            throw ContentInspectionException("The Doom WAD contains an invalid lump name.")
        }
        return bytes.copyOf(end).toString(StandardCharsets.US_ASCII).uppercase(Locale.ROOT)
    }

    private fun RandomAccessFile.readLittleEndianInt(): Int =
        Integer.reverseBytes(readInt())

    private fun multiplyExact(left: Long, right: Long): Long =
        try {
            Math.multiplyExact(left, right)
        } catch (_: ArithmeticException) {
            throw ContentInspectionException("The Doom WAD directory is too large.")
        }

    private val EPISODE_MAP_PATTERN = Regex("E[1-9]M[1-9]")
    private val COMMERCIAL_MAP_PATTERN = Regex("MAP[0-9][0-9]")
}

internal object ArchiveInspector {
    private const val MAX_ENTRIES = 4_096
    private const val MAX_NAME_LENGTH = 240
    private const val MAX_ENTRY_BYTES = 256L * 1024L * 1024L
    private const val MAX_TOTAL_UNCOMPRESSED_BYTES = 1024L * 1024L * 1024L
    private const val MAX_COMPRESSION_RATIO = 200L

    fun inspect(file: File): InspectedContent {
        try {
            ZipFile(file).use { archive ->
                var count = 0
                var totalSize = 0L
                var recognized = false
                val requirements = mutableSetOf<String>()
                val entries = archive.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    count++
                    if (count > MAX_ENTRIES) {
                        throw ContentInspectionException(
                            "The archive contains too many entries.",
                        )
                    }
                    validateName(entry.name)
                    if (entry.isDirectory) continue
                    val size = entry.size
                    val compressedSize = entry.compressedSize
                    if (size < 0L || compressedSize < 0L || size > MAX_ENTRY_BYTES) {
                        throw ContentInspectionException(
                            "The archive contains an entry with an unsafe size.",
                        )
                    }
                    totalSize = try {
                        Math.addExact(totalSize, size)
                    } catch (_: ArithmeticException) {
                        throw ContentInspectionException("The archive expands beyond safe limits.")
                    }
                    if (totalSize > MAX_TOTAL_UNCOMPRESSED_BYTES ||
                        compressedSize > 0L && size / compressedSize > MAX_COMPRESSION_RATIO
                    ) {
                        throw ContentInspectionException("The archive expands beyond safe limits.")
                    }
                    val normalized = entry.name.lowercase(Locale.ROOT)
                    recognized = recognized || isRecognizedModEntry(normalized)
                    if (normalized == "zscript" || normalized.endsWith("/zscript")) {
                        requirements += "ZScript"
                    }
                    if (normalized == "decorate" || normalized.endsWith("/decorate")) {
                        requirements += "DECORATE"
                    }
                }
                if (count == 0 || !recognized) {
                    throw ContentInspectionException(
                        "The archive does not contain recognizable Doom mod data.",
                    )
                }
                return InspectedContent(
                    contentType = ContentType.MOD_ARCHIVE,
                    detectedRequirements = requirements,
                )
            }
        } catch (error: ZipException) {
            throw ContentInspectionException("The selected archive is malformed.")
        }
    }

    private fun validateName(name: String) {
        val segments = name.replace('\\', '/').split('/')
        if (name.isBlank() || name.length > MAX_NAME_LENGTH || name.startsWith('/') ||
            segments.any { it == ".." || it.contains('\u0000') }
        ) {
            throw ContentInspectionException("The archive contains an unsafe entry name.")
        }
    }

    private fun isRecognizedModEntry(name: String): Boolean =
        name.endsWith(".wad") ||
            name == "mapinfo" ||
            name == "umapinfo" ||
            name == "decorate" ||
            name == "zscript" ||
            RECOGNIZED_DIRECTORIES.any(name::startsWith)

    private val RECOGNIZED_DIRECTORIES = listOf(
        "maps/",
        "sprites/",
        "textures/",
        "flats/",
        "sounds/",
        "music/",
        "graphics/",
    )
}

internal object PatchInspector {
    private const val MAX_PATCH_BYTES = 16L * 1024L * 1024L

    fun inspect(file: File): InspectedContent {
        if (file.length() > MAX_PATCH_BYTES) {
            throw ContentInspectionException("The patch file is larger than 16 MiB.")
        }
        val text = file.readText(StandardCharsets.ISO_8859_1)
        if (text.indexOf('\u0000') >= 0) {
            throw ContentInspectionException("The selected document is not a text patch.")
        }
        val normalized = text.trimStart()
        val recognized =
            normalized.startsWith("Patch File for DeHackEd v") ||
                DEH_FIELD_PATTERN.containsMatchIn(text) ||
                BEX_SECTION_PATTERN.containsMatchIn(text)
        if (!recognized) {
            throw ContentInspectionException(
                "The selected document is not a recognizable DEH or BEX patch.",
            )
        }
        return InspectedContent(contentType = ContentType.PATCH)
    }

    private val DEH_FIELD_PATTERN =
        Regex("(?im)^(Doom version|Patch format)\\s*=\\s*\\d+\\s*$")
    private val BEX_SECTION_PATTERN =
        Regex("(?im)^\\[(STRINGS|PARS|CODEPTR|HELPER|SPRITES|SOUNDS)\\]\\s*$")
}
