package com.centwise.features.subscriptions

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
import com.centwise.data.models.SubscriptionItem
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val billingCycles = listOf("Weekly", "Monthly", "Quarterly", "Yearly")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionSheet(
    onDismiss: () -> Unit,
    onSave: (SubscriptionItem) -> Unit,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var billingCycle by remember { mutableStateOf("Monthly") }
    var dueDateMillis by remember {
        mutableStateOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)
    }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val sheetBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val isValid = name.isNotBlank() && amountText.toDoubleOrNull() != null

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
            Text("New Subscription", style = CentwiseTypography.Title2, color = textPrimary)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Service name (e.g. Netflix)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (৳)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Billing Cycle", style = CentwiseTypography.Headline, color = textPrimary)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                billingCycles.take(2).forEach { cycle ->
                    FilterChip(
                        selected = billingCycle == cycle,
                        onClick = { billingCycle = cycle },
                        label = { Text(cycle) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                billingCycles.drop(2).forEach { cycle ->
                    FilterChip(
                        selected = billingCycle == cycle,
                        onClick = { billingCycle = cycle },
                        label = { Text(cycle) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("Next Due Date", style = CentwiseTypography.Headline, color = textPrimary)

            TextButton(
                onClick = { dueDateMillis = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000 },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(dueDateMillis)),
                    color = accent
                )
            }
            Text(
                "Full date picker connects to the shared date selection in the next update.",
                style = CentwiseTypography.Caption,
                color = textSecondary
            )

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
                            SubscriptionItem(
                                name = name.trim(),
                                amount = amountText.toDoubleOrNull() ?: 0.0,
                                billingCycle = billingCycle,
                                nextBillingDate = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                                    .format(Date(dueDateMillis))
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
fun AddEditSubscriptionSheetPreview() {
    AddEditSubscriptionSheet(onDismiss = {}, onSave = {})
}
