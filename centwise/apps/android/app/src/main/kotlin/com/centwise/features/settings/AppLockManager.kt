package com.centwise.features.settings

import android.app.KeyguardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppLockManager {
    private const val PREFS_NAME = "centwise_settings"

    var isLockEnabled: Boolean = false
        private set

    var isLocked: Boolean by mutableStateOf(false)
        private set

    private var lastBackgroundTimeMillis: Long = 0L

    fun load(context: Context) {
        isLockEnabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("appLockEnabled", false)
    }

    fun setLockEnabled(context: Context, enabled: Boolean) {
        isLockEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("appLockEnabled", enabled).apply()
    }

    /** True when the device has a secure lock screen (PIN, pattern, or biometric). */
    fun canLock(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isDeviceSecure == true
    }

    fun lockTypeLabel(context: Context): String =
        if (canLock(context)) "Screen lock or biometrics" else "Requires a screen lock"

    fun lockNow() {
        if (isLockEnabled) {
            isLocked = true
        }
    }

    fun unlock() {
        isLocked = false
    }

    fun onAppBackgrounded() {
        if (isLockEnabled) {
            lastBackgroundTimeMillis = System.currentTimeMillis()
        }
    }

    fun onAppForegrounded() {
        if (!isLockEnabled) return
        val elapsed = System.currentTimeMillis() - lastBackgroundTimeMillis
        // Lock immediately after 60+ seconds in background (timeout support comes with settings)
        if (lastBackgroundTimeMillis > 0 && elapsed >= 60_000L) {
            isLocked = true
        }
    }
}
