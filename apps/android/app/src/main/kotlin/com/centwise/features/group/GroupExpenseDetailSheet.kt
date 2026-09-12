package com.centwise.features.group

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.features.settings.LanguagePrefs
import com.centwise.core.profile.UserPrefs
import kotlinx.coroutines.launch

@Composable
fun GroupExpenseDetailSheet(
    expense: DailyExpenseEntry,
    memberCount: Int,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (DailyExpenseEntry) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    GroupExpenseDetailSheet(
        dayGroup = DayExpenseGroup(date = expense.date, entries = listOf(expense)),
        memberCount = memberCount,
        onDismiss = onDismiss,
        onDeleteEntry = onDelete,
        onDeleteAllForDate = { onDelete(expense.id) },
        onEditEntry = onEdit,
        accent = accent,
        modifier = modifier,
        isDark = isDark
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupExpenseDetailSheet(
    dayGroup: DayExpenseGroup,
    memberCount: Int,
    onDismiss: () -> Unit,
    onDeleteEntry: (String) -> Unit,
    onDeleteAllForDate: (String) -> Unit,
    onEditEntry: (DailyExpenseEntry) -> Unit,
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

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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

    val totalDayAmount = dayGroup.totalAmount
    val safeMemberCount = if (memberCount > 0) memberCount else 1
    val splitPerMember = totalDayAmount / safeMemberCount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bg,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Navigation Top Bar (Close Pill on left, Title in center, Solid Accent Edit Pill Button on right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel / Close Pill Button
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0x28FFFFFF) else Color(0x16000000),
                    modifier = Modifier
                        .clip(CircleShape)
                        .iosBounceClick {
                            dismissWithAnimation {}
                        }
                ) {
                    Text(
                        text = GroupStrings.cancel,
                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = textPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }

                Text(
                    text = GroupStrings.expenseDetailsTitle,
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )

                // Solid Accent Edit Pill Button (Matching TransactionDetailSheet 1:1)
                Surface(
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .iosBounceClick {
                            dismissWithAnimation {
                                onEditEntry(dayGroup.entries.first())
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = GroupStrings.edit,
                            style = CentwiseTypography.Headline.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // 2. Hero Amount Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = CurrencyFormatter.formatBDT(totalDayAmount, useBengaliNumerals = LanguagePrefs.isBengali),
                    style = CentwiseTypography.HeroAmount.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),
                    color = CentwiseColors.ExpenseRed
                )

                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0x1FFFFFFF) else Color(0x10000000)
                ) {
                    Text(
                        text = dayGroup.displayDate,
                        style = CentwiseTypography.Caption,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        fontSize = 12.sp
                    )
                }
            }

            // 3. Purchases by Member - Each User Shown Separately One by One
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = (if (dayGroup.entries.size > 1) {
                        if (LanguagePrefs.isBengali) "সদস্যভিত্তিক খরচের বিবরণ" else "Purchases by Member"
                    } else {
                        GroupStrings.paidByLabel
                    }).uppercase(),
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                dayGroup.entries.forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = if (isDark) 4.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Spender Header Row (Avatar, Name, Amount, Individual Edit Button)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = UserPrefs.getAvatarResId(entry.spenderAvatar)),
                                        contentDescription = entry.spenderName,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, accent.copy(alpha = 0.35f), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column {
                                        Text(
                                            text = entry.spenderName,
                                            style = CentwiseTypography.Headline.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                            color = textPrimary
                                        )
                                        val itemCountText = if (entry.items.isNotEmpty()) {
                                            if (LanguagePrefs.isBengali) "${entry.items.size}টি জিনিস কেনা হয়েছে" else "${entry.items.size} items bought"
                                        } else {
                                            if (LanguagePrefs.isBengali) "১টি সাধারণ খরচ" else "1 entry"
                                        }
                                        Text(
                                            text = itemCountText,
                                            style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                                            color = textSecondary
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = CurrencyFormatter.formatBDT(entry.totalAmount, useBengaliNumerals = LanguagePrefs.isBengali),
                                        style = CentwiseTypography.Headline.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                                        color = textPrimary
                                    )

                                    // Individual Edit Button for this specific spender entry
                                    Surface(
                                        shape = CircleShape,
                                        color = accent.copy(alpha = 0.14f),
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .iosBounceClick {
                                                dismissWithAnimation {
                                                    onEditEntry(entry)
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = accent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Items bought by THIS user
                            if (entry.items.isNotEmpty()) {
                                HorizontalDivider(color = dividerColor, thickness = 0.8.dp)

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    entry.items.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                ExpenseItemIconBadge(
                                                    itemName = item.name,
                                                    accent = accent,
                                                    size = 30.dp
                                                )

                                                Column {
                                                    Text(
                                                        text = item.displayName,
                                                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                                        color = textPrimary
                                                    )
                                                    if (item.displayQuantity.isNotBlank()) {
                                                        Text(
                                                            text = item.displayQuantity,
                                                            style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                                                            color = textSecondary
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = CurrencyFormatter.formatBDT(item.price, useBengaliNumerals = LanguagePrefs.isBengali),
                                                style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                                color = textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Notes for THIS user (if any)
                            if (entry.notes.isNotBlank()) {
                                HorizontalDivider(color = dividerColor, thickness = 0.8.dp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (LanguagePrefs.isBengali) "নোট:" else "Note:",
                                        style = CentwiseTypography.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                        color = textSecondary
                                    )
                                    Text(
                                        text = entry.displayNotes,
                                        style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                                        color = textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Equal Split Breakdown Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 4.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = GroupStrings.perMemberShare,
                            style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                        Text(
                            text = "(${safeMemberCount} " + (if (LanguagePrefs.isBengali) "জন সদস্যের মাঝে সমান ভাগ" else "members equal split") + ")",
                            style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                            color = textSecondary.copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        text = CurrencyFormatter.formatBDT(splitPerMember, useBengaliNumerals = LanguagePrefs.isBengali) + GroupStrings.perPersonSuffix,
                        style = CentwiseTypography.Headline.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = accent
                    )
                }
            }

            // 5. Destructive Delete Button at Bottom
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 4.dp else 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .iosBounceClick { showDeleteConfirmDialog = true }
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = if (dayGroup.entries.size > 1) GroupStrings.deleteAllForDate else GroupStrings.deleteExpense,
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = CentwiseColors.ExpenseRed,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = if (dayGroup.entries.size > 1) GroupStrings.deleteAllForDateConfirmTitle else GroupStrings.deleteConfirmTitle,
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Text(
                    text = if (dayGroup.entries.size > 1) GroupStrings.deleteAllForDateConfirmMessage else GroupStrings.deleteConfirmMessage,
                    style = CentwiseTypography.Body,
                    color = textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        dismissWithAnimation {
                            if (dayGroup.entries.size > 1) {
                                onDeleteAllForDate(dayGroup.date)
                            } else {
                                onDeleteEntry(dayGroup.entries.first().id)
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (dayGroup.entries.size > 1) GroupStrings.deleteAllForDate else GroupStrings.deleteExpense,
                        color = CentwiseColors.ExpenseRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(
                        text = GroupStrings.cancel,
                        color = textSecondary
                    )
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(18.dp)
        )
    }
}
