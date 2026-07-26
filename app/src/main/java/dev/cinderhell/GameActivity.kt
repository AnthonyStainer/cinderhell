package dev.cinderhell

import android.os.Bundle
import android.os.Build
import android.os.Process
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import dev.cinderhell.input.ControllerDeviceMonitor
import dev.cinderhell.session.AppPaths
import dev.cinderhell.session.BundledSessionCoordinator
import dev.cinderhell.session.GameArgumentsBuilder
import dev.cinderhell.session.GameSessionDescriptor
import dev.cinderhell.session.GameSessionResult
import dev.cinderhell.session.SessionFailureCode
import dev.cinderhell.session.SessionOutcome
import dev.cinderhell.session.SessionStore
import dev.cinderhell.session.SessionValidationException
import dev.cinderhell.session.RecentStateStore
import org.libsdl.app.SDLActivity

class GameActivity : SDLActivity() {
    private lateinit var sessionStore: SessionStore
    private var requestedSessionId = ""
    private var descriptor: GameSessionDescriptor? = null
    private var nativeArguments: Array<String>? = null
    private lateinit var audioFocus: GameAudioFocusController
    private lateinit var controllerMonitor: ControllerDeviceMonitor
    private var controllerWasConnected = false

    @Volatile
    private var activityResumed = false

    @Volatile
    private var destroyRequested = false

    @Volatile
    private var resultWritten = false

    @Volatile
    private var terminateAfterDestroy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        sessionStore = SessionStore(AppPaths(filesDir))
        requestedSessionId =
            intent?.getStringExtra(BundledSessionCoordinator.SESSION_ID_EXTRA).orEmpty()
        super.onCreate(savedInstanceState)
        GamePresentationController.configureImmersive(this)
        audioFocus = GameAudioFocusController(
            context = this,
            onFocusLost = {
                if (activityResumed) pauseNativeThread()
            },
            onFocusGained = {
                if (activityResumed) resumeNativeThread()
            },
        )
        controllerMonitor = ControllerDeviceMonitor(this) { state ->
            runOnUiThread {
                if (controllerWasConnected && !state.connected) {
                    Toast.makeText(
                        this,
                        "Controller disconnected. Reconnect it; Android Back still opens the menu.",
                        Toast.LENGTH_LONG,
                    ).show()
                } else if (!controllerWasConnected && state.connected) {
                    Log.i(
                        TAG,
                        "Controller connected: ${state.name}; rumble=${state.rumbleSupported}",
                    )
                }
                controllerWasConnected = state.connected
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                ::sendBackToGame,
            )
        }
    }

    override fun main() {
        try {
            val current = sessionStore.consumePending(requestedSessionId)
            descriptor = current
            RuntimeAssetInstaller.ensureInstalled(this)
            nativeArguments = GameArgumentsBuilder.build(current)
            runOnUiThread {
                GamePresentationController.configure(
                    this,
                    current.options.targetRefreshRate,
                )
            }
            Log.v(TAG, "Running validated SDL_main session ${current.sessionId}")
            val exitCode = nativeRunMain(
                getMainSharedObject(),
                getMainFunction(),
                getArguments(),
            )
            Log.v(TAG, "SDL_main returned $exitCode")
            if (!destroyRequested && exitCode == 0) {
                RecentStateStore(AppPaths(filesDir)).capture(current)
            }

            sessionStore.recordResult(
                GameSessionResult(
                    sessionId = current.sessionId,
                    nonce = current.nonce,
                    profileId = current.profileId,
                    completedAtEpochMillis = System.currentTimeMillis(),
                    outcome = if (destroyRequested) {
                        SessionOutcome.INTERRUPTED
                    } else if (exitCode == 0) {
                        SessionOutcome.CLEAN_EXIT
                    } else {
                        SessionOutcome.STARTUP_FAILURE
                    },
                    failureCode = if (destroyRequested) {
                        SessionFailureCode.PROCESS_LOST
                    } else if (exitCode != 0) {
                        SessionFailureCode.NATIVE_STARTUP_FAILURE
                    } else {
                        null
                    },
                    userMessage = if (destroyRequested) {
                        "The game was interrupted. Your saves are still available."
                    } else if (exitCode != 0) {
                        "The game stopped during startup. Your content and saves were not changed."
                    } else {
                        null
                    },
                ),
            )
            resultWritten = true
        } catch (error: SessionValidationException) {
            writeStartupFailure(error.code, error.message.orEmpty())
            finishAfterStartupFailure()
        } catch (error: Exception) {
            Log.e(TAG, "Game startup failed", error)
            writeStartupFailure(
                SessionFailureCode.NATIVE_STARTUP_FAILURE,
                "The game could not start. Your content and saves were not changed.",
            )
            finishAfterStartupFailure()
        } finally {
            terminateAfterDestroy = true
        }
    }

    override fun getArguments(): Array<String> {
        return checkNotNull(nativeArguments) { "Validated game arguments are unavailable" }
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        if (::audioFocus.isInitialized && !audioFocus.request()) {
            pauseNativeThread()
        }
        GamePresentationController.configureImmersive(this)
    }

    override fun onStart() {
        super.onStart()
        if (::controllerMonitor.isInitialized) controllerMonitor.start()
    }

    override fun onStop() {
        if (::controllerMonitor.isInitialized) controllerMonitor.stop()
        super.onStop()
    }

    override fun onPause() {
        activityResumed = false
        if (::audioFocus.isInitialized) audioFocus.abandon()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) GamePresentationController.configureImmersive(this)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && nativeArguments != null) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    Log.v(TAG, "Translating Android Back down to Doom menu input")
                    onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE)
                }

                KeyEvent.ACTION_UP -> onNativeKeyUp(KeyEvent.KEYCODE_ESCAPE)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Android routes predictive back through OnBackInvokedDispatcher")
    override fun onBackPressed() {
        sendBackToGame()
    }

    override fun onDestroy() {
        destroyRequested = true
        super.onDestroy()
        if (terminateAfterDestroy) {
            Process.killProcess(Process.myPid())
        }
    }

    private fun writeStartupFailure(
        code: SessionFailureCode,
        message: String,
    ) {
        val current = descriptor
        runCatching {
            sessionStore.recordStartupFailure(
                sessionId = current?.sessionId ?: requestedSessionId,
                nonce = current?.nonce.orEmpty(),
                profileId = current?.profileId,
                code = code,
                userMessage = message.ifBlank {
                    "The game could not start. Please try again."
                },
            )
            resultWritten = true
        }.onFailure {
            Log.e(TAG, "Could not write game startup result", it)
        }
    }

    private fun finishAfterStartupFailure() {
        runOnUiThread {
            if (!isFinishing) finish()
        }
    }

    private fun sendBackToGame() {
        if (nativeArguments == null) {
            finish()
            return
        }
        Log.v(TAG, "Translating Android Back gesture to Doom menu input")
        onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE)
        onNativeKeyUp(KeyEvent.KEYCODE_ESCAPE)
    }

    private companion object {
        const val TAG = "CinderhellGame"
    }
}
