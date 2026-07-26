package dev.cinderhell.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

internal data class ControllerDeviceState(
    val connected: Boolean,
    val name: String? = null,
    val descriptor: String? = null,
    val rumbleSupported: Boolean = false,
)

internal class ControllerDeviceMonitor(
    context: Context,
    private val onChanged: (ControllerDeviceState) -> Unit,
) : InputManager.InputDeviceListener {
    private val inputManager =
        context.getSystemService(Context.INPUT_SERVICE) as InputManager

    fun start() {
        inputManager.registerInputDeviceListener(this, null)
        publish()
    }

    fun stop() {
        inputManager.unregisterInputDeviceListener(this)
    }

    override fun onInputDeviceAdded(deviceId: Int) = publish()

    override fun onInputDeviceRemoved(deviceId: Int) = publish()

    override fun onInputDeviceChanged(deviceId: Int) = publish()

    private fun publish() {
        val controller = InputDevice.getDeviceIds()
            .asSequence()
            .mapNotNull(InputDevice::getDevice)
            .firstOrNull(::isController)
        @Suppress("DEPRECATION")
        onChanged(
            ControllerDeviceState(
                connected = controller != null,
                name = controller?.name,
                descriptor = controller?.descriptor,
                rumbleSupported = controller?.vibrator?.hasVibrator() == true,
            ),
        )
    }

    companion object {
        fun isController(device: InputDevice): Boolean {
            val sources = device.sources
            return sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        }
    }
}

internal class StickNavigationTranslator(
    private val threshold: Float = 0.65f,
    private val repeatDelayMillis: Long = 180,
    private val clock: () -> Long = SystemClock::uptimeMillis,
) {
    private var lastDirection: Int? = null
    private var lastDispatchAt = 0L

    fun translate(event: MotionEvent): Int? {
        if (event.action != MotionEvent.ACTION_MOVE ||
            event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK
        ) {
            return null
        }
        val horizontal = strongest(
            event.getAxisValue(MotionEvent.AXIS_HAT_X),
            event.getAxisValue(MotionEvent.AXIS_X),
        )
        val vertical = strongest(
            event.getAxisValue(MotionEvent.AXIS_HAT_Y),
            event.getAxisValue(MotionEvent.AXIS_Y),
        )
        return translateAxes(horizontal, vertical)
    }

    fun translateAxes(horizontal: Float, vertical: Float): Int? {
        val direction = when {
            kotlin.math.abs(horizontal) >= kotlin.math.abs(vertical) &&
                horizontal <= -threshold -> KeyEvent.KEYCODE_DPAD_LEFT
            kotlin.math.abs(horizontal) >= kotlin.math.abs(vertical) &&
                horizontal >= threshold -> KeyEvent.KEYCODE_DPAD_RIGHT
            vertical <= -threshold -> KeyEvent.KEYCODE_DPAD_UP
            vertical >= threshold -> KeyEvent.KEYCODE_DPAD_DOWN
            else -> null
        }
        val now = clock()
        if (direction == null) {
            lastDirection = null
            return null
        }
        if (direction == lastDirection && now - lastDispatchAt < repeatDelayMillis) {
            return null
        }
        lastDirection = direction
        lastDispatchAt = now
        return direction
    }

    private fun strongest(first: Float, second: Float): Float =
        if (kotlin.math.abs(first) >= kotlin.math.abs(second)) first else second
}
