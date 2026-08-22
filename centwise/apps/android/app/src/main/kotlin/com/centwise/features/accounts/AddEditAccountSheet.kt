package com.centwise.features.accounts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.AccountItem
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

private val accountTypes = listOf("MFS Wallet", "Bank Account", "Card", "Cash")
private val providers = listOf("bKash", "Nagad", "Rocket", "Upay", "BRAC Bank", "City Bank", "Dutch-Bangla Bank", "Cash", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountSheet(
    onDismiss: () -> Unit,
    onSave: (AccountItem) -> Unit,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var name by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(accountTypes.first()) }
    var selectedProvider by remember { mutableStateOf(providers.first()) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val sheetBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary

    val isValid = name.isNotBlank() &&
            (balanceText.isBlank() || balanceText.toDoubleOrNull() != null)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add Account", style = CentwiseTypography.Title2, color = textPrimary)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account name (e.g. My bKash)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("Account / wallet number (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = balanceText,
                onValueChange = { balanceText = it },
                label = { Text("Starting balance (৳)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Account Type", style = CentwiseTypography.Headline, color = textPrimary)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                accountTypes.take(2).forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                accountTypes.drop(2).forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("Provider", style = CentwiseTypography.Headline, color = textPrimary)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                providers.chunked(3).forEach { rowProviders ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowProviders.forEach { provider ->
                            FilterChip(
                                selected = selectedProvider == provider,
                                onClick = { selectedProvider = provider },
                                label = { Text(provider, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowProviders.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            AccountItem(
                                name = name.trim(),
                                type = selectedType,
                                balance = balanceText.toDoubleOrNull() ?: 0.0,
                                providerName = selectedProvider,
                                accountNumber = accountNumber.trim().ifBlank { "0000" }
                            )
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Save")
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
