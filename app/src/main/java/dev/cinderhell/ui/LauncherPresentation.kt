package dev.cinderhell.ui

import dev.cinderhell.launcher.LauncherSnapshot
import dev.cinderhell.profile.ProfilePresets

internal enum class LauncherNoticeTone {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
}

internal data class LauncherNotice(
    val message: String,
    val tone: LauncherNoticeTone,
) {
    companion object {
        fun info(message: String) = LauncherNotice(message, LauncherNoticeTone.INFO)
        fun success(message: String) = LauncherNotice(message, LauncherNoticeTone.SUCCESS)
        fun warning(message: String) = LauncherNotice(message, LauncherNoticeTone.WARNING)
        fun error(message: String) = LauncherNotice(message, LauncherNoticeTone.ERROR)
    }
}

internal data class HomePresentation(
    val gameName: String,
    val profileName: String,
    val presetName: String,
    val modSummary: String,
    val playLabel: String,
    val continueTitle: String?,
    val continueDetail: String?,
)

internal fun LauncherSnapshot.homePresentation(): HomePresentation? {
    val selected = profiles.singleOrNull {
        it.profile.profileId == selectedProfileId
    } ?: return null
    val game = content.singleOrNull {
        it.contentId == selected.profile.gameContentId
    } ?: return null
    val preset = runCatching {
        ProfilePresets.require(
            selected.profile.presetId,
            selected.profile.presetVersion,
        )
    }.getOrNull()
    val modCount = selected.orderedEntries.size
    val recent = continueSummary
    return HomePresentation(
        gameName = game.displayName,
        profileName = selected.profile.name,
        presetName = preset?.displayName ?: "Custom",
        modSummary = when (modCount) {
            0 -> "No added mods"
            1 -> "1 added mod"
            else -> "$modCount added mods"
        },
        playLabel = "Play ${selected.profile.name}",
        continueTitle = recent?.let { "${it.gameName} — ${it.latestLevel}" },
        continueDetail = recent?.profileName,
    )
}

internal fun formatContentSize(byteSize: Long): String {
    require(byteSize >= 0) { "Content size cannot be negative." }
    val mib = byteSize / (1024.0 * 1024.0)
    return if (mib >= 1.0) {
        String.format(java.util.Locale.ROOT, "%.1f MiB", mib)
    } else {
        "${byteSize / 1024} KiB"
    }
}

internal fun contentTypeLabel(name: String): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
