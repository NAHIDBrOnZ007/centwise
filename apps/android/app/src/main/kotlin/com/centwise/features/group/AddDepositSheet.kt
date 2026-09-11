package com.centwise.features.group

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.CentwiseSegmentedControl
import com.centwise.core.design.components.DismissKeyboardOnScroll
import com.centwise.core.design.components.ModalSheetTopBar
import com.centwise.core.design.components.clearFocusOnTapOutside
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.core.profile.UserPrefs
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.settings.LanguagePrefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDepositSheet(
    group: SharedGroup,
    onDismiss: () -> Unit,
    onSave: (memberId: String, amount: Double, paymentMethod: String, note: String) -> Unit,
    accent: Color = AccentOptions.byName(AppearancePrefs.accentName).color,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val haptic = LocalHapticFeedback.current
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var selectedMemberId by remember {
        mutableStateOf(group.members.firstOrNull()?.id ?: "")
    }

    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("") }

    val methods = listOf("Cash", "bKash", "Nagad", "Bank")
    val presetAmounts = listOf(1000.0, 2000.0, 3000.0, 5000.0)

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val canSave = selectedMemberId.isNotBlank() && parsedAmount > 0

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
        DismissKeyboardOnScroll(scrollState)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clearFocusOnTapOutside()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Navigation Top Bar (Rounded Pill Buttons for Cancel & Confirm)
            ModalSheetTopBar(
                title = GroupStrings.addDepositTitle,
                onCancel = { dismissWithAnimation {} },
                onSave = {
                    if (canSave) {
                        if (AppearancePrefs.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        dismissWithAnimation {
                            onSave(selectedMemberId, parsedAmount, selectedMethod, note.trim())
                        }
                    }
                },
                saveEnabled = canSave,
                saveLabel = GroupStrings.confirmDeposit,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDark = isDark
            )

            // 2. Amount Hero Section (Exact matching AddEditTransactionSheet 1:1)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                shadowElevation = if (isDark) 4.dp else 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (LanguagePrefs.isBengali) "জমার পরিমাণ" else "DEPOSIT AMOUNT",
                        style = CentwiseTypography.Caption,
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "৳ ",
                            style = CentwiseTypography.LargeTitle.copy(fontSize = 32.sp),
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        BasicTextField(
                            value = amountText,
                            onValueChange = { newText ->
                                amountText = newText.filter { it.isDigit() || it == '.' }
                            },
                            textStyle = CentwiseTypography.LargeTitle.copy(
                                fontSize = 34.sp,
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            cursorBrush = SolidColor(accent),
                            decorationBox = { innerTextField ->
                                if (amountText.isEmpty()) {
                                    Text(
                                        text = "0.00",
                                        style = CentwiseTypography.LargeTitle.copy(
                                            fontSize = 34.sp,
                                            color = textSecondary.copy(alpha = 0.35f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset Quick Pill Buttons (CircleShape capsule pills)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetAmounts.forEach { preset ->
                            val isSelected = parsedAmount == preset
                            val pillBg by animateColorAsState(
                                targetValue = if (isSelected) accent else if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000),
                                label = "preset_pill_bg"
                            )
                            val pillTextColor = if (isSelected) Color.White else textPrimary

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(pillBg)
                                    .clickable {
                                        amountText = preset.toLong().toString()
                                        if (AppearancePrefs.hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+৳${preset.toLong()}",
                                    style = CentwiseTypography.Caption.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = pillTextColor
                                )
                            }
                        }
                    }
                }
            }

            // 3. Compact Sliding Segmented Payment Method Picker (CentwiseSegmentedControl)
            Column {
                Text(
                    text = if (LanguagePrefs.isBengali) "পেমেন্ট মাধ্যম" else "PAYMENT METHOD",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                CentwiseSegmentedControl(
                    items = methods,
                    selectedItem = selectedMethod,
                    onItemSelected = {
                        selectedMethod = it
                        if (AppearancePrefs.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    itemLabel = {
                        if (LanguagePrefs.isBengali) {
                            when (it) {
                                "Cash" -> "ক্যাশ"
                                "bKash" -> "বিকাশ"
                                "Nagad" -> "নগদ"
                                "Bank" -> "ব্যাংক"
                                else -> it
                            }
                        } else it
                    },
                    itemIcon = {
                        when (it.lowercase()) {
                            "cash" -> Icons.Default.Payments
                            "bkash", "nagad" -> Icons.Default.PhoneAndroid
                            else -> Icons.Default.AccountBalance
                        }
                    },
                    modifier = Modifier.height(36.dp),
                    accent = accent,
                    isDark = isDark
                )
            }

            // 4. Member Selector Card (Matching iOS Pill Buttons with Avatars)
            Column {
                Text(
                    text = if (LanguagePrefs.isBengali) "সদস্য নির্বাচন" else "MEMBER",
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
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(group.members) { member ->
                            val isSelected = member.id == selectedMemberId
                            val chipBg by animateColorAsState(
                                targetValue = if (isSelected) accent else if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000),
                                label = "deposit_member_bg"
                            )
                            val textColor = if (isSelected) Color.White else textPrimary

                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(chipBg)
                                    .clickable {
                                        selectedMemberId = member.id
                                        if (AppearancePrefs.hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = UserPrefs.getAvatarResId(member.avatar)),
                                    contentDescription = member.name,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = member.name,
                                    style = CentwiseTypography.Subheadline.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // 5. Note / TrxID Card (Matching Details Form Input in AddEditTransactionSheet)
            Column {
                Text(
                    text = if (LanguagePrefs.isBengali) "মন্তব্য / ট্রানজেকশন আইডি" else "NOTE / TRANSACTION ID (OPTIONAL)",
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
                    TextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = {
                            Text(
                                GroupStrings.depositNotesPlaceholder,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        textStyle = CentwiseTypography.Body.copy(fontSize = 15.sp)
                    )
                }
            }
        }
    }
}
