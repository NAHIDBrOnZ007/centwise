package com.centwise.features.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.core.profile.UserPrefs
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import kotlinx.coroutines.launch

object OnboardingPrefs {
    private const val PREFS_NAME = "centwise_settings"

    fun isCompleted(context: android.content.Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean("onboardingCompleted", false)

    fun setCompleted(context: android.content.Context) {
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("onboardingCompleted", true).apply()
    }
}

private data class OnboardingStep(
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

    var selectedAvatar by remember { mutableStateOf(UserPrefs.getUserAvatar(context)) }
    var userNameInput by remember { mutableStateOf(UserPrefs.getUserName(context).let { if (it == "User") "" else it }) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val infoSteps = listOf(
        OnboardingStep(
            icon = Icons.Default.Sms,
            tint = CentwiseColors.BKashPink,
            title = "Track from SMS",
            description = "Centwise reads bank and MFS messages from bKash, Nagad, Rocket, and Bangladeshi banks, then creates transactions automatically."
        ),
        OnboardingStep(
            icon = Icons.Default.Security,
            tint = CentwiseColors.IncomeGreen,
            title = "Private by design",
            description = "Everything stays on your phone. No account needed, no cloud, no upload. Your money data is yours alone."
        ),
        OnboardingStep(
            icon = Icons.Default.NotificationsActive,
            tint = CentwiseColors.AccentBlue,
            title = "Budgets & insights",
            description = "Set category budgets, watch spending trends, and get warned before you go over. Both Bangla and English supported."
        )
    )

    fun finish() {
        val finalName = if (userNameInput.trim().isEmpty()) "User" else userNameInput.trim()
        UserPrefs.setUserName(context, finalName)
        UserPrefs.setUserAvatar(context, selectedAvatar)
        OnboardingPrefs.setCompleted(context)
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Top Centwise Header & Skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Centwise",
                style = CentwiseTypography.Headline,
                color = accent
            )

            TextButton(onClick = { finish() }) {
                Text("Skip", color = textSecondary)
            }
        }

        Spacer(modifier = Modifier.weight(0.2f))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1.8f)
        ) { page ->
            if (page == 0) {
                // Page 0: Profile & Avatar Setup
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome to Centwise",
                        style = CentwiseTypography.Title1,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Choose your avatar and enter your name",
                        style = CentwiseTypography.Subheadline,
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selected Avatar Preview
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.15f))
                            .border(3.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = UserPrefs.getAvatarResId(selectedAvatar)),
                            contentDescription = "Selected Avatar",
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = userNameInput,
                        onValueChange = { userNameInput = it },
                        placeholder = { Text("Your Name (e.g. Faysal)", color = textSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = textSecondary.copy(alpha = 0.3f),
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(52.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Select Avatar",
                        style = CentwiseTypography.Caption,
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 10 Avatar Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(50.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(UserPrefs.AVAILABLE_AVATARS) { avatarName ->
                            val isSelected = selectedAvatar == avatarName
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                                    .then(
                                        if (isSelected) Modifier.border(2.5.dp, accent, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedAvatar = avatarName },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = UserPrefs.getAvatarResId(avatarName)),
                                    contentDescription = avatarName,
                                    modifier = Modifier.size(38.dp),
                                    contentScale = ContentScale.Fit
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val step = infoSteps[page - 1]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(step.tint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = step.tint,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = step.title,
                        style = CentwiseTypography.Title1,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = step.description,
                        style = CentwiseTypography.Subheadline,
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.2f))

        // Page indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) accent else textSecondary.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val isLastPage = pagerState.currentPage == 3
        Button(
            onClick = {
                if (isLastPage) {
                    finish()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = if (isLastPage) "Get Started" else "Continue",
                style = CentwiseTypography.Headline
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen(onFinished = {})
}
