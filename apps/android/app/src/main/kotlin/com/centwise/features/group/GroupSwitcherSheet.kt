package com.centwise.features.group

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.settings.LanguagePrefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSwitcherSheet(
    allGroups: List<SharedGroup>,
    currentGroupId: String,
    onSelectGroup: (String) -> Unit,
    onCreateNewGroup: () -> Unit,
    onJoinGroup: () -> Unit,
    onDismiss: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBorder = if (isDark) Color(0x14FFFFFF) else Color(0x0F000000)
    val dividerColor = if (isDark) Color(0x14FFFFFF) else Color(0x0D000000)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

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
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Navigation Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0x28FFFFFF) else Color(0x16000000),
                    modifier = Modifier
                        .clip(CircleShape)
                        .iosBounceClick { dismissWithAnimation {} }
                ) {
                    Text(
                        text = GroupStrings.cancel,
                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = textPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }

                Text(
                    text = GroupStrings.switchGroupTitle,
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )

                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${allGroups.size}",
                        style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // 2. Subtitle Label
            Text(
                text = GroupStrings.yourGroups.uppercase(),
                style = CentwiseTypography.Caption,
                color = textSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )

            // 3. List of Groups
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(allGroups, key = { it.id }) { group ->
                    val isSelected = group.id == currentGroupId
                    val userBalance = group.userBalance

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accent else cardBorder
                        ),
                        shadowElevation = if (isDark) 4.dp else 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .iosBounceClick {
                                if (AppearancePrefs.hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                dismissWithAnimation {
                                    onSelectGroup(group.id)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Group Type Icon Badge
                                GroupIconBadge(
                                    type = group.type,
                                    size = 42.dp,
                                    iconSize = 22.dp,
                                    accent = accent
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = group.displayName,
                                            style = CentwiseTypography.Headline.copy(
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            color = textPrimary,
                                            maxLines = 1
                                        )

                                        // New Activity Notification Badge Pill
                                        if (group.hasNewActivity) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = accent
                                            ) {
                                                Text(
                                                    text = GroupStrings.newBadge,
                                                    style = CentwiseTypography.Caption.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "${GroupStrings.membersCountText(group.members.size)} • ${group.monthName}",
                                        style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                                        color = textSecondary
                                    )

                                    // User net balance chip
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val (badgeBg, badgeText, badgeColor) = when {
                                            userBalance > 0 -> Triple(
                                                accent.copy(alpha = 0.12f),
                                                GroupStrings.surplusPrefix + CurrencyFormatter.formatBDT(userBalance, useBengaliNumerals = LanguagePrefs.isBengali),
                                                accent
                                            )
                                            userBalance < 0 -> Triple(
                                                CentwiseColors.ExpenseRed.copy(alpha = 0.12f),
                                                GroupStrings.duePrefix + CurrencyFormatter.formatBDT(kotlin.math.abs(userBalance), useBengaliNumerals = LanguagePrefs.isBengali),
                                                CentwiseColors.ExpenseRed
                                            )
                                            else -> Triple(
                                                if (isDark) Color(0x1FFFFFFF) else Color(0x10000000),
                                                GroupStrings.settledUp,
                                                textSecondary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = badgeBg
                                        ) {
                                            Text(
                                                text = badgeText,
                                                style = CentwiseTypography.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                                color = badgeColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Selected Checkmark or Trailing Arrow
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = dividerColor, thickness = 0.8.dp)

            // 4. Action Buttons at Bottom (+ Create New Group & Join with Code)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Join Group Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .iosBounceClick {
                            dismissWithAnimation {
                                onJoinGroup()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = GroupStrings.joinGroupCodeBtn,
                            style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = textPrimary
                        )
                    }
                }

                // Create New Group Button (Solid Accent)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accent,
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(14.dp))
                        .iosBounceClick {
                            dismissWithAnimation {
                                onCreateNewGroup()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = GroupStrings.createNewGroupBtn,
                            style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
