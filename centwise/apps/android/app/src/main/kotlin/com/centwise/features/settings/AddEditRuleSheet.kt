package com.centwise.features.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.CategoryOption
import com.centwise.data.models.RuleMatchType
import com.centwise.data.models.SmartRule
import com.centwise.data.models.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleSheet(
    editingRule: SmartRule? = null,
    onDismiss: () -> Unit,
    onSave: (SmartRule) -> Unit,
    onDelete: (() -> Unit)? = null,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var name by remember { mutableStateOf(editingRule?.name ?: "") }
    var keyword by remember { mutableStateOf(editingRule?.keyword ?: "") }
    var matchType by remember { mutableStateOf(editingRule?.matchType ?: RuleMatchType.CONTAINS) }
    var categoryName by remember { mutableStateOf(editingRule?.categoryName ?: CategoryOption.defaults.first().name) }
    var selectedTransactionType by remember { mutableStateOf(editingRule?.transactionType ?: TransactionType.EXPENSE) }
    var isEnabled by remember { mutableStateOf(editingRule?.isEnabled ?: true) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val sheetBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val isValid = name.isNotBlank() && keyword.isNotBlank()

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
            Text(
                text = if (editingRule == null) "New Rule" else "Edit Rule",
                style = CentwiseTypography.Title2,
                color = textPrimary
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Rule name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("Keyword (e.g. Foodpanda, Pathao)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Match Condition", style = CentwiseTypography.Headline, color = textPrimary)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RuleMatchType.entries.forEach { type ->
                    val isSelected = type == matchType
                    FilterChip(
                        selected = isSelected,
                        onClick = { matchType = type },
                        label = { Text(type.displayName, style = CentwiseTypography.Caption) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("Assign To", style = CentwiseTypography.Headline, color = textPrimary)

            // Category selector as chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryOption.defaults.chunked(2).forEach { rowCategories ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowCategories.forEach { category ->
                            val isSelected = category.name == categoryName
                            FilterChip(
                                selected = isSelected,
                                onClick = { categoryName = category.name },
                                label = { Text(category.name, style = CentwiseTypography.Caption) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowCategories.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Text("Transaction Type", style = CentwiseTypography.Headline, color = textPrimary)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                    val isSelected = type == selectedTransactionType
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTransactionType = type },
                        label = { Text(type.displayName, style = CentwiseTypography.Caption) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("Rule Enabled", style = CentwiseTypography.Body, color = textPrimary)
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = accent)
                )
            }

            Text(
                "Disabled rules are kept but no longer applied to new transactions.",
                style = CentwiseTypography.Caption,
                color = textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CentwiseColors.ExpenseRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete")
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            SmartRule(
                                id = editingRule?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                keyword = keyword.trim(),
                                matchType = matchType,
                                categoryName = categoryName,
                                transactionType = selectedTransactionType,
                                isEnabled = isEnabled
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
fun AddEditRuleSheetPreview() {
    AddEditRuleSheet(onDismiss = {}, onSave = {})
}
