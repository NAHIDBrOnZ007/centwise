package com.centwise.features.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    onDismiss: () -> Unit,
    onSave: (TransactionItem) -> Unit,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }
    var selectedPaymentMethod by remember { mutableStateOf("bKash") }

    val categories = listOf("Food & Dining", "Groceries", "Transport", "Bills & Utilities", "Entertainment", "Shopping", "Salary")
    val paymentMethods = listOf("bKash", "Nagad", "Rocket", "BRAC Bank", "City Bank", "Cash")

    val bg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bg,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Add Transaction",
                style = CentwiseTypography.Title2,
                color = textPrimary
            )

            // 1. Transaction Type Selector (Income vs Expense)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x24FFFFFF) else Color(0x0A000000))
                    .padding(4.dp)
            ) {
                listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    if (type == TransactionType.INCOME) CentwiseColors.IncomeGreen else CentwiseColors.ExpenseRed
                                } else Color.Transparent
                            )
                            .clickable { selectedType = type }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.displayName,
                            style = CentwiseTypography.Headline,
                            color = if (isSelected) Color.White else textSecondary
                        )
                    }
                }
            }

            // 2. Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (BDT ৳)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = CentwiseTypography.HeroAmount.copy(fontSize = 24.sp)
            )

            // 3. Merchant / Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Merchant / Title (e.g. Foodpanda, Uber)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = CentwiseTypography.Body
            )

            // 4. Payment Method Picker (bKash, Nagad, Rocket, Bank)
            Text(text = "Payment Method", style = CentwiseTypography.Headline.copy(fontSize = 13.sp), color = textSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentMethods.forEach { method ->
                    val isSelected = selectedPaymentMethod == method
                    val brandColor = CentwiseColors.providerColor(method)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) brandColor else if (isDark) Color(0x24FFFFFF) else Color(0x0A000000))
                            .clickable { selectedPaymentMethod = method }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = method,
                            style = CentwiseTypography.Subheadline,
                            color = if (isSelected) Color.White else textPrimary
                        )
                    }
                }
            }

            // 5. Category Chips
            Text(text = "Category", style = CentwiseTypography.Headline.copy(fontSize = 13.sp), color = textSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) CentwiseColors.AccentMauve else if (isDark) Color(0x24FFFFFF) else Color(0x0A000000))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            style = CentwiseTypography.Subheadline,
                            color = if (isSelected) Color.White else textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && title.isNotBlank()) {
                        onSave(
                            TransactionItem(
                                title = title,
                                amount = amount,
                                type = selectedType,
                                category = selectedCategory,
                                paymentMethod = selectedPaymentMethod
                            )
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CentwiseColors.AccentMauve)
            ) {
                Text(
                    text = "Save Transaction",
                    style = CentwiseTypography.Headline,
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditTransactionSheetPreview() {
    AddEditTransactionSheet(
        onDismiss = {},
        onSave = {}
    )
}
