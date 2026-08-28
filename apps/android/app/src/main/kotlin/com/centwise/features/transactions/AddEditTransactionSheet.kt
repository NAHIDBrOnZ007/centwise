package com.centwise.features.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.data.repository.TransactionRepository
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    initialTransaction: TransactionItem? = null,
    onDismiss: () -> Unit,
    onSave: (TransactionItem) -> Boolean,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var title by remember { mutableStateOf(initialTransaction?.title ?: "") }
    var amountText by remember {
        mutableStateOf(
            if (initialTransaction != null && initialTransaction.amount > 0)
                if (initialTransaction.amount % 1.0 == 0.0) initialTransaction.amount.toLong().toString() else initialTransaction.amount.toString()
            else ""
        )
    }
    var selectedType by remember { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(initialTransaction?.category ?: "") }
    var selectedPaymentMethod by remember { mutableStateOf(initialTransaction?.paymentMethod ?: "bKash") }

    val categories by TransactionRepository.shared.categories.collectAsState()
    val accounts by TransactionRepository.shared.accounts.collectAsState()
    val paymentMethods = remember(accounts) {
        (accounts.filterNot { it.archived }.map { it.name } +
            listOf("Cash", "bKash", "Nagad", "Rocket", "Upay", "CellFin", "BRAC Bank", "City Bank"))
            .distinct()
    }

    LaunchedEffect(categories, initialTransaction) {
        if (initialTransaction != null) {
            selectedCategory = initialTransaction.category
        } else if (selectedCategory.isEmpty()) {
            selectedCategory = categories.firstOrNull()?.name.orEmpty()
        }
    }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val fieldBg = if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTransaction == null) "Add Transaction" else "Edit Transaction",
                    style = CentwiseTypography.Title2,
                    color = textPrimary
                )
            }

            // 1. Transaction Type Segmented Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(fieldBg)
                    .padding(4.dp)
            ) {
                listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                    val isSelected = selectedType == type
                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (type == TransactionType.INCOME) CentwiseColors.IncomeGreen else CentwiseColors.ExpenseRed
                        } else Color.Transparent,
                        animationSpec = spring(),
                        label = "pill_bg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(pillBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.displayName,
                            style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                            color = if (isSelected) Color.White else textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Hero Amount Input Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(fieldBg)
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AMOUNT (BDT)",
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
                        color = if (selectedType == TransactionType.INCOME) CentwiseColors.IncomeGreen else accent,
                        fontWeight = FontWeight.Bold
                    )
                    BasicTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        textStyle = CentwiseTypography.LargeTitle.copy(
                            fontSize = 32.sp,
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
                                        fontSize = 32.sp,
                                        color = textSecondary.copy(alpha = 0.4f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // 3. Merchant / Title Input (Smooth Rounded Filled Field)
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Merchant / Title (e.g. Foodpanda, Uber)", color = textSecondary.copy(alpha = 0.6f)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                modifier = Modifier.fillMaxWidth(),
                textStyle = CentwiseTypography.Body
            )

            // 4. Payment Method Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) brandColor else fieldBg,
                            modifier = Modifier.clickable { selectedPaymentMethod = method }
                        ) {
                            Text(
                                text = method,
                                style = CentwiseTypography.Subheadline,
                                color = if (isSelected) Color.White else textPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 5. Category Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Category", style = CentwiseTypography.Headline.copy(fontSize = 13.sp), color = textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat.name
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) accent else fieldBg,
                            modifier = Modifier.clickable { selectedCategory = cat.name }
                        ) {
                            Text(
                                text = cat.name,
                                style = CentwiseTypography.Subheadline,
                                color = if (isSelected) Color.White else textPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 6. Save Button (Vibrant & Premium)
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && title.isNotBlank()) {
                        val saved = onSave(
                            TransactionItem(
                                id = initialTransaction?.id ?: java.util.UUID.randomUUID().toString(),
                                title = title,
                                amount = amount,
                                type = selectedType,
                                category = selectedCategory,
                                paymentMethod = selectedPaymentMethod,
                                timestamp = initialTransaction?.timestamp ?: System.currentTimeMillis(),
                                note = initialTransaction?.note,
                                reference = initialTransaction?.reference,
                                rawSms = initialTransaction?.rawSms
                            )
                        )
                        if (saved) onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialTransaction == null) "Save Transaction" else "Update Transaction",
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.Bold
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
        onSave = { true }
    )
}
