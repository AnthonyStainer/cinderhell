package dev.cinderhell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Debug-build-only probe used by the physical-device lifecycle gate.
 */
class AudioFocusProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val pendingResult = goAsync()
        val audioManager = context.getSystemService(AudioManager::class.java)
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setOnAudioFocusChangeListener({})
            .build()
        val granted =
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        Log.i(TAG, "Transient focus probe granted: $granted")
        Handler(Looper.getMainLooper()).postDelayed(
            {
                if (granted) audioManager.abandonAudioFocusRequest(request)
                Log.i(TAG, "Transient focus probe released")
                pendingResult.finish()
            },
            HOLD_MILLIS,
        )
    }

    private companion object {
        const val ACTION = "dev.cinderhell.TEST_AUDIO_FOCUS"
        const val TAG = "CinderhellFocusProbe"
        const val HOLD_MILLIS = 2_000L
    }
}
