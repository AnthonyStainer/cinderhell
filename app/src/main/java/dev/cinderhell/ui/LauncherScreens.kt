@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.cinderhell.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.cinderhell.input.ControllerDeviceState
import dev.cinderhell.launcher.LauncherSnapshot
import dev.cinderhell.library.ContentItemEntity
import dev.cinderhell.library.ContentRemovalPlan
import dev.cinderhell.library.ProfileWithEntries
import dev.cinderhell.profile.PresetId
import dev.cinderhell.profile.PresetReapplyPreview
import dev.cinderhell.profile.ProfileEntryDraft
import dev.cinderhell.profile.ProfilePresets

internal sealed interface LauncherRoute {
    data object Home : LauncherRoute
    data class ProfileEditor(val profileId: String?) : LauncherRoute
    data class Advanced(val profileId: String) : LauncherRoute
    data object Library : LauncherRoute
    data object Notices : LauncherRoute
}

internal data class ProfileSaveRequest(
    val profileId: String?,
    val name: String,
    val gameContentId: String,
    val presetId: PresetId,
    val orderedContentIds: List<String>,
)

@Composable
internal fun LauncherScreen(
    snapshot: LauncherSnapshot?,
    route: LauncherRoute,
    busy: Boolean,
    statusMessage: String?,
    controller: ControllerDeviceState,
    focusedId: String?,
    removalPlan: ContentRemovalPlan?,
    presetPreview: PresetReapplyPreview?,
    noticesText: String,
    onFocused: (String) -> Unit,
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onImport: () -> Unit,
    onSelectGame: (String) -> Unit,
    onSelectProfile: (String) -> Unit,
    onRoute: (LauncherRoute) -> Unit,
    onSaveProfile: (ProfileSaveRequest) -> Unit,
    onRequestRemoval: (String) -> Unit,
    onConfirmRemoval: () -> Unit,
    onDismissRemoval: () -> Unit,
    onRequestPreset: (String, PresetId) -> Unit,
    onConfirmPreset: () -> Unit,
    onDismissPreset: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        if (snapshot == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CINDERHELL", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator()
                    statusMessage?.let {
                        Spacer(Modifier.height(16.dp))
                        Text(it)
                    }
                }
            }
            return@Surface
        }

        BackHandler(enabled = route != LauncherRoute.Home) {
            onRoute(LauncherRoute.Home)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 18.dp),
        ) {
            Header(controller)
            Spacer(Modifier.height(12.dp))
            statusMessage?.let {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            when (route) {
                LauncherRoute.Home -> HomeScreen(
                    snapshot = snapshot,
                    busy = busy,
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onPlay = onPlay,
                    onContinue = onContinue,
                    onImport = onImport,
                    onSelectGame = onSelectGame,
                    onSelectProfile = onSelectProfile,
                    onRoute = onRoute,
                )

                is LauncherRoute.ProfileEditor -> ProfileEditorScreen(
                    snapshot = snapshot,
                    route = route,
                    busy = busy,
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onSave = onSaveProfile,
                    onBack = { onRoute(LauncherRoute.Home) },
                )

                is LauncherRoute.Advanced -> AdvancedScreen(
                    snapshot = snapshot,
                    route = route,
                    busy = busy,
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onRequestPreset = onRequestPreset,
                    onBack = { onRoute(LauncherRoute.Home) },
                )

                LauncherRoute.Library -> LibraryScreen(
                    snapshot = snapshot,
                    busy = busy,
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onImport = onImport,
                    onRequestRemoval = onRequestRemoval,
                    onBack = { onRoute(LauncherRoute.Home) },
                )

                LauncherRoute.Notices -> NoticesScreen(
                    noticesText = noticesText,
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onBack = { onRoute(LauncherRoute.Home) },
                )
            }
        }
    }

    removalPlan?.let { plan ->
        ConfirmationDialog(
            title = "Remove ${plan.content.displayName}?",
            body = if (plan.affectedProfiles.isEmpty()) {
                "The private imported copy will be removed."
            } else {
                "This also affects: ${plan.affectedProfiles.joinToString { it.name }}."
            },
            confirmId = "confirm-removal",
            focusedId = focusedId,
            onFocused = onFocused,
            onConfirm = onConfirmRemoval,
            onDismiss = onDismissRemoval,
        )
    }
    presetPreview?.let { preview ->
        ConfirmationDialog(
            title = "Apply ${preview.preset.displayName}?",
            body = if (preview.changes.isEmpty()) {
                "The curated settings already match this preset."
            } else {
                "${preview.changes.size} curated settings will change. Other in-game settings are preserved."
            },
            confirmId = "confirm-preset",
            focusedId = focusedId,
            onFocused = onFocused,
            onConfirm = onConfirmPreset,
            onDismiss = onDismissPreset,
        )
    }
}

@Composable
private fun Header(controller: ControllerDeviceState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "CINDERHELL",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.weight(1f))
        Text(
            if (controller.connected) {
                "● ${controller.name ?: "Controller"}"
            } else {
                "○ Controller disconnected"
            },
            color = if (controller.connected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun HomeScreen(
    snapshot: LauncherSnapshot,
    busy: Boolean,
    focusedId: String?,
    onFocused: (String) -> Unit,
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onImport: () -> Unit,
    onSelectGame: (String) -> Unit,
    onSelectProfile: (String) -> Unit,
    onRoute: (LauncherRoute) -> Unit,
) {
    val selected = snapshot.profiles.singleOrNull {
        it.profile.profileId == snapshot.selectedProfileId
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (snapshot.firstRun) {
            Text("One great Doom engine. Your games, one button away.")
            Text(
                "Freedoom is ready now. Import a Doom game whenever you like.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        snapshot.continueSummary?.let { recent ->
            Text("Continue", style = MaterialTheme.typography.titleLarge)
            ControllerButton(
                id = "continue",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onContinue,
                enabled = !busy,
            ) {
                Column {
                    Text("${recent.gameName} — ${recent.latestLevel}")
                    Text(recent.profileName, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Text("Ready", style = MaterialTheme.typography.titleLarge)
        ControllerButton(
            id = "play",
            focusedId = focusedId,
            onFocused = onFocused,
            onClick = onPlay,
            enabled = !busy && selected != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Preparing…" else "Play ${selected?.profile?.name.orEmpty()}")
        }
        Text("Your games", style = MaterialTheme.typography.titleLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            snapshot.games.forEach { game ->
                val selectedGame = selected?.profile?.gameContentId == game.contentId
                ControllerButton(
                    id = "game-${game.contentId}",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = { onSelectGame(game.contentId) },
                    enabled = !busy,
                ) {
                    Text(if (selectedGame) "✓ ${game.displayName}" else game.displayName)
                }
            }
        }
        Text("Mod profiles", style = MaterialTheme.typography.titleLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            snapshot.profiles.forEach { profile ->
                ControllerButton(
                    id = "profile-${profile.profile.profileId}",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = { onSelectProfile(profile.profile.profileId) },
                    enabled = !busy,
                ) {
                    Text(
                        if (profile.profile.selected) {
                            "✓ ${profile.profile.name}"
                        } else {
                            profile.profile.name
                        },
                    )
                }
            }
            ControllerButton(
                id = "add-profile",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = { onRoute(LauncherRoute.ProfileEditor(null)) },
                enabled = !busy,
            ) {
                Text("Add mod set")
            }
            selected?.let { profile ->
                ControllerButton(
                    id = "edit-profile",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = {
                        onRoute(LauncherRoute.ProfileEditor(profile.profile.profileId))
                    },
                    enabled = !busy,
                ) {
                    Text("Edit")
                }
            }
        }
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControllerButton(
                id = "import",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onImport,
                enabled = !busy,
            ) {
                Text("Import game or mod")
            }
            ControllerButton(
                id = "library",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = { onRoute(LauncherRoute.Library) },
                enabled = !busy,
            ) {
                Text("Library")
            }
            ControllerButton(
                id = "notices",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = { onRoute(LauncherRoute.Notices) },
                enabled = !busy,
            ) {
                Text("Notices")
            }
            selected?.let {
                ControllerButton(
                    id = "advanced",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = {
                        onRoute(LauncherRoute.Advanced(it.profile.profileId))
                    },
                    enabled = !busy,
                ) {
                    Text("Advanced")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NoticesScreen(
    noticesText: String,
    focusedId: String?,
    onFocused: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Open-source notices", style = MaterialTheme.typography.headlineMedium)
        Text(noticesText, style = MaterialTheme.typography.bodySmall)
        ControllerButton(
            id = "notices-back",
            focusedId = focusedId,
            onFocused = onFocused,
            onClick = onBack,
        ) { Text("Back") }
    }
}

@Composable
private fun ProfileEditorScreen(
    snapshot: LauncherSnapshot,
    route: LauncherRoute.ProfileEditor,
    busy: Boolean,
    focusedId: String?,
    onFocused: (String) -> Unit,
    onSave: (ProfileSaveRequest) -> Unit,
    onBack: () -> Unit,
) {
    val existing = snapshot.profiles.singleOrNull {
        it.profile.profileId == route.profileId
    }
    val defaultGame = existing?.profile?.gameContentId
        ?: snapshot.profiles.singleOrNull { it.profile.selected }?.profile?.gameContentId
        ?: snapshot.games.firstOrNull()?.contentId.orEmpty()
    var name by remember(route.profileId) {
        mutableStateOf(existing?.profile?.name ?: "New mod set")
    }
    var gameId by remember(route.profileId, snapshot.games.size) {
        mutableStateOf(defaultGame)
    }
    var presetId by remember(route.profileId) {
        mutableStateOf(
            existing?.profile?.presetId
                ?.let { wire -> PresetId.entries.single { it.wireValue == wire } }
                ?: PresetId.HANDHELD,
        )
    }
    var entries by remember(route.profileId, snapshot.additions.size) {
        mutableStateOf(
            existing?.orderedEntries.orEmpty().map {
                ProfileEntryDraft(it.entry.entryId, it.entry.contentId)
            },
        )
    }
    val contentById = snapshot.content.associateBy(ContentItemEntity::contentId)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (existing == null) "Create mod profile" else "Edit ${existing.profile.name}",
            style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(80) },
            label = { Text("Profile name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Game", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            snapshot.games.forEach { game ->
                ControllerButton(
                    id = "editor-game-${game.contentId}",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = { gameId = game.contentId },
                    enabled = !busy,
                ) {
                    Text(if (gameId == game.contentId) "✓ ${game.displayName}" else game.displayName)
                }
            }
        }
        if (existing == null) {
            Text("Preset", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfilePresets.all.forEach { preset ->
                    ControllerButton(
                        id = "editor-preset-${preset.id.wireValue}",
                        focusedId = focusedId,
                        onFocused = onFocused,
                        onClick = { presetId = preset.id },
                        enabled = !busy,
                    ) {
                        Text(if (presetId == preset.id) "✓ ${preset.displayName}" else preset.displayName)
                    }
                }
            }
        } else {
            Text(
                "Preset: ${ProfilePresets.require(existing.profile.presetId, existing.profile.presetVersion).displayName}. Change it from Advanced.",
            )
        }
        Text("Load order", style = MaterialTheme.typography.titleMedium)
        if (entries.isEmpty()) Text("No mods or patches. The game loads by itself.")
        entries.forEachIndexed { index, draft ->
            val item = contentById[draft.contentId]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("${index + 1}. ${item?.displayName ?: "Missing item"}", Modifier.weight(1f))
                ControllerButton(
                    id = "entry-up-${draft.contentId}",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = {
                        entries = move(entries, index, index - 1)
                    },
                    enabled = !busy && index > 0,
                ) { Text("Up") }
                ControllerButton(
                    id = "entry-down-${draft.contentId}",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = {
                        entries = move(entries, index, index + 1)
                    },
                    enabled = !busy && index < entries.lastIndex,
                ) { Text("Down") }
                ControllerButton(
                    id = "entry-remove-${draft.contentId}",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = {
                        entries = entries.filterNot { it.contentId == draft.contentId }
                    },
                    enabled = !busy,
                ) { Text("Remove") }
            }
        }
        if (snapshot.additions.any { candidate ->
                entries.none { it.contentId == candidate.contentId }
            }
        ) {
            Text("Available additions", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                snapshot.additions
                    .filter { candidate -> entries.none { it.contentId == candidate.contentId } }
                    .forEach { item ->
                        ControllerButton(
                            id = "entry-add-${item.contentId}",
                            focusedId = focusedId,
                            onFocused = onFocused,
                            onClick = {
                                entries = entries + ProfileEntryDraft(contentId = item.contentId)
                            },
                            enabled = !busy,
                        ) { Text("Add ${item.displayName}") }
                    }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControllerButton(
                id = "save-profile",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = {
                    onSave(
                        ProfileSaveRequest(
                            profileId = route.profileId,
                            name = name,
                            gameContentId = gameId,
                            presetId = presetId,
                            orderedContentIds = entries.map(ProfileEntryDraft::contentId),
                        ),
                    )
                },
                enabled = !busy && name.isNotBlank() && gameId.isNotBlank(),
            ) { Text("Save profile") }
            ControllerButton(
                id = "profile-back",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onBack,
                enabled = !busy,
            ) { Text("Back") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AdvancedScreen(
    snapshot: LauncherSnapshot,
    route: LauncherRoute.Advanced,
    busy: Boolean,
    focusedId: String?,
    onFocused: (String) -> Unit,
    onRequestPreset: (String, PresetId) -> Unit,
    onBack: () -> Unit,
) {
    val profile = snapshot.profiles.singleOrNull {
        it.profile.profileId == route.profileId
    }
    if (profile == null) {
        Text("This profile is no longer available.")
        return
    }
    val current = ProfilePresets.require(
        profile.profile.presetId,
        profile.profile.presetVersion,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Advanced — ${profile.profile.name}", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Current preset: ${current.displayName}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Reapplying a preset changes only Cinderhell's curated video, audio, and controller values. Your other in-game settings remain untouched.",
        )
        ProfilePresets.all.forEach { preset ->
            ControllerButton(
                id = "apply-${preset.id.wireValue}",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = { onRequestPreset(profile.profile.profileId, preset.id) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        if (current.id == preset.id) {
                            "${preset.displayName} (current)"
                        } else {
                            preset.displayName
                        },
                    )
                    Text(preset.description, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Text("More supported settings", style = MaterialTheme.typography.titleMedium)
        Text(
            "Rendering, audio, HUD, controller curves, deadzones, rumble, gyro, and input bindings remain available from Options inside the game.",
        )
        ControllerButton(
            id = "advanced-back",
            focusedId = focusedId,
            onFocused = onFocused,
            onClick = onBack,
            enabled = !busy,
        ) { Text("Back") }
    }
}

@Composable
private fun LibraryScreen(
    snapshot: LauncherSnapshot,
    busy: Boolean,
    focusedId: String?,
    onFocused: (String) -> Unit,
    onImport: () -> Unit,
    onRequestRemoval: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Library", style = MaterialTheme.typography.headlineMedium)
        snapshot.content.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${item.contentType.name.lowercase().replace('_', ' ')} · ${item.byteSize / 1024} KiB",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (item.bundled) {
                    Text("Included")
                } else {
                    ControllerButton(
                        id = "remove-${item.contentId}",
                        focusedId = focusedId,
                        onFocused = onFocused,
                        onClick = { onRequestRemoval(item.contentId) },
                        enabled = !busy,
                    ) { Text("Remove") }
                }
            }
            HorizontalDivider()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControllerButton(
                id = "library-import",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onImport,
                enabled = !busy,
            ) { Text("Import") }
            ControllerButton(
                id = "library-back",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onBack,
                enabled = !busy,
            ) { Text("Back") }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    body: String,
    confirmId: String,
    focusedId: String?,
    onFocused: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            ControllerButton(
                id = confirmId,
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onConfirm,
            ) { Text("Confirm") }
        },
        dismissButton = {
            ControllerButton(
                id = "$confirmId-cancel",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onDismiss,
            ) { Text("Cancel") }
        },
    )
}

private fun move(
    entries: List<ProfileEntryDraft>,
    from: Int,
    to: Int,
): List<ProfileEntryDraft> {
    if (from !in entries.indices || to !in entries.indices || from == to) return entries
    return entries.toMutableList().apply { add(to, removeAt(from)) }
}
