package dev.cinderhell.profile

import dev.cinderhell.library.ContentBlobStore
import dev.cinderhell.library.ProfileEntity
import dev.cinderhell.session.AtomicFiles
import java.io.File

internal data class ConfigSettingChange(
    val key: String,
    val currentValue: Int?,
    val presetValue: Int,
)

internal data class PresetReapplyPreview(
    val profileId: String,
    val configPath: String,
    val sourceDigest: String?,
    val preset: ProfilePreset,
    val changes: List<ConfigSettingChange>,
)

internal class ProfileConfigService {
    fun ensureMaterialized(profile: ProfileEntity, preset: ProfilePreset): File {
        val config = File(profile.configPath)
        checkNotNull(config.parentFile).mkdirs()
        if (!config.exists()) {
            AtomicFiles.writeUtf8(config, newConfig(preset))
        }
        check(config.isFile) { "The profile configuration is unavailable." }
        return config
    }

    fun previewReapply(profile: ProfileEntity, preset: ProfilePreset): PresetReapplyPreview {
        val config = File(profile.configPath)
        val values = if (config.isFile) parseValues(config.readText()) else emptyMap()
        return PresetReapplyPreview(
            profileId = profile.profileId,
            configPath = config.canonicalPath,
            sourceDigest = config.takeIf(File::isFile)?.let(ContentBlobStore::sha256),
            preset = preset,
            changes = preset.settings.mapNotNull { (key, value) ->
                if (values[key] == value) {
                    null
                } else {
                    ConfigSettingChange(key, values[key], value)
                }
            },
        )
    }

    fun applyPreview(preview: PresetReapplyPreview) {
        val config = File(preview.configPath)
        val currentDigest = config.takeIf(File::isFile)?.let(ContentBlobStore::sha256)
        check(currentDigest == preview.sourceDigest) {
            "The in-game settings changed; preview the preset again."
        }
        val currentText = config.takeIf(File::isFile)?.readText().orEmpty()
        val remaining = preview.preset.settings.toMutableMap()
        val output = buildList {
            currentText.lineSequence().forEach { line ->
                val match = CONFIG_LINE.matchEntire(line)
                val key = match?.groupValues?.get(2)
                if (key != null && key in remaining) {
                    val value = checkNotNull(remaining.remove(key))
                    add("${match.groupValues[1]}$key${match.groupValues[3]}$value")
                } else {
                    add(line)
                }
            }
            if (isNotEmpty() && last().isNotBlank() && remaining.isNotEmpty()) add("")
            remaining.forEach { (key, value) -> add("$key $value") }
        }.joinToString("\n").trimEnd() + "\n"
        AtomicFiles.writeUtf8(config, output)
    }

    private fun newConfig(preset: ProfilePreset): String = buildString {
        appendLine("# Cinderhell ${preset.displayName} preset v${preset.version}")
        appendLine("# In-game changes are preserved until you explicitly reapply a preset.")
        preset.settings.forEach { (key, value) ->
            appendLine("$key $value")
        }
    }

    private fun parseValues(text: String): Map<String, Int> =
        text.lineSequence().mapNotNull { line ->
            val match = CONFIG_LINE.matchEntire(line) ?: return@mapNotNull null
            match.groupValues[2] to match.groupValues[4].trim().toIntOrNull()
        }.filter { it.second != null }
            .associate { it.first to checkNotNull(it.second) }

    private companion object {
        val CONFIG_LINE = Regex("^(\\s*)([A-Za-z0-9_.]+)(\\s+)(.*?)\\s*$")
    }
}
