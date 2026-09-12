package com.centwise.features.group

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.ModalSheetTopBar
import com.centwise.core.design.components.clearFocusOnTapOutside
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.settings.LanguagePrefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInviteSheet(
    group: SharedGroup,
    onDismiss: () -> Unit,
    onJoinAnother: (code: String) -> Unit,
    accent: Color = AccentOptions.byName(AppearancePrefs.accentName).color,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val inputBg = if (isDark) Color(0xFF1E2633) else Color(0xFFF0F3F6)
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var showScannerMode by remember { mutableStateOf(false) }
    var inputJoinCode by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    val dismissWithAnimation: (postAction: () -> Unit) -> Unit = { postAction ->
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                postAction()
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bg,
        dragHandle = {
            Box(modifier = Modifier.clearFocusOnTapOutside()) {
                BottomSheetDefaults.DragHandle()
            }
        }
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clearFocusOnTapOutside()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ModalSheetTopBar(
                title = if (showScannerMode) GroupStrings.joinTitle else GroupStrings.inviteTitle,
                onCancel = { dismissWithAnimation {} },
                onSave = { dismissWithAnimation {} },
                saveLabel = GroupStrings.done,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDark = isDark
            )

            // 2. Mode Selector Pill (CentwiseSegmentedControl)
            com.centwise.core.design.components.CentwiseSegmentedControl(
                items = listOf(false, true),
                selectedItem = showScannerMode,
                onItemSelected = {
                    showScannerMode = it
                    if (AppearancePrefs.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                itemLabel = { if (!it) GroupStrings.myGroupQrTab else GroupStrings.scanQrTab },
                itemIcon = { if (!it) Icons.Default.QrCode2 else Icons.Default.QrCodeScanner },
                modifier = Modifier.height(36.dp),
                accent = accent,
                isDark = isDark
            )

            if (!showScannerMode) {
                Column {
                    Text(
                        text = if (LanguagePrefs.isBengali) "গ্রুপ কিউআর ও ইনভাইট" else "QR CODE & INVITE",
                        style = CentwiseTypography.Caption,
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        shadowElevation = if (isDark) 4.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GroupIconBadge(
                                    type = group.type,
                                    size = 40.dp,
                                    iconSize = 22.dp,
                                    accent = accent
                                )
                                Text(
                                    text = group.name,
                                    style = CentwiseTypography.Title2.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Text(
                                text = GroupStrings.memberCountText(group.type, group.members.size),
                                style = CentwiseTypography.Caption,
                                color = textSecondary
                            )

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                shadowElevation = if (isDark) 6.dp else 2.dp,
                                modifier = Modifier.size(210.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    StylizedQrCode(accent = accent)
                                }
                            }

                            // Join Code Copy Capsule Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Group Code", group.joinCode))
                                        copied = true
                                        val toastMsg = if (LanguagePrefs.isBengali) "কোড কপি করা হয়েছে: ${group.joinCode}" else "Code copied: ${group.joinCode}"
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                        if (AppearancePrefs.hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = GroupStrings.groupJoinCodeLabel,
                                            style = CentwiseTypography.Caption,
                                            color = textSecondary
                                        )
                                        Text(
                                            text = group.joinCode,
                                            style = CentwiseTypography.Title3.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp
                                            ),
                                            color = accent
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = if (copied) accent.copy(alpha = 0.20f) else accent.copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (copied) GroupStrings.copied else GroupStrings.copyCode,
                                                style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.Bold),
                                                color = accent
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = GroupStrings.inviteHint,
                                style = CentwiseTypography.Caption,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = if (LanguagePrefs.isBengali) "ক্যামেরা স্ক্যানার" else "CAMERA SCANNER",
                            style = CentwiseTypography.Caption,
                            color = textSecondary,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            shadowElevation = if (isDark) 4.dp else 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(220.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black.copy(alpha = 0.85f))
                                        .border(2.dp, accent, RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Text(
                                            text = GroupStrings.scannerInstruction,
                                            style = CentwiseTypography.Subheadline,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = if (LanguagePrefs.isBengali) "অথবা কোড লিখুন" else "OR ENTER CODE",
                            style = CentwiseTypography.Caption,
                            color = textSecondary,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            shadowElevation = if (isDark) 4.dp else 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = inputJoinCode,
                                    onValueChange = { inputJoinCode = it.uppercase() },
                                    placeholder = {
                                        Text(
                                            "e.g. GRP-7842",
                                            color = textSecondary.copy(alpha = 0.5f),
                                            fontSize = 15.sp
                                        )
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    ),
                                    textStyle = CentwiseTypography.Headline.copy(
                                        fontSize = 16.sp,
                                        letterSpacing = 2.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = if (inputJoinCode.isNotBlank()) accent else accent.copy(alpha = 0.4f),
                                    modifier = Modifier.clickable(enabled = inputJoinCode.isNotBlank()) {
                                        if (AppearancePrefs.hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        dismissWithAnimation {
                                            onJoinAnother(inputJoinCode.trim())
                                        }
                                    }
                                ) {
                                    Text(
                                        text = GroupStrings.join,
                                        style = CentwiseTypography.Subheadline.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StylizedQrCode(accent: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val size = size.minDimension
        val moduleSize = size / 21f
        val darkColor = Color(0xFF111827)

        drawFinder(Offset(0f, 0f), moduleSize, darkColor, accent)
        drawFinder(Offset(size - 7 * moduleSize, 0f), moduleSize, darkColor, accent)
        drawFinder(Offset(0f, size - 7 * moduleSize), moduleSize, darkColor, accent)

        val pattern = listOf(
            Pair(9, 2), Pair(10, 2), Pair(12, 2), Pair(13, 2),
            Pair(8, 3), Pair(11, 3), Pair(13, 3),
            Pair(9, 4), Pair(12, 4), Pair(10, 5),
            Pair(2, 9), Pair(4, 9), Pair(6, 9), Pair(9, 9), Pair(11, 9), Pair(14, 9), Pair(17, 9),
            Pair(3, 10), Pair(5, 10), Pair(8, 10), Pair(10, 10), Pair(12, 10), Pair(16, 10),
            Pair(2, 11), Pair(7, 11), Pair(9, 11), Pair(13, 11), Pair(15, 11), Pair(18, 11),
            Pair(9, 13), Pair(12, 13), Pair(14, 13), Pair(17, 13),
            Pair(8, 14), Pair(11, 14), Pair(13, 14), Pair(16, 14),
            Pair(9, 16), Pair(12, 16), Pair(15, 16), Pair(18, 16),
            Pair(10, 18), Pair(13, 18), Pair(16, 18), Pair(19, 18)
        )

        pattern.forEach { (x, y) ->
            drawRoundRect(
                color = darkColor,
                topLeft = Offset(x * moduleSize, y * moduleSize),
                size = Size(moduleSize * 0.9f, moduleSize * 0.9f),
                cornerRadius = CornerRadius(moduleSize * 0.25f, moduleSize * 0.25f)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinder(
    topLeft: Offset,
    moduleSize: Float,
    outerColor: Color,
    centerColor: Color
) {
    drawRoundRect(
        color = outerColor,
        topLeft = topLeft,
        size = Size(7 * moduleSize, 7 * moduleSize),
        cornerRadius = CornerRadius(moduleSize * 0.8f, moduleSize * 0.8f)
    )
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(topLeft.x + moduleSize, topLeft.y + moduleSize),
        size = Size(5 * moduleSize, 5 * moduleSize),
        cornerRadius = CornerRadius(moduleSize * 0.6f, moduleSize * 0.6f)
    )
    drawRoundRect(
        color = centerColor,
        topLeft = Offset(topLeft.x + 2 * moduleSize, topLeft.y + 2 * moduleSize),
        size = Size(3 * moduleSize, 3 * moduleSize),
        cornerRadius = CornerRadius(moduleSize * 0.4f, moduleSize * 0.4f)
    )
}
