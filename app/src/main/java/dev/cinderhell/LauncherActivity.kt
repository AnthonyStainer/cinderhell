package dev.cinderhell

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import dev.cinderhell.input.ControllerDeviceMonitor
import dev.cinderhell.input.ControllerDeviceState
import dev.cinderhell.input.StickNavigationTranslator
import dev.cinderhell.launcher.LauncherService
import dev.cinderhell.launcher.LauncherSnapshot
import dev.cinderhell.library.ContentImportResult
import dev.cinderhell.library.ContentImporter
import dev.cinderhell.library.ContentRemovalPlan
import dev.cinderhell.library.ContentType
import dev.cinderhell.profile.PresetReapplyPreview
import dev.cinderhell.session.BundledSessionCoordinator
import dev.cinderhell.session.GameSessionDescriptor
import dev.cinderhell.session.SessionOutcome
import dev.cinderhell.ui.LauncherNotice
import dev.cinderhell.ui.LauncherRoute
import dev.cinderhell.ui.LauncherScreen
import dev.cinderhell.ui.ProfileSaveRequest
import dev.cinderhell.ui.theme.CinderhellTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherActivity : ComponentActivity() {
    private lateinit var sessions: BundledSessionCoordinator
    private lateinit var importer: ContentImporter
    private lateinit var launcher: LauncherService
    private lateinit var controllerMonitor: ControllerDeviceMonitor
    private val stickNavigation = StickNavigationTranslator()
    private val noticesText by lazy {
        assets.open("legal/THIRD_PARTY_NOTICES.txt").bufferedReader().use { it.readText() }
    }

    private var snapshot by androidx.compose.runtime.mutableStateOf<LauncherSnapshot?>(null)
    private var route by androidx.compose.runtime.mutableStateOf<LauncherRoute>(LauncherRoute.Home)
    private var busy by androidx.compose.runtime.mutableStateOf(true)
    private var notice by androidx.compose.runtime.mutableStateOf<LauncherNotice?>(null)
    private var focusedId by androidx.compose.runtime.mutableStateOf<String?>("play")
    private var controller by androidx.compose.runtime.mutableStateOf(ControllerDeviceState(false))
    private var removalPlan by androidx.compose.runtime.mutableStateOf<ContentRemovalPlan?>(null)
    private var presetPreview by androidx.compose.runtime.mutableStateOf<PresetReapplyPreview?>(null)
    private val documentPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(::importDocument)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessions = BundledSessionCoordinator(this)
        importer = ContentImporter(this)
        launcher = LauncherService(this)
        controllerMonitor = ControllerDeviceMonitor(this) { state ->
            runOnUiThread {
                val wasConnected = controller.connected
                controller = state
                if (wasConnected && !state.connected) {
                    notice = LauncherNotice.warning(
                        "Controller disconnected. Reconnect it or use Android Back to stay safe.",
                    )
                } else if (!wasConnected && state.connected && snapshot != null) {
                    notice = LauncherNotice.success(
                        "${state.name ?: "Controller"} connected.",
                    )
                }
            }
        }
        setContent {
            CinderhellTheme {
                LauncherScreen(
                    snapshot = snapshot,
                    route = route,
                    busy = busy,
                    statusNotice = notice,
                    controller = controller,
                    focusedId = focusedId,
                    removalPlan = removalPlan,
                    presetPreview = presetPreview,
                    noticesText = noticesText,
                    onFocused = { focusedId = it },
                    onPlay = { startGame(continueRecent = false) },
                    onContinue = { startGame(continueRecent = true) },
                    onImport = { documentPicker.launch(arrayOf("*/*")) },
                    onSelectGame = ::selectGame,
                    onSelectProfile = ::selectProfile,
                    onRoute = ::navigate,
                    onSaveProfile = ::saveProfile,
                    onRequestRemoval = ::requestRemoval,
                    onConfirmRemoval = ::confirmRemoval,
                    onDismissRemoval = { removalPlan = null },
                    onRequestPreset = ::requestPreset,
                    onConfirmPreset = ::confirmPreset,
                    onDismissPreset = { presetPreview = null },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::controllerMonitor.isInitialized) controllerMonitor.start()
    }

    override fun onStop() {
        if (::controllerMonitor.isInitialized) controllerMonitor.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!::sessions.isInitialized) return
        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                sessions.recoverAndConsumeResults()
            }
            results.lastOrNull()?.let { result ->
                notice = when (result.outcome) {
                    SessionOutcome.CLEAN_EXIT ->
                        LauncherNotice.success("Game closed safely.")

                    SessionOutcome.STARTUP_FAILURE,
                    SessionOutcome.INTERRUPTED,
                    -> LauncherNotice.error(
                        result.userMessage ?: "The game stopped unexpectedly.",
                    )
                }
            }
            refresh(showBusy = snapshot == null)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val keyCode = stickNavigation.translate(event)
            ?: return super.dispatchGenericMotionEvent(event)
        val down = KeyEvent(event.eventTime, event.eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up = KeyEvent(event.eventTime, event.eventTime, KeyEvent.ACTION_UP, keyCode, 0)
        return super.dispatchKeyEvent(down) || super.dispatchKeyEvent(up)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val controllerSource =
            event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (controllerSource) {
            val mapped = when (event.keyCode) {
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_BUTTON_START,
                -> KeyEvent.KEYCODE_ENTER

                KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BACK
                else -> null
            }
            if (mapped != null) {
                return super.dispatchKeyEvent(
                    KeyEvent(
                        event.downTime,
                        event.eventTime,
                        event.action,
                        mapped,
                        event.repeatCount,
                        event.metaState,
                        event.deviceId,
                        event.scanCode,
                        event.flags,
                        event.source,
                    ),
                )
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun refresh(showBusy: Boolean) {
        lifecycleScope.launch {
            if (showBusy) busy = true
            try {
                snapshot = withContext(Dispatchers.IO) { launcher.load() }
            } catch (error: Exception) {
                notice = LauncherNotice.error(
                    error.message ?: "The game library could not be prepared.",
                )
            } finally {
                if (showBusy) busy = false
            }
        }
    }

    private fun startGame(continueRecent: Boolean) {
        if (busy) return
        lifecycleScope.launch {
            busy = true
            notice = null
            var descriptor: GameSessionDescriptor? = null
            try {
                descriptor = withContext(Dispatchers.IO) {
                    if (continueRecent) {
                        sessions.createContinueSession()
                    } else {
                        sessions.createSelectedSession()
                    }
                }
                startActivity(
                    Intent(this@LauncherActivity, GameActivity::class.java)
                        .putExtra(
                            BundledSessionCoordinator.SESSION_ID_EXTRA,
                            descriptor.sessionId,
                        ),
                )
            } catch (error: Exception) {
                descriptor?.let {
                    withContext(Dispatchers.IO) {
                        sessions.cancelPendingSession(
                            it,
                            "The game could not be opened. Please try again.",
                        )
                    }
                }
                notice = LauncherNotice.error(
                    error.message ?: "The game could not be opened.",
                )
            } finally {
                busy = false
            }
        }
    }

    private fun selectGame(contentId: String) = perform {
        launcher.selectGame(contentId)
        "Game selected."
    }

    private fun selectProfile(profileId: String) = perform {
        launcher.selectProfile(profileId)
        "Profile selected."
    }

    private fun saveProfile(request: ProfileSaveRequest) = perform {
        launcher.saveProfile(
            profileId = request.profileId,
            name = request.name,
            gameContentId = request.gameContentId,
            presetId = request.presetId,
            orderedContentIds = request.orderedContentIds,
        )
        route = LauncherRoute.Home
        focusedId = "play"
        "Profile saved."
    }

    private fun requestRemoval(contentId: String) {
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { launcher.prepareRemoval(contentId) }
            }.onSuccess {
                removalPlan = it
                focusedId = "confirm-removal"
            }.onFailure {
                notice = LauncherNotice.error(
                    it.message ?: "That item could not be removed.",
                )
            }
        }
    }

    private fun confirmRemoval() {
        val plan = removalPlan ?: return
        removalPlan = null
        perform {
            launcher.confirmRemoval(plan)
            "Content removed safely."
        }
    }

    private fun requestPreset(profileId: String, presetId: dev.cinderhell.profile.PresetId) {
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    launcher.previewPresetChange(profileId, presetId)
                }
            }.onSuccess {
                presetPreview = it
                focusedId = "confirm-preset"
            }.onFailure {
                notice = LauncherNotice.error(
                    it.message ?: "The preset could not be previewed.",
                )
            }
        }
    }

    private fun confirmPreset() {
        val preview = presetPreview ?: return
        presetPreview = null
        perform {
            launcher.applyPresetChange(preview)
            "The ${preview.preset.displayName} preset was applied."
        }
    }

    private fun importDocument(uri: Uri) {
        if (busy) return
        lifecycleScope.launch {
            busy = true
            notice = LauncherNotice.info("Importing selected document…")
            try {
                val result = importer.import(uri)
                val item = when (result) {
                    is ContentImportResult.Imported -> result.item
                    is ContentImportResult.Duplicate -> result.existing
                }
                if (item.contentType == ContentType.GAME_WAD) {
                    withContext(Dispatchers.IO) { launcher.selectGame(item.contentId) }
                }
                notice = when (result) {
                    is ContentImportResult.Imported ->
                        LauncherNotice.success("${result.item.displayName} was added.")

                    is ContentImportResult.Duplicate ->
                        LauncherNotice.info(
                            "${result.existing.displayName} was already imported.",
                        )
                }
                snapshot = withContext(Dispatchers.IO) { launcher.load() }
            } catch (error: Exception) {
                notice = LauncherNotice.error(
                    error.message ?: "The selected document could not be imported.",
                )
            } finally {
                busy = false
            }
        }
    }

    private fun perform(action: suspend () -> String) {
        if (busy) return
        lifecycleScope.launch {
            busy = true
            notice = null
            try {
                notice = LauncherNotice.success(
                    withContext(Dispatchers.IO) { action() },
                )
                snapshot = withContext(Dispatchers.IO) { launcher.load() }
            } catch (error: Exception) {
                notice = LauncherNotice.error(
                    error.message ?: "That change could not be saved.",
                )
            } finally {
                busy = false
            }
        }
    }

    private fun navigate(destination: LauncherRoute) {
        route = destination
        focusedId = when (destination) {
            LauncherRoute.Home -> "play"
            LauncherRoute.Library -> "library-import"
            LauncherRoute.Notices -> "notices-back"
            is LauncherRoute.ProfileEditor -> "save-profile"
            is LauncherRoute.Advanced -> "apply-handheld"
        }
    }
}
