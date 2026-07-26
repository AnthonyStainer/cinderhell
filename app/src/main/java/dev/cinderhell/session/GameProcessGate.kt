package dev.cinderhell.session

import android.app.ActivityManager
import android.content.Context

internal object GameProcessGate {
    fun isRunning(context: Context): Boolean {
        val manager = context.getSystemService(ActivityManager::class.java)
        val processName = "${context.packageName}:game"
        return manager.runningAppProcesses.orEmpty().any {
            it.processName == processName
        }
    }
}
