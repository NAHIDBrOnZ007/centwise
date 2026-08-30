package com.centwise.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.core.profile.UserPrefs
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import java.util.Calendar

@Composable
fun GreetingCard(
    userName: String? = null,
    greeting: String? = null,
    avatarResId: Int? = null,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUserName = userName?.takeIf { it.isNotBlank() } ?: UserPrefs.getUserName(context)
    val currentAvatarResId = avatarResId ?: UserPrefs.getUserAvatarResId(context)
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val displayGreeting = greeting ?: when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .background(cardBg)
            .iosBounceClick { onProfileClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Avatar Circle with Accent Ring & Edit indicator
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f))
                .border(2.dp, accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = currentAvatarResId),
                contentDescription = "User Avatar",
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentUserName,
                style = CentwiseTypography.Headline.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                color = textPrimary,
                maxLines = 1
            )
            Text(
                text = displayGreeting,
                style = CentwiseTypography.Subheadline,
                color = textSecondary
            )
        }

        // Trailing Edit Icon Matching iOS pencil.circle
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit Profile",
            tint = textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingCardPreview() {
    GreetingCard(
        userName = "User",
        greeting = "Good morning"
    )
}
