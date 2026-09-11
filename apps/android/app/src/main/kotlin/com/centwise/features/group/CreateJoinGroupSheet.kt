package com.centwise.features.group

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.CentwiseSegmentedControl
import com.centwise.core.design.components.DismissKeyboardOnScroll
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
fun CreateJoinGroupSheet(
    onDismiss: () -> Unit,
    onCreateGroup: (name: String, type: GroupType, description: String, target: Double, creatorName: String) -> Unit,
    onJoinGroup: (code: String, memberName: String) -> Unit,
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

    var isCreating by remember { mutableStateOf(true) }

    var groupType by remember { mutableStateOf(GroupType.FAMILY) }
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var monthlyTargetText by remember { mutableStateOf("5000") }
    var creatorName by remember { mutableStateOf("") }

    var joinCode by remember { mutableStateOf("") }
    var joinMemberName by remember { mutableStateOf("") }

    val canProceed = if (isCreating) {
        groupName.isNotBlank() && (monthlyTargetText.toDoubleOrNull() ?: 0.0) > 0
    } else {
        joinCode.isNotBlank()
    }

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
            // 1. Navigation Top Bar (Rounded Pill Buttons for Cancel & Action)
            ModalSheetTopBar(
                title = if (isCreating) GroupStrings.createGroupButton else GroupStrings.joinTitle,
                onCancel = { dismissWithAnimation {} },
                onSave = {
                    if (canProceed) {
                        if (AppearancePrefs.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        dismissWithAnimation {
                            if (isCreating) {
                                onCreateGroup(
                                    groupName.trim(),
                                    groupType,
                                    description.trim(),
                                    monthlyTargetText.toDoubleOrNull() ?: 5000.0,
                                    creatorName.trim()
                                )
                            } else {
                                onJoinGroup(joinCode.trim(), joinMemberName.trim())
                            }
                        }
                    }
                },
                saveEnabled = canProceed,
                saveLabel = if (isCreating) GroupStrings.create else GroupStrings.join,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDark = isDark
            )

            // 2. Centwise Sliding Segmented Switcher
            CentwiseSegmentedControl(
                items = listOf(true, false),
                selectedItem = isCreating,
                onItemSelected = {
                    isCreating = it
                    if (AppearancePrefs.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                itemLabel = { if (it) GroupStrings.newGroupTab else GroupStrings.joinCodeTab },
                itemIcon = { if (it) Icons.Default.AddHome else Icons.Default.QrCodeScanner },
                modifier = Modifier.height(36.dp),
                accent = accent,
                isDark = isDark
            )

            if (isCreating) {
                // 3. Group Type Selection Card
                Column {
                    Text(
                        text = if (LanguagePrefs.isBengali) "গ্রুপের ধরন" else "GROUP TYPE",
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(GroupType.entries) { type ->
                                val isSelected = type == groupType
                                val chipBg by animateColorAsState(
                                    targetValue = if (isSelected) accent else if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000),
                                    label = "type_bg"
                                )
                                val textColor = if (isSelected) Color.White else textPrimary

                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(chipBg)
                                        .clickable {
                                            groupType = type
                                            if (groupName.isBlank()) {
                                                groupName = GroupStrings.defaultGroupNameFor(type)
                                            }
                                            if (AppearancePrefs.hapticsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = type.vectorIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = GroupStrings.groupTypeDisplay(type),
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

                // 4. Group Information Form Card (Matching Details Card in AddEditTransactionSheet)
                Column {
                    Text(
                        text = if (LanguagePrefs.isBengali) "গ্রুপের তথ্য" else "GROUP INFORMATION",
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
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            // Group Name
                            TextField(
                                value = groupName,
                                onValueChange = { groupName = it },
                                placeholder = {
                                    Text(
                                        GroupStrings.groupNamePlaceholder,
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
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                textStyle = CentwiseTypography.Body.copy(fontSize = 15.sp)
                            )

                            HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                            // Description / Address
                            TextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = {
                                    Text(
                                        if (LanguagePrefs.isBengali) "যেমন: ফ্ল্যাট ৪বি, রোড ২৭" else "e.g. Flat 4B, Road 27",
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
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                textStyle = CentwiseTypography.Body.copy(fontSize = 15.sp)
                            )

                            HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                            // Monthly Target
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (LanguagePrefs.isBengali) "মাসিক জনপ্রতি বাজেট টার্গেট (৳)" else "Monthly Target per Member (৳)",
                                    style = CentwiseTypography.Body,
                                    color = textPrimary,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                TextField(
                                    value = monthlyTargetText,
                                    onValueChange = { newText ->
                                        monthlyTargetText = newText.filter { it.isDigit() }
                                    },
                                    placeholder = { Text("5000", color = textSecondary.copy(alpha = 0.5f), fontSize = 15.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = accent,
                                        unfocusedTextColor = accent
                                    ),
                                    modifier = Modifier.weight(1f),
                                    textStyle = CentwiseTypography.Headline.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )
                                )
                            }

                            HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                            // Creator Name (Admin)
                            TextField(
                                value = creatorName,
                                onValueChange = { creatorName = it },
                                placeholder = {
                                    Text(
                                        if (LanguagePrefs.isBengali) "আপনার নাম (অ্যাডমিন)" else "Your name (Admin)",
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
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                textStyle = CentwiseTypography.Body.copy(fontSize = 15.sp)
                            )
                        }
                    }
                }
            } else {
                // Join Code Card
                Column {
                    Text(
                        text = if (LanguagePrefs.isBengali) "গ্রুপে যুক্ত হোন" else "JOIN BY CODE",
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
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            TextField(
                                value = joinCode,
                                onValueChange = { joinCode = it.uppercase() },
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
                                    focusedTextColor = accent,
                                    unfocusedTextColor = accent
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                textStyle = CentwiseTypography.Headline.copy(
                                    fontSize = 17.sp,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                            TextField(
                                value = joinMemberName,
                                onValueChange = { joinMemberName = it },
                                placeholder = {
                                    Text(
                                        GroupStrings.enterMemberNamePlaceholder,
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
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                textStyle = CentwiseTypography.Body.copy(fontSize = 15.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
