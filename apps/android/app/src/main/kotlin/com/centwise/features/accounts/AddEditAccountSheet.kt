package com.centwise.features.accounts

import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.AccountItem
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

private val accountTypes = listOf("MFS Wallet", "Bank Account", "Card", "Cash")
private val providers = listOf(
    "bKash", "Nagad", "Rocket", "Upay", "CellFin",
    "BRAC Bank", "City Bank", "Dutch-Bangla Bank", "Eastern Bank", "Islami Bank",
    "Cash", "Other"
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
    var showProviderMenu by remember { mutableStateOf(false) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val menuBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val menuBorder = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x0F000000))

    val parsedBalance = balanceText.toDoubleOrNull() ?: 0.0
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
                title = if (initialAccount == null) "New Account" else "Edit Account",
                onCancel = { dismissWithAnimation {} },
                onSave = {
                    val finalName = if (name.trim().isBlank()) selectedProvider else name.trim()
                    val newAcct = AccountItem(
                        id = initialAccount?.id ?: java.util.UUID.randomUUID().toString(),
                        name = finalName,
                        type = selectedType,
                        balance = parsedBalance,
                        providerName = selectedProvider,
                        accountNumber = accountNumber.trim().ifBlank { "0000" }
                    )
                    dismissWithAnimation {
                        onSave(newAcct)
                    }
                },
                saveEnabled = true,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDark = isDark
            )

            // 2. Account Information Section Card (Exact matching iOS AddEditAccountScreen)
            Column {
                Text(
                    text = "ACCOUNT INFORMATION",
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
                        // Nickname Input
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Account name", color = textSecondary.copy(alpha = 0.5f), fontSize = 15.sp) },
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

                        // Provider Picker Row
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showProviderMenu = true }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Provider", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = providerIcon(selectedProvider),
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = selectedProvider,
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
                                expanded = showProviderMenu,
                                onDismissRequest = { showProviderMenu = false },
                                offset = DpOffset(0.dp, 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = menuBg,
                                shadowElevation = 8.dp,
                                border = menuBorder
                            ) {
                                providers.forEach { prov ->
                                    val isSelected = selectedProvider == prov
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = providerIcon(prov),
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = prov,
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
                                            selectedProvider = prov
                                            showProviderMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        // Account Type Picker
                        Column(modifier = Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Account Type", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)

                            com.centwise.core.design.components.CentwiseSegmentedControl(
                                items = accountTypes,
                                selectedItem = selectedType,
                                onItemSelected = { selectedType = it },
                                itemLabel = { it },
                                modifier = Modifier.height(34.dp),
                                accent = accent,
                                isDark = isDark
                            )
                        }
                    }
                }
            }

            // 3. Details Section Card
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
                        TextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            placeholder = { Text("Last 4 digits (optional)", color = textSecondary.copy(alpha = 0.5f), fontSize = 15.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Balance ৳", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(
                                value = balanceText,
                                onValueChange = { balanceText = it },
                                placeholder = { Text("0.00", color = textSecondary.copy(alpha = 0.5f), fontSize = 15.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                ),
                                modifier = Modifier.weight(1f),
                                textStyle = CentwiseTypography.Body.copy(
                                    fontSize = 15.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditAccountSheetPreview() {
    AddEditAccountSheet(onDismiss = {}, onSave = {})
}
