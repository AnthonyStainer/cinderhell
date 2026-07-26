@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.cinderhell.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.cinderhell.input.ControllerDeviceState
import dev.cinderhell.launcher.LauncherSnapshot
import dev.cinderhell.library.ContentItemEntity
import dev.cinderhell.library.ContentRemovalPlan
import dev.cinderhell.profile.PresetId
import dev.cinderhell.profile.PresetReapplyPreview
import dev.cinderhell.profile.ProfileEntryDraft
import dev.cinderhell.profile.ProfilePresets
import dev.cinderhell.ui.theme.CinderhellSpacing

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
    statusNotice: LauncherNotice?,
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
    CinderhellBackdrop {
        if (snapshot == null) {
            LoadingLauncher(statusNotice)
        } else {
            BackHandler(enabled = route != LauncherRoute.Home) {
                onRoute(LauncherRoute.Home)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = CinderhellSpacing.PageHorizontal,
                        vertical = CinderhellSpacing.PageVertical,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CinderhellWordmark(
                    controllerLabel = if (controller.connected) {
                        controller.name ?: "Controller ready"
                    } else {
                        "Controller disconnected"
                    },
                    controllerConnected = controller.connected,
                )
                LauncherStatusBanner(statusNotice, busy)
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
            destructive = true,
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
    val presentation = snapshot.homePresentation()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(CinderhellSpacing.Section),
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.88f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                ) {
                    PlayHero(
                        snapshot = snapshot,
                        presentation = presentation,
                        busy = busy,
                        focusedId = focusedId,
                        onFocused = onFocused,
                        onPlay = onPlay,
                        onContinue = onContinue,
                    )
                    Spacer(Modifier.height(CinderhellSpacing.Section))
                }
                Column(
                    modifier = Modifier
                        .weight(1.12f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(CinderhellSpacing.Section),
                ) {
                    SelectionPanels(
                        snapshot = snapshot,
                        selectedProfileId = selected?.profile?.profileId,
                        selectedGameId = selected?.profile?.gameContentId,
                        busy = busy,
                        focusedId = focusedId,
                        onFocused = onFocused,
                        onSelectGame = onSelectGame,
                        onSelectProfile = onSelectProfile,
                        onRoute = onRoute,
                    )
                    UtilityActions(
                        selectedProfileId = selected?.profile?.profileId,
                        busy = busy,
                        focusedId = focusedId,
                        onFocused = onFocused,
                        onImport = onImport,
                        onRoute = onRoute,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CinderhellSpacing.Section),
            ) {
                PlayHero(
                    snapshot = snapshot,
                    presentation = presentation,
                    busy = busy,
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onPlay = onPlay,
                    onContinue = onContinue,
                )
                SelectionPanels(
                    snapshot = snapshot,
                    selectedProfileId = selected?.profile?.profileId,
                    selectedGameId = selected?.profile?.gameContentId,
                    busy = busy,
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onSelectGame = onSelectGame,
                    onSelectProfile = onSelectProfile,
                    onRoute = onRoute,
                )
                UtilityActions(
                    selectedProfileId = selected?.profile?.profileId,
                    busy = busy,
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onImport = onImport,
                    onRoute = onRoute,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PlayHero(
    snapshot: LauncherSnapshot,
    presentation: HomePresentation?,
    busy: Boolean,
    focusedId: String?,
    onFocused: (String) -> Unit,
    onPlay: () -> Unit,
    onContinue: () -> Unit,
) {
    EmberPanel(modifier = Modifier.fillMaxWidth(), highlighted = true) {
        if (snapshot.firstRun) {
            MetadataPill("Ready out of the box", accent = true)
            Text(
                "One great Doom engine. Your games, one button away.",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Freedoom is ready now. Import a Doom game whenever you like.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        RouteHeading(
            eyebrow = "Ready to play",
            title = presentation?.gameName ?: "Choose a game",
            detail = presentation?.profileName
                ?: "Select an installed game or create a profile.",
        )
        presentation?.let {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetadataPill(it.presetName, accent = true)
                MetadataPill(it.modSummary)
                MetadataPill("Woof")
            }
        }
        ControllerButton(
            id = "play",
            focusedId = focusedId,
            onFocused = onFocused,
            onClick = onPlay,
            enabled = !busy && presentation != null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
            role = ControllerButtonRole.PRIMARY,
        ) {
            Text(
                if (busy) "Preparing…" else presentation?.playLabel ?: "Choose a profile",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        presentation?.continueTitle?.let { continueTitle ->
            SectionHeading("Continue", "Latest save")
            ControllerButton(
                id = "continue",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onContinue,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                role = ControllerButtonRole.SECONDARY,
            ) {
                Column {
                    Text(continueTitle, style = MaterialTheme.typography.titleMedium)
                    presentation.continueDetail?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionPanels(
    snapshot: LauncherSnapshot,
    selectedProfileId: String?,
    selectedGameId: String?,
    busy: Boolean,
    focusedId: String?,
    onFocused: (String) -> Unit,
    onSelectGame: (String) -> Unit,
    onSelectProfile: (String) -> Unit,
    onRoute: (LauncherRoute) -> Unit,
) {
    EmberPanel(modifier = Modifier.fillMaxWidth()) {
        SectionHeading("Your games", "${snapshot.games.size} installed")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            snapshot.games.forEach { game ->
                val selected = selectedGameId == game.contentId
                ControllerButton(
                    id = "game-${game.contentId}",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = { onSelectGame(game.contentId) },
                    enabled = !busy,
                    selected = selected,
                    modifier = Modifier.widthIn(min = 150.dp, max = 260.dp),
                    role = ControllerButtonRole.QUIET,
                ) {
                    Column {
                        if (selected) {
                            Text("SELECTED", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            game.displayName,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(CinderhellSpacing.Section))
    EmberPanel(modifier = Modifier.fillMaxWidth()) {
        SectionHeading("Mod profiles", "${snapshot.profiles.size} ready")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            snapshot.profiles.forEach { profile ->
                val selected = profile.profile.profileId == selectedProfileId
                ControllerButton(
                    id = "profile-${profile.profile.profileId}",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = { onSelectProfile(profile.profile.profileId) },
                    enabled = !busy,
                    selected = selected,
                    modifier = Modifier.widthIn(min = 160.dp, max = 280.dp),
                    role = ControllerButtonRole.QUIET,
                ) {
                    Column {
                        if (selected) {
                            Text("SELECTED", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            profile.profile.name,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            when (profile.orderedEntries.size) {
                                0 -> "Base game"
                                1 -> "1 addition"
                                else -> "${profile.orderedEntries.size} additions"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            ControllerButton(
                id = "add-profile",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = { onRoute(LauncherRoute.ProfileEditor(null)) },
                enabled = !busy,
                role = ControllerButtonRole.SECONDARY,
            ) {
                Text("Add mod set")
            }
            selectedProfileId?.let { profileId ->
                ControllerButton(
                    id = "edit-profile",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = { onRoute(LauncherRoute.ProfileEditor(profileId)) },
                    enabled = !busy,
                    role = ControllerButtonRole.QUIET,
                ) {
                    Text("Edit selected")
                }
            }
        }
    }
}

@Composable
private fun UtilityActions(
    selectedProfileId: String?,
    busy: Boolean,
    focusedId: String?,
    onFocused: (String) -> Unit,
    onImport: () -> Unit,
    onRoute: (LauncherRoute) -> Unit,
) {
    EmberPanel(modifier = Modifier.fillMaxWidth()) {
        SectionHeading("Manage", "Secondary actions")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ControllerButton(
                id = "import",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onImport,
                enabled = !busy,
                role = ControllerButtonRole.SECONDARY,
            ) { Text("Import game or mod") }
            ControllerButton(
                id = "library",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = { onRoute(LauncherRoute.Library) },
                enabled = !busy,
                role = ControllerButtonRole.QUIET,
            ) { Text("Library") }
            selectedProfileId?.let { profileId ->
                ControllerButton(
                    id = "advanced",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = { onRoute(LauncherRoute.Advanced(profileId)) },
                    enabled = !busy,
                    role = ControllerButtonRole.QUIET,
                ) { Text("Advanced") }
            }
            ControllerButton(
                id = "notices",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = { onRoute(LauncherRoute.Notices) },
                enabled = !busy,
                role = ControllerButtonRole.QUIET,
            ) { Text("Notices") }
        }
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
        RouteHeading(
            eyebrow = "About Cinderhell",
            title = "Open-source notices",
            detail = "Licences and attribution for the software and game data in this build.",
        )
        EmberPanel(modifier = Modifier.fillMaxWidth()) {
            Text(noticesText, style = MaterialTheme.typography.bodySmall)
        }
        FooterActions {
            ControllerButton(
                id = "notices-back",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onBack,
                role = ControllerButtonRole.PRIMARY,
            ) { Text("Back") }
        }
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
        RouteHeading(
            eyebrow = "Mod profile",
            title = if (existing == null) {
                "Create a loadout"
            } else {
                "Edit ${existing.profile.name}"
            },
            detail = "Choose one game, a curated preset, and an exact mod load order.",
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(80) },
            label = { Text("Profile name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SectionHeading("Game", "Required")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            snapshot.games.forEach { game ->
                ControllerButton(
                    id = "editor-game-${game.contentId}",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = { gameId = game.contentId },
                    enabled = !busy,
                    selected = gameId == game.contentId,
                    role = ControllerButtonRole.QUIET,
                ) {
                    Text(game.displayName)
                }
            }
        }
        if (existing == null) {
            SectionHeading("Preset", "Curated defaults")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfilePresets.all.forEach { preset ->
                    ControllerButton(
                        id = "editor-preset-${preset.id.wireValue}",
                        focusedId = focusedId,
                        onFocused = onFocused,
                        onClick = { presetId = preset.id },
                        enabled = !busy,
                        selected = presetId == preset.id,
                        role = ControllerButtonRole.QUIET,
                    ) {
                        Text(preset.displayName)
                    }
                }
            }
        } else {
            Text(
                "Preset: ${ProfilePresets.require(existing.profile.presetId, existing.profile.presetVersion).displayName}. Change it from Advanced.",
            )
        }
        SectionHeading("Load order", "${entries.size} additions")
        if (entries.isEmpty()) {
            EmptyState(
                title = "Base game only",
                detail = "No mods or patches are attached. The selected game loads by itself.",
            )
        }
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
                    role = ControllerButtonRole.DANGER,
                ) { Text("Remove") }
            }
        }
        if (snapshot.additions.any { candidate ->
                entries.none { it.contentId == candidate.contentId }
            }
        ) {
            SectionHeading("Available additions", "Add to load order")
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
        FooterActions {
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
                role = ControllerButtonRole.PRIMARY,
            ) { Text("Save profile") }
            ControllerButton(
                id = "profile-back",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onBack,
                enabled = !busy,
                role = ControllerButtonRole.QUIET,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RouteHeading(
                eyebrow = "Advanced",
                title = "Profile unavailable",
            )
            EmptyState(
                title = "This profile was removed",
                detail = "Return home and select another profile before changing presets.",
            )
            ControllerButton(
                id = "advanced-back",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onBack,
                role = ControllerButtonRole.PRIMARY,
            ) { Text("Back") }
        }
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
        RouteHeading(
            eyebrow = "Advanced",
            title = profile.profile.name,
            detail = "Reapply Cinderhell's curated presentation and controller defaults.",
        )
        EmberPanel(modifier = Modifier.fillMaxWidth(), highlighted = true) {
            SectionHeading("Presentation preset", "${current.displayName} selected")
            Text(
                "Only curated video, audio, and controller values change. Other in-game settings remain untouched.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProfilePresets.all.forEach { preset ->
                ControllerButton(
                    id = "apply-${preset.id.wireValue}",
                    focusedId = focusedId,
                    onFocused = onFocused,
                    onClick = { onRequestPreset(profile.profile.profileId, preset.id) },
                    enabled = !busy,
                    selected = current.id == preset.id,
                    modifier = Modifier.fillMaxWidth(),
                    role = ControllerButtonRole.QUIET,
                ) {
                    Column {
                        Text(preset.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            preset.description,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
        EmberPanel(modifier = Modifier.fillMaxWidth()) {
            SectionHeading("Engine options", "Inside the game")
            Text(
                "Rendering, audio, HUD, controller curves, deadzones, rumble, gyro, and input bindings remain available from Options inside the game.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FooterActions {
            ControllerButton(
                id = "advanced-back",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onBack,
                enabled = !busy,
                role = ControllerButtonRole.PRIMARY,
            ) { Text("Back") }
        }
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RouteHeading(
            eyebrow = "Content",
            title = "Library",
            detail = "App-owned games, mods, archives, and patches available to profiles.",
        )
        if (snapshot.content.isEmpty()) {
            EmptyState(
                title = "Nothing installed",
                detail = "Import a supported Doom game or mod to begin.",
            )
        } else {
            EmberPanel(modifier = Modifier.fillMaxWidth()) {
                snapshot.content.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                item.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                MetadataPill(contentTypeLabel(item.contentType.name))
                                MetadataPill(formatContentSize(item.byteSize))
                                if (item.bundled) {
                                    MetadataPill("Included", accent = true)
                                }
                            }
                        }
                        if (!item.bundled) {
                            ControllerButton(
                                id = "remove-${item.contentId}",
                                focusedId = focusedId,
                                onFocused = onFocused,
                                onClick = { onRequestRemoval(item.contentId) },
                                enabled = !busy,
                                role = ControllerButtonRole.DANGER,
                            ) { Text("Remove") }
                        }
                    }
                    if (index != snapshot.content.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 5.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                        )
                    }
                }
            }
        }
        FooterActions {
            ControllerButton(
                id = "library-import",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onImport,
                enabled = !busy,
                role = ControllerButtonRole.PRIMARY,
            ) { Text("Import game or mod") }
            ControllerButton(
                id = "library-back",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onBack,
                enabled = !busy,
                role = ControllerButtonRole.QUIET,
            ) { Text("Back") }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    body: String,
    confirmId: String,
    destructive: Boolean = false,
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
                role = if (destructive) {
                    ControllerButtonRole.DANGER
                } else {
                    ControllerButtonRole.PRIMARY
                },
            ) { Text("Confirm") }
        },
        dismissButton = {
            ControllerButton(
                id = "$confirmId-cancel",
                focusedId = focusedId,
                onFocused = onFocused,
                onClick = onDismiss,
                role = ControllerButtonRole.QUIET,
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
