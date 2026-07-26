package dev.cinderhell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.cinderhell.session.BundledSessionCoordinator

/**
 * Debug-build-only bridge for reproducible physical-device compatibility gates.
 *
 * Session validation remains in [GameActivity]; this receiver only accepts a
 * syntactically valid private descriptor ID and forwards it inside the app.
 */
class DeviceGateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val sessionId = intent.getStringExtra(BundledSessionCoordinator.SESSION_ID_EXTRA)
        if (sessionId == null || !SESSION_ID.matches(sessionId)) {
            Log.w(TAG, "Rejected malformed debug session ID")
            return
        }
        context.startActivity(
            Intent(context, GameActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(BundledSessionCoordinator.SESSION_ID_EXTRA, sessionId),
        )
    }

    private companion object {
        const val ACTION = "dev.cinderhell.DEBUG_LAUNCH_SESSION"
        const val TAG = "CinderhellDeviceGate"
        val SESSION_ID = Regex("[0-9a-f]{32}")
    }
}
