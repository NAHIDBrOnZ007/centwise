package com.centwise.features.settings

import kotlinx.coroutines.launch
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
import com.centwise.core.design.components.CategoryIconHelper
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.CategoryOption

private val iconChoices = listOf(
    "fork.knife", "car.fill", "bag.fill", "bolt.fill", "antenna.radiowaves.left.and.right",
    "banknote.fill", "arrow.left.arrow.right", "cross.case.fill", "play.tv.fill",
    "book.closed.fill", "airplane", "house.fill", "cart.fill", "gift.fill",
    "creditcard.fill", "fuelpump.fill", "tshirt.fill", "tag.fill"
)

private val colorChoices = listOf(
    0xFF00A86B, 0xFFF7941D, 0xFFE2136E, 0xFF8C3494, 0xFF06B6D4, 0xFF6366F1,
    0xFFA855F7, 0xFFE11D48, 0xFF10B981, 0xFF3B82F6, 0xFFF59E0B, 0xFFFB7185,
    0xFF14B8A6, 0xFF84CC16, 0xFFF472B6, 0xFF7C3AED, 0xFF64748B, 0xFF334155
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategorySheet(
    editingCategory: CategoryOption? = null,
    onDismiss: () -> Unit,
    onSave: (CategoryOption) -> Unit,
    onDelete: (() -> Unit)? = null,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var name by remember { mutableStateOf(editingCategory?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(editingCategory?.icon ?: "tag.fill") }
    var selectedColorHex by remember {
        mutableStateOf(editingCategory?.colorHex ?: colorChoices.first())
    }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val isValid = name.trim().isNotBlank()
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
                title = if (editingCategory == null) "New Category" else "Edit Category",
                onCancel = { dismissWithAnimation {} },
                onSave = {
                    if (isValid) {
                        val newCat = CategoryOption(
                            id = editingCategory?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name.trim(),
                            icon = selectedIcon,
                            colorHex = selectedColorHex
                        )
                        dismissWithAnimation {
                            onSave(newCat)
                        }
                    }
                },
                saveEnabled = isValid,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDark = isDark
            )

            // 2. Name Section Card
            Column {
                Text(
                    text = "NAME",
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
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Category name", color = textSecondary.copy(alpha = 0.5f), fontSize = 15.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        textStyle = CentwiseTypography.Body.copy(fontSize = 15.sp)
                    )
                }
            }

            // 3. Icon Section Card
            Column {
                Text(
                    text = "ICON",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    shadowElevation = if (isDark) 4.dp else 1.dp
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(iconChoices) { iconName ->
                            val isSelected = selectedIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) accent else if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                                    .clickable { selectedIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.iconFor(iconName, iconName),
                                    contentDescription = iconName,
                                    tint = if (isSelected) Color.White else textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Color Section Card
            Column {
                Text(
                    text = "COLOR",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    shadowElevation = if (isDark) 4.dp else 1.dp
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(colorChoices) { hex ->
                            val color = Color(hex)
                            val isSelected = hex == selectedColorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Preview Section Card
            Column {
                Text(
                    text = "PREVIEW",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    shadowElevation = if (isDark) 4.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(selectedColorHex).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.iconFor(selectedIcon, selectedIcon),
                                contentDescription = null,
                                tint = Color(selectedColorHex),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = if (name.isBlank()) "Category name" else name,
                            style = CentwiseTypography.Headline,
                            color = if (name.isBlank()) textSecondary else textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Destructive Delete Button
            if (onDelete != null && editingCategory != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDelete()
                            onDismiss()
                        }
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "Delete Category",
                        style = CentwiseTypography.Headline,
                        fontWeight = FontWeight.SemiBold,
                        color = CentwiseColors.ExpenseRed,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                    )
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
