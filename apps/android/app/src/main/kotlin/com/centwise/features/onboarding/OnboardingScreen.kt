package com.centwise.features.onboarding

import com.centwise.MainActivity
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.centwise.core.scanner.HistoricalSmsScanner
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            icon = Icons.Default.Security,
            tint = CentwiseColors.IncomeGreen,
            title = "Private by design",
            description = "Everything stays on your phone. No account needed, no cloud, no upload. Your money data is yours alone."
        ),
        OnboardingStep(
            icon = Icons.Default.NotificationsActive,
            tint = accent,
            title = "Budgets & insights",
            description = "Set category budgets, watch spending trends, and get warned before you go over. Both Bangla and English supported."
        )
    )

    var showPermissionStep by remember { mutableStateOf(false) }

    fun finish() {
        val finalName = if (userNameInput.trim().isEmpty()) "User" else userNameInput.trim()
        UserPrefs.setUserName(context, finalName)
        UserPrefs.setUserAvatar(context, selectedAvatar)
        showPermissionStep = true
    }

    if (showPermissionStep) {
        OnboardingPermissionStep(
            isDark = isDark,
            onFinished = onFinished
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Top Centwise Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Centwise",
                style = CentwiseTypography.Headline,
                color = accent
            )

        }

        Spacer(modifier = Modifier.weight(0.2f))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1.8f)
        ) { page ->
            if (page == 0) {
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
                            .background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = accent, modifier = Modifier.size(44.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Welcome to Centwise", style = CentwiseTypography.Title1, color = textPrimary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Your simple, private money tracker for Bangladesh.", style = CentwiseTypography.Subheadline, color = textSecondary, textAlign = TextAlign.Center)
                }
            } else if (page == 1) {
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

                    // Selected Avatar Preview (iOS 1:1 Matching)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.15f))
                            .border(3.dp, accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = UserPrefs.getAvatarResId(selectedAvatar)),
                            contentDescription = "Selected Avatar",
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    BasicTextField(
                        value = userNameInput,
                        onValueChange = { userNameInput = it },
                        singleLine = true,
                        textStyle = CentwiseTypography.Headline.copy(
                            textAlign = TextAlign.Center,
                            color = textPrimary,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(accent),
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0C000000)),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userNameInput.isEmpty()) {
                                    Text(
                                        text = "Your name",
                                        style = CentwiseTypography.Headline.copy(fontSize = 16.sp),
                                        color = textSecondary.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Pick an Avatar",
                        style = CentwiseTypography.Caption,
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 10 Avatar Grid (Exact 5x2 iOS layout with smooth round circles & unclipped checkmark badge)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(UserPrefs.AVAILABLE_AVATARS) { avatarName ->
                            val isSelected = selectedAvatar == avatarName
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) accent.copy(alpha = 0.15f)
                                            else if (isDark) Color(0x14FFFFFF)
                                            else Color(0x0A000000)
                                        )
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
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .offset(x = 18.dp, y = (-18).dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) CentwiseColors.DarkBackground else Color.White)
                                            .padding(1.5.dp)
                                            .clip(CircleShape)
                                            .background(accent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val step = infoSteps[page - 2]
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
        com.centwise.core.design.components.CentwiseButton(
            title = if (isLastPage) "Get Started" else "Continue",
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            isFullWidth = true,
            onClick = {
                if (isLastPage) {
                    finish()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            isDark = isDark
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private enum class PermissionStage { NOTIFICATIONS, SMS, SCAN }

@Composable
private fun OnboardingPermissionStep(
    isDark: Boolean,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    var stage by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) PermissionStage.NOTIFICATIONS else PermissionStage.SMS
        )
    }

    var isScanning by remember { mutableStateOf(false) }
    fun scanSms() {
        if (isScanning) return
        isScanning = true
        scope.launch(Dispatchers.IO) {
            val result = HistoricalSmsScanner.scanInbox(context.applicationContext)
            withContext(Dispatchers.Main) {
                isScanning = false
                OnboardingPrefs.setCompleted(context)
                onFinished()
            }
        }
    }

    fun handleSmsResult(result: Map<String, Boolean>) {
        val granted = result[Manifest.permission.READ_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        if (granted) stage = PermissionStage.SCAN else {
            OnboardingPrefs.setCompleted(context)
            onFinished()
        }
    }

    fun requestNextPermission() {
        val activity = context as? MainActivity ?: return
        if (stage == PermissionStage.NOTIFICATIONS) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                stage = PermissionStage.SMS
            } else {
                activity.requestOnboardingPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) {
                    stage = PermissionStage.SMS
                }
            }
            return
        }
        if (stage == PermissionStage.SCAN) return
        val missing = buildList {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_SMS)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECEIVE_SMS)
        }
        if (missing.isEmpty()) stage = PermissionStage.SCAN
        else activity.requestOnboardingPermissions(missing.toTypedArray(), ::handleSmsResult)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(bg).systemBarsPadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Centwise", style = CentwiseTypography.Headline, color = accent, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = if (stage == PermissionStage.NOTIFICATIONS) Icons.Default.NotificationsActive else Icons.Default.Sms,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = when (stage) {
                PermissionStage.NOTIFICATIONS -> "Allow notifications"
                PermissionStage.SMS -> "Allow SMS access"
                PermissionStage.SCAN -> if (isScanning) "Scanning your SMS" else "Scan your SMS"
            },
            style = CentwiseTypography.Title1,
            color = textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = when (stage) {
                PermissionStage.NOTIFICATIONS -> "Centwise uses notifications to let you know when a transaction is captured."
                PermissionStage.SMS -> "Centwise reads bank and MFS messages to scan your transactions. Your data stays on this device."
                PermissionStage.SCAN -> "Centwise is scanning your SMS using the shared Rust engine."
            },
            style = CentwiseTypography.Subheadline,
            color = textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        if (isScanning) {
            CircularProgressIndicator(color = accent, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(20.dp))
        }
        com.centwise.core.design.components.CentwiseButton(
            title = when {
                stage != PermissionStage.SCAN -> "Allow"
                isScanning -> "Scanning..."
                else -> "Scan SMS"
            },
            icon = Icons.Default.Sms,
            isFullWidth = true,
            onClick = {
                if (stage == PermissionStage.SCAN) {
                    scanSms()
                } else {
                    requestNextPermission()
                }
            },
            isDark = isDark
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen(onFinished = {})
}
