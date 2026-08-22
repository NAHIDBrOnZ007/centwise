package com.centwise.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.CategoryOption

private val categoryColorChoices = listOf(
    0xFF00A86B, 0xFFF97316, 0xFF06B6D4, 0xFFEC4899, 0xFFEAB308,
    0xFF8B5CF6, 0xFF10B981, 0xFF007AFF, 0xFFEF4444, 0xFF6366F1,
    0xFF14B8A6, 0xFF64748B
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategorySheet(
    editingCategory: CategoryOption? = null,
    onDismiss: () -> Unit,
    onSave: (CategoryOption) -> Unit,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var name by remember { mutableStateOf(editingCategory?.name ?: "") }
    var selectedColorHex by remember { mutableStateOf(editingCategory?.colorHex ?: 0xFF00A86B) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val sheetBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val isValid = name.isNotBlank()

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = if (editingCategory == null) "New Category" else "Edit Category",
                style = CentwiseTypography.Title2,
                color = textPrimary
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Color", style = CentwiseTypography.Headline, color = textPrimary)

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categoryColorChoices) { hex ->
                    val color = Color(hex)
                    val isSelected = hex == selectedColorHex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) textPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }

            // Preview
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(selectedColorHex).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.trim().take(1).ifEmpty { "C" },
                        style = CentwiseTypography.Body,
                        color = Color(selectedColorHex)
                    )
                }
                Text(
                    text = name.ifBlank { "Category name" },
                    style = CentwiseTypography.Body,
                    color = if (name.isBlank()) textSecondary else textPrimary
                )
            }

            // Actions
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
                            CategoryOption(
                                id = editingCategory?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                icon = editingCategory?.icon ?: "category",
                                colorHex = selectedColorHex
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
fun AddEditCategorySheetPreview() {
    AddEditCategorySheet(onDismiss = {}, onSave = {})
}
