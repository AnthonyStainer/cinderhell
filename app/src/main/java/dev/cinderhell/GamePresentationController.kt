package dev.cinderhell

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.abs

internal object GamePresentationController {
    private const val TAG = "CinderhellDisplay"

    fun configure(activity: Activity, targetRefreshRate: Int) {
        configureImmersive(activity)

        val display = if (Build.VERSION.SDK_INT >= 30) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return

        val modes = display.supportedModes
        val current = display.mode
        val best = modes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .minByOrNull { abs(it.refreshRate - targetRefreshRate) }
            ?: modes.minByOrNull { abs(it.refreshRate - targetRefreshRate) }
            ?: return

        activity.window.attributes = activity.window.attributes.apply {
            preferredDisplayModeId = best.modeId
            preferredRefreshRate = best.refreshRate
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        Log.i(
            TAG,
            "Requested display mode ${best.modeId} at ${best.refreshRate} Hz for $targetRefreshRate Hz target",
        )
    }

    fun configureImmersive(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
