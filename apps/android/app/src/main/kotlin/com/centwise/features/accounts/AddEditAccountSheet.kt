package com.centwise.features.accounts

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.AccountItem
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

private val accountTypes = listOf("MFS Wallet", "Bank Account", "Card", "Cash")
private val providers = listOf(
    "bKash",
    "Nagad",
    "Rocket",
    "Upay",
    "BRAC Bank",
    "City Bank",
    "Dutch-Bangla Bank",
    "Eastern Bank",
    "Islami Bank",
    "Cash",
    "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountSheet(
    initialAccount: AccountItem? = null,
    onDismiss: () -> Unit,
    onSave: (AccountItem) -> Unit,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var accountNumber by remember { mutableStateOf(initialAccount?.accountNumber ?: "") }
    var balanceText by remember {
        mutableStateOf(
            if (initialAccount != null && initialAccount.balance > 0)
                if (initialAccount.balance % 1.0 == 0.0) initialAccount.balance.toLong().toString() else initialAccount.balance.toString()
            else ""
        )
    }
    var selectedType by remember { mutableStateOf(initialAccount?.type ?: accountTypes.first()) }
    var selectedProvider by remember { mutableStateOf(initialAccount?.providerName ?: providers.first()) }
    var providerDropdownExpanded by remember { mutableStateOf(false) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val sheetBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val fieldBg = if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000)

    val isValid = name.isNotBlank() && (balanceText.isBlank() || balanceText.toDoubleOrNull() != null)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = if (initialAccount == null) "Add Account" else "Edit Account",
                style = CentwiseTypography.Title2,
                color = textPrimary
            )

            // 1. Hero Balance Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(fieldBg)
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STARTING BALANCE",
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
                        value = balanceText,
                        onValueChange = { balanceText = it },
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
                            if (balanceText.isEmpty()) {
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

            // 2. Account Name Input
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Account Name (e.g. My Personal bKash, Salary A/C)", color = textSecondary.copy(alpha = 0.6f)) },
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

            // 3. Account / Wallet Number Input (Optional)
            TextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                placeholder = { Text("Account / Wallet Number (Optional)", color = textSecondary.copy(alpha = 0.6f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

            // 4. Account Type Segmented Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Account Type", style = CentwiseTypography.Headline.copy(fontSize = 13.sp), color = textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(fieldBg)
                        .padding(4.dp)
                ) {
                    accountTypes.forEach { type ->
                        val isSelected = selectedType == type
                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) accent else Color.Transparent,
                            animationSpec = spring(),
                            label = "type_pill_bg"
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
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type,
                                style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                                color = if (isSelected) Color.White else textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 5. Provider Selector (Dropdown + Quick Chips)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Provider", style = CentwiseTypography.Headline.copy(fontSize = 13.sp), color = textSecondary)
                    Text(
                        text = "Select from List ▾",
                        style = CentwiseTypography.Caption,
                        color = accent,
                        modifier = Modifier.clickable { providerDropdownExpanded = true }
                    )
                }

                // Horizontal Quick Provider Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    providers.forEach { provider ->
                        val isSelected = selectedProvider == provider
                        val brandColor = CentwiseColors.providerColor(provider)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) brandColor else fieldBg,
                            modifier = Modifier.clickable { selectedProvider = provider }
                        ) {
                            Text(
                                text = provider,
                                style = CentwiseTypography.Subheadline,
                                color = if (isSelected) Color.White else textPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Dropdown Menu
                DropdownMenu(
                    expanded = providerDropdownExpanded,
                    onDismissRequest = { providerDropdownExpanded = false }
                ) {
                    providers.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider) },
                            onClick = {
                                selectedProvider = provider
                                providerDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 6. Save Button (Hero Accent)
            Button(
                onClick = {
                    if (isValid) {
                        onSave(
                            AccountItem(
                                name = name.trim(),
                                type = selectedType,
                                balance = balanceText.toDoubleOrNull() ?: 0.0,
                                providerName = selectedProvider,
                                accountNumber = accountNumber.trim().ifBlank { "0000" }
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White,
                    disabledContainerColor = fieldBg
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialAccount == null) "Save Account" else "Update Account",
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditAccountSheetPreview() {
    AddEditAccountSheet(onDismiss = {}, onSave = {})
}
