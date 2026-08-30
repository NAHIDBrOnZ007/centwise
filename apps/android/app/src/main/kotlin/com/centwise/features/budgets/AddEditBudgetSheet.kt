package com.centwise.features.budgets

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
import androidx.compose.foundation.shape.CircleShape
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
import com.centwise.core.design.components.CategoryIconHelper
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.BudgetItem
import com.centwise.data.repository.TransactionRepository
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

private val budgetPeriods = listOf("Monthly", "Weekly", "Yearly")

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
    var selectedPeriod by remember { mutableStateOf(editingBudget?.period ?: "Monthly") }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val categories by TransactionRepository.shared.categories.collectAsState()
    LaunchedEffect(categories, editingBudget) {
        if (editingBudget != null) {
            selectedCategoryName = editingBudget.categoryName
        } else if (selectedCategoryName.isEmpty()) {
            selectedCategoryName = categories.firstOrNull()?.name ?: "Food & Dining"
        }
    }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val menuBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val menuBorder = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x0F000000))

    val parsedLimit = limitText.toDoubleOrNull() ?: 0.0
    val isValid = parsedLimit > 0 && selectedCategoryName.isNotBlank()

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
                title = if (editingBudget == null) "New Budget" else "Edit Budget",
                onCancel = { dismissWithAnimation {} },
                onSave = {
                    if (isValid) {
                        val newBudget = editingBudget?.copy(
                            categoryName = selectedCategoryName,
                            allocatedAmount = parsedLimit,
                            period = selectedPeriod
                        ) ?: BudgetItem(
                            categoryName = selectedCategoryName,
                            allocatedAmount = parsedLimit,
                            spentAmount = 0.0,
                            period = selectedPeriod
                        )
                        dismissWithAnimation {
                            onSave(newBudget)
                        }
                    }
                },
                saveEnabled = isValid,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDark = isDark
            )

            // 2. Budget Info Form Card
            Column {
                Text(
                    text = "BUDGET INFO",
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
                        // Category Picker Row
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCategoryMenu = true }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Category", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.iconFor(selectedCategoryName),
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = selectedCategoryName,
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
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false },
                                offset = DpOffset(0.dp, 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = menuBg,
                                shadowElevation = 8.dp,
                                border = menuBorder
                            ) {
                                categories.forEach { cat ->
                                    val isSelected = selectedCategoryName == cat.name
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = CategoryIconHelper.iconFor(cat.name, cat.icon),
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = cat.name,
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
                                            selectedCategoryName = cat.name
                                            showCategoryMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        // Limit Input
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monthly limit (৳)", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(
                                value = limitText,
                                onValueChange = { limitText = it },
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

            // 3. Period Segmented Control
            Column {
                Text(
                    text = "PERIOD",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                com.centwise.core.design.components.CentwiseSegmentedControl(
                    items = budgetPeriods,
                    selectedItem = selectedPeriod,
                    onItemSelected = { selectedPeriod = it },
                    itemLabel = { it },
                    modifier = Modifier.height(34.dp),
                    accent = accent,
                    isDark = isDark
                )
            }

            // 4. Preview Section Card
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
                                .background(accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.iconFor(selectedCategoryName),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = selectedCategoryName,
                                style = CentwiseTypography.Body,
                                color = textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$selectedPeriod limit: ${CurrencyFormatter.formatBDT(parsedLimit)}",
                                style = CentwiseTypography.Caption,
                                color = textSecondary
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
fun AddEditBudgetSheetPreview() {
    AddEditBudgetSheet(onDismiss = {}, onSave = {})
}
