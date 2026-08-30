package com.centwise.features.transactions

import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.CategoryIconHelper
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.data.repository.TransactionRepository
import com.centwise.features.accounts.providerIcon
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var selectedPaymentMethod by remember { mutableStateOf(initialTransaction?.paymentMethod ?: "Cash") }
    var notes by remember { mutableStateOf(initialTransaction?.note ?: initialTransaction?.rawSms ?: "") }

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    val categories by TransactionRepository.shared.categories.collectAsState()
    val accounts by TransactionRepository.shared.accounts.collectAsState()

    val availableAccounts = remember(accounts) {
        val list = mutableListOf("Cash")
        list.addAll(accounts.filterNot { it.archived }.map { it.name })
        list.addAll(listOf("bKash", "Nagad", "Rocket", "Upay", "CellFin", "BRAC Bank", "City Bank"))
        list.distinct()
    }

    LaunchedEffect(categories, initialTransaction) {
        if (initialTransaction != null) {
            selectedCategory = initialTransaction.category
        } else if (selectedCategory.isEmpty()) {
            selectedCategory = categories.firstOrNull()?.name ?: "Food & Dining"
        }
    }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val menuBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val menuBorder = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x0F000000))

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = title.trim().isNotBlank() && parsedAmount > 0

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
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Navigation Top Bar (Rounded Pill Buttons for Cancel & Save)
            com.centwise.core.design.components.ModalSheetTopBar(
                title = if (initialTransaction == null) "New Transaction" else "Edit Transaction",
                onCancel = { dismissWithAnimation {} },
                onSave = {
                    if (isValid) {
                        val newTx = TransactionItem(
                            id = initialTransaction?.id ?: java.util.UUID.randomUUID().toString(),
                            title = title.trim(),
                            amount = parsedAmount,
                            type = selectedType,
                            category = selectedCategory,
                            paymentMethod = selectedPaymentMethod,
                            timestamp = initialTransaction?.timestamp ?: System.currentTimeMillis(),
                            note = notes.ifBlank { null },
                            reference = initialTransaction?.reference,
                            rawSms = initialTransaction?.rawSms
                        )
                        dismissWithAnimation {
                            onSave(newTx)
                        }
                    }
                },
                saveEnabled = isValid,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDark = isDark
            )

            // 2. Amount Hero Section (Theme Accent Uniform Color)
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
                        text = "AMOUNT",
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
                            onValueChange = { amountText = it },
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
                }
            }

            // 3. Compact Sliding Segmented Type Picker
            com.centwise.core.design.components.CentwiseSegmentedControl(
                items = TransactionType.entries,
                selectedItem = selectedType,
                onItemSelected = { selectedType = it },
                itemLabel = { it.displayName },
                modifier = Modifier.height(34.dp),
                accent = accent,
                isDark = isDark
            )

            // 4. Details Form Card (Merchant, Category, Account, Date)
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
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        // Title Input
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Merchant or title", color = textSecondary.copy(alpha = 0.5f), fontSize = 15.sp) },
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

                        // Category Picker Row
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { showCategoryMenu = true }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Category", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.iconFor(selectedCategory),
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = selectedCategory,
                                        style = CentwiseTypography.Body,
                                        color = textSecondary,
                                        fontSize = 15.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = textSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false },
                                offset = DpOffset(0.dp, 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = menuBg,
                                shadowElevation = 8.dp,
                                border = menuBorder
                            ) {
                                categories.forEach { cat ->
                                    val isSelected = selectedCategory == cat.name
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = CategoryIconHelper.iconFor(cat.name, cat.icon),
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = cat.name,
                                                    style = CentwiseTypography.Body,
                                                    color = textPrimary,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedCategory = cat.name
                                            showCategoryMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        // Account / Wallet Picker Row
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { showAccountMenu = true }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Account / Wallet", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = providerIcon(selectedPaymentMethod),
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = selectedPaymentMethod,
                                        style = CentwiseTypography.Body,
                                        color = textSecondary,
                                        fontSize = 15.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = textSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false },
                                offset = DpOffset(0.dp, 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = menuBg,
                                shadowElevation = 8.dp,
                                border = menuBorder
                            ) {
                                availableAccounts.forEach { acc ->
                                    val isSelected = selectedPaymentMethod == acc
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = providerIcon(acc),
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = acc,
                                                    style = CentwiseTypography.Body,
                                                    color = textPrimary,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedPaymentMethod = acc
                                            showAccountMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        // Date & Time Row
                        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
                        val txDate = Date(initialTransaction?.timestamp ?: System.currentTimeMillis())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Date & Time", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)
                            Text(
                                text = dateFormat.format(txDate),
                                style = CentwiseTypography.Body,
                                color = textSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // 5. Notes Section (Optional)
            Column {
                Text(
                    text = "NOTES (OPTIONAL)",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    shadowElevation = if (isDark) 4.dp else 1.dp
                ) {
                    TextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Add notes or tags...", color = textSecondary.copy(alpha = 0.5f), fontSize = 15.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        textStyle = CentwiseTypography.Body.copy(fontSize = 15.sp),
                        maxLines = 3
                    )
                }
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
