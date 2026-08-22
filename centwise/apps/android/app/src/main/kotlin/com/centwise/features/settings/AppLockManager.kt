package com.centwise.features.settings

import android.app.KeyguardManager
import android.content.Context

object AppLockManager {
    private const val PREFS_NAME = "centwise_settings"

    var isLockEnabled: Boolean = false
        private set

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
}
