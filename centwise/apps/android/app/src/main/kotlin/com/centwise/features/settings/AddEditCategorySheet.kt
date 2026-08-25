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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.CategoryOption

private val categoryColorChoices = listOf(
    0xFF059669, 0xFF00A86B, 0xFFF97316, 0xFF06B6D4, 0xFFEC4899, 0xFFEAB308,
    0xFF8B5CF6, 0xFF10B981, 0xFF007AFF, 0xFFEF4444, 0xFF6366F1, 0xFF14B8A6,
    0xFF64748B, 0xFFD97706, 0xFF2563EB, 0xFF7C3AED, 0xFFDB2777, 0xFF475569
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
    var selectedColorHex by remember { mutableStateOf(editingCategory?.colorHex ?: 0xFF059669) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val sheetBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val fieldBg = if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000)

    val isValid = name.isNotBlank()
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = if (editingCategory == null) "New Category" else "Edit Category",
                style = CentwiseTypography.Title2,
                color = textPrimary
            )

            // Category Name Input
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Category Name (e.g. Health & Fitness, Rent)", color = textSecondary.copy(alpha = 0.6f)) },
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

            // Color Swatches
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category Color Accent", style = CentwiseTypography.Headline.copy(fontSize = 13.sp), color = textSecondary)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categoryColorChoices) { hex ->
                        val color = Color(hex)
                        val isSelected = hex == selectedColorHex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorHex = hex }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) textPrimary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Save Button
            Button(
                onClick = {
                    if (isValid) {
                        onSave(
                            editingCategory?.copy(name = name.trim(), colorHex = selectedColorHex)
                                ?: CategoryOption(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    icon = "tag",
                                    colorHex = selectedColorHex
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
                    text = if (editingCategory == null) "Save Category" else "Update Category",
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditCategorySheetPreview() {
    AddEditCategorySheet(onDismiss = {}, onSave = {})
}
