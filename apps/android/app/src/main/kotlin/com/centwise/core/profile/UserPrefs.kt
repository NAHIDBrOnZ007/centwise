package com.centwise.core.profile

import android.content.Context
import com.centwise.R

object UserPrefs {
    private const val PREFS_NAME = "centwise_user_profile"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_AVATAR = "user_avatar"

    val AVAILABLE_AVATARS = listOf(
        "avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5",
        "avatar_6", "avatar_7", "avatar_8", "avatar_9", "avatar_10"
    )

    fun getUserName(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, "User") ?: "User"

    fun setUserName(context: Context, name: String) {
        val cleanName = if (name.trim().isEmpty()) "User" else name.trim()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER_NAME, cleanName).apply()
    }

    fun getUserAvatar(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_AVATAR, "avatar_1") ?: "avatar_1"

    fun setUserAvatar(context: Context, avatar: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER_AVATAR, avatar).apply()
    }

    fun getAvatarResId(avatarName: String): Int {
        return when (avatarName) {
            "avatar_1" -> R.drawable.avatar_1
            "avatar_2" -> R.drawable.avatar_2
            "avatar_3" -> R.drawable.avatar_3
            "avatar_4" -> R.drawable.avatar_4
            "avatar_5" -> R.drawable.avatar_5
            "avatar_6" -> R.drawable.avatar_6
            "avatar_7" -> R.drawable.avatar_7
            "avatar_8" -> R.drawable.avatar_8
            "avatar_9" -> R.drawable.avatar_9
            "avatar_10" -> R.drawable.avatar_10
            else -> R.drawable.avatar_1
        }
    }

    fun getUserAvatarResId(context: Context): Int {
        return getAvatarResId(getUserAvatar(context))
    }
}
