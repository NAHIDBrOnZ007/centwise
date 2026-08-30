package com.centwise.features.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.ModalSheetTopBar
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.core.profile.UserPrefs
import kotlinx.coroutines.launch

/**
 * Idiomatic Jetpack Compose Edit Profile Sheet matching iOS AvatarPickerView 1:1.
 * Features centered pill input, live avatar preview ring, checkmark avatar grid, and tactile spring dismissals.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    currentName: String,
    currentAvatar: String,
    onDismiss: () -> Unit,
    onSave: (name: String, avatar: String) -> Unit,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var selectedAvatar by remember { mutableStateOf(currentAvatar) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val dismissWithAnimation: () -> Unit = {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // iOS 1:1 Modal Sheet Navigation Bar
            ModalSheetTopBar(
                title = "Edit Profile",
                onCancel = { dismissWithAnimation() },
                onSave = {
                    val finalName = if (nameInput.trim().isEmpty()) "User" else nameInput.trim()
                    onSave(finalName, selectedAvatar)
                    dismissWithAnimation()
                },
                saveLabel = "Done",
                saveEnabled = nameInput.isNotBlank(),
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Selected Avatar Preview Ring
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f))
                    .border(3.dp, accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = UserPrefs.getAvatarResId(selectedAvatar)),
                    contentDescription = "Selected Avatar",
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Centered Pill Name Field (Matching iOS Plain TextField with Rounded Rectangle)
            BasicTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                singleLine = true,
                textStyle = CentwiseTypography.Headline.copy(
                    textAlign = TextAlign.Center,
                    color = textPrimary,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(accent),
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0C000000)),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (nameInput.isEmpty()) {
                            Text(
                                text = "Enter your name",
                                style = CentwiseTypography.Headline.copy(fontSize = 16.sp),
                                color = textSecondary.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Choose Your Avatar",
                style = CentwiseTypography.Subheadline,
                color = textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 10 Avatar Grid (5x2 Layout Matching iOS AvatarPickerView)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(UserPrefs.AVAILABLE_AVATARS) { avatarName ->
                    val isSelected = selectedAvatar == avatarName
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) accent.copy(alpha = 0.15f)
                                    else if (isDark) Color(0x14FFFFFF)
                                    else Color(0x08000000)
                                )
                                .then(if (isSelected) Modifier.border(2.5.dp, accent, CircleShape) else Modifier)
                                .iosBounceClick { selectedAvatar = avatarName },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = UserPrefs.getAvatarResId(avatarName)),
                                contentDescription = avatarName,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // Checkmark Badge on Selected Avatar
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = accent,
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}
