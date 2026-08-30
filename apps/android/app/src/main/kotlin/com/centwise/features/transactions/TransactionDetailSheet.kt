package com.centwise.features.transactions

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionItem,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onEdit: ((TransactionItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) CentwiseColors.IncomeGreen else CentwiseColors.ExpenseRed
    val amountPrefix = if (isIncome) "+ " else "- "
    val badgeBg = if (isIncome) CentwiseColors.IncomeGreen.copy(alpha = 0.12f) else CentwiseColors.ExpenseRed.copy(alpha = 0.12f)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

            // 1. Navigation Top Bar (Close on left, Details in center, Edit accent button on right if onEdit is provided)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close Pill Button
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
                        text = "Close",
                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = textPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }

                Text(
                    text = "Details",
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )

                if (onEdit != null) {
                    // Edit Solid Accent Action Pill Button
                    Surface(
                        shape = CircleShape,
                        color = accent,
                        modifier = Modifier
                            .clip(CircleShape)
                            .iosBounceClick {
                                dismissWithAnimation {
                                    onEdit(transaction)
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
                                text = "Edit",
                                style = CentwiseTypography.Headline.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(68.dp))
                }
            }

            // 2. Hero Amount Section (Matching iOS 1:1)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$amountPrefix${CurrencyFormatter.formatBDT(transaction.amount)}",
                    style = CentwiseTypography.HeroAmount.copy(fontSize = 38.sp, fontWeight = FontWeight.Bold),
                    color = amountColor
                )

                Surface(
                    shape = CircleShape,
                    color = badgeBg
                ) {
                    Text(
                        text = if (isIncome) "↓ INCOME" else "↑ EXPENSE",
                        style = CentwiseTypography.Caption,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        fontSize = 11.sp
                    )
                }
            }

            // 3. Structured Details Inset Card
            Column {
                Text(
                    text = "DETAILS",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
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
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        DetailRowItem(label = "Merchant", value = transaction.title, textPrimary = textPrimary, textSecondary = textSecondary)
                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        DetailRowItem(label = "Category", value = transaction.category, textPrimary = textPrimary, textSecondary = textSecondary)
                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
                        DetailRowItem(label = "Date", value = dateFormat.format(Date(transaction.timestamp)), textPrimary = textPrimary, textSecondary = textSecondary)
                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        DetailRowItem(label = "Bank", value = transaction.paymentMethod, textPrimary = textPrimary, textSecondary = textSecondary)

                        val ref = transaction.reference
                        if (!ref.isNullOrBlank()) {
                            HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                            DetailRowItem(label = "Reference", value = ref, textPrimary = textPrimary, textSecondary = textSecondary)
                        }

                        val notes = transaction.note ?: transaction.rawSms
                        if (!notes.isNullOrBlank()) {
                            HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                            DetailRowItem(label = "Notes", value = notes, textPrimary = textPrimary, textSecondary = textSecondary)
                        }
                    }
                }
            }

            // 4. Destructive Delete Transaction Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                shadowElevation = if (isDark) 4.dp else 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .iosBounceClick { showDeleteConfirmDialog = true }
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "Delete Transaction",
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
            title = { Text("Delete Transaction", style = CentwiseTypography.Headline) },
            text = { Text("Are you sure you want to delete this transaction?", style = CentwiseTypography.Body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        dismissWithAnimation {
                            onDelete(transaction.id)
                        }
                    }
                ) {
                    Text("Delete", color = CentwiseColors.ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }
}

@Composable
private fun DetailRowItem(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = CentwiseTypography.Body,
            color = textSecondary,
            fontSize = 15.sp,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = value,
            style = CentwiseTypography.Body,
            fontWeight = FontWeight.Medium,
            color = textPrimary,
            fontSize = 15.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionDetailSheetPreview() {
    TransactionDetailSheet(
        transaction = TransactionItem(
            title = "Foodpanda BD",
            amount = 650.0,
            type = TransactionType.EXPENSE,
            category = "Food & Dining",
            paymentMethod = "bKash",
            rawSms = "Payment Tk 650.00 to Foodpanda successful. Ref: FP8392. Fee Tk 0.00. Balance Tk 14,250.00."
        ),
        onDismiss = {},
        onDelete = {}
    )
}
