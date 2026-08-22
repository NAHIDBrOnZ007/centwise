package com.centwise.features.budgets

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
import com.centwise.data.models.BudgetItem
import com.centwise.data.models.CategoryOption
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetSheet(
    editingBudget: BudgetItem? = null,
    onDismiss: () -> Unit,
    onSave: (BudgetItem) -> Unit,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var selectedCategoryName by remember {
        mutableStateOf(editingBudget?.categoryName ?: CategoryOption.defaults.first().name)
    }
    var limitText by remember {
        mutableStateOf(editingBudget?.allocatedAmount?.let { "%.0f".format(it) } ?: "")
    }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val sheetBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val isValid = limitText.toDoubleOrNull() != null && limitText.toDoubleOrNull()!! > 0

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
                text = if (editingBudget == null) "New Budget" else "Edit Budget",
                style = CentwiseTypography.Title2,
                color = textPrimary
            )

            Text("Category", style = CentwiseTypography.Headline, color = textPrimary)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryOption.defaults.chunked(2).forEach { rowCategories ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowCategories.forEach { category ->
                            FilterChip(
                                selected = category.name == selectedCategoryName,
                                onClick = { selectedCategoryName = category.name },
                                label = { Text(category.name, style = CentwiseTypography.Caption) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowCategories.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            OutlinedTextField(
                value = limitText,
                onValueChange = { limitText = it },
                label = { Text("Monthly limit (৳)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Spending in this category is tracked against the limit automatically.",
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
                        val limit = limitText.toDoubleOrNull() ?: return@Button
                        val spent = editingBudget?.spentAmount ?: 0.0
                        onSave(
                            BudgetItem(
                                id = editingBudget?.id ?: java.util.UUID.randomUUID().toString(),
                                categoryName = selectedCategoryName,
                                allocatedAmount = limit,
                                spentAmount = spent,
                                progress = if (limit > 0) (spent / limit).toFloat() else 0f
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
fun AddEditBudgetSheetPreview() {
    AddEditBudgetSheet(onDismiss = {}, onSave = {})
}
