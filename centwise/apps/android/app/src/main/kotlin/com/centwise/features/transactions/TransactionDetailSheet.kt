package com.centwise.features.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionItem,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val bg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) CentwiseColors.IncomeGreen else CentwiseColors.ExpenseRed
    val amountPrefix = if (isIncome) "+ " else "- "

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bg,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title and Amount Hero
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = transaction.title,
                    style = CentwiseTypography.Title1,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$amountPrefix${CurrencyFormatter.formatBDT(transaction.amount)}",
                    style = CentwiseTypography.HeroAmount.copy(fontSize = 36.sp),
                    color = amountColor
                )
            }

            HorizontalDivider(
                color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000),
                thickness = 1.dp
            )

            // Detail Metadata Rows
            DetailRow(label = "Type", value = transaction.type.displayName, textPrimary = textPrimary, textSecondary = textSecondary)
            DetailRow(label = "Category", value = transaction.category, textPrimary = textPrimary, textSecondary = textSecondary)
            DetailRow(label = "Payment Method", value = transaction.paymentMethod, textPrimary = textPrimary, textSecondary = textSecondary)

            // Raw SMS Box (if present)
            if (!transaction.rawSms.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium))
                        .background(if (isDark) Color(0x14FFFFFF) else Color(0x08000000))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Original SMS Payload",
                        style = CentwiseTypography.Caption,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.rawSms,
                        style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                        color = textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons (Delete)
            Button(
                onClick = {
                    onDelete(transaction.id)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CentwiseColors.ExpenseRed.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = CentwiseColors.ExpenseRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delete Transaction",
                    style = CentwiseTypography.Headline,
                    color = CentwiseColors.ExpenseRed
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = CentwiseTypography.Body,
            color = textSecondary
        )
        Text(
            text = value,
            style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
            color = textPrimary
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
