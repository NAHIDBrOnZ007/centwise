package com.centwise.features.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.BudgetItem
import com.centwise.data.repository.TransactionRepository
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
        mutableStateOf(editingBudget?.categoryName ?: "")
    }
    var limitText by remember {
        mutableStateOf(
            editingBudget?.allocatedAmount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }

    val categories by TransactionRepository.shared.categories.collectAsState()
    LaunchedEffect(categories, editingBudget) {
        if (editingBudget != null) {
            selectedCategoryName = editingBudget.categoryName
        } else if (selectedCategoryName.isEmpty()) {
            selectedCategoryName = categories.firstOrNull()?.name.orEmpty()
        }
    }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val sheetBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val fieldBg = if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000)

    val isValid = limitText.toDoubleOrNull() != null && limitText.toDoubleOrNull()!! > 0
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
            Text(
                text = if (editingBudget == null) "New Budget" else "Edit Budget",
                style = CentwiseTypography.Title2,
                color = textPrimary
            )

            // 1. Hero Limit Input Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(fieldBg)
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MONTHLY LIMIT (BDT)",
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
                        value = limitText,
                        onValueChange = { limitText = it },
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
                            if (limitText.isEmpty()) {
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

            // 2. Category Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Target Category", style = CentwiseTypography.Headline.copy(fontSize = 13.sp), color = textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategoryName == cat.name
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) accent else fieldBg,
                            modifier = Modifier.clickable { selectedCategoryName = cat.name }
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

            Text(
                "Spending in this category will be tracked against this limit automatically in real-time.",
                style = CentwiseTypography.Caption,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Save Button
            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 0.0
                    if (limit > 0) {
                        onSave(
                            editingBudget?.copy(
                                categoryName = selectedCategoryName,
                                allocatedAmount = limit
                            ) ?: BudgetItem(
                                categoryName = selectedCategoryName,
                                allocatedAmount = limit,
                                spentAmount = 0.0
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
                    text = if (editingBudget == null) "Set Budget" else "Update Budget",
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditBudgetSheetPreview() {
    AddEditBudgetSheet(onDismiss = {}, onSave = {})
}
