package com.centwise.features.settings

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.CategoryIconHelper
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.RuleMatchType
import com.centwise.data.models.SmartRule
import com.centwise.data.models.TransactionType
import com.centwise.data.repository.TransactionRepository

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
    var categoryName by remember { mutableStateOf(editingRule?.categoryName ?: "") }
    var selectedTransactionType by remember { mutableStateOf(editingRule?.transactionType ?: TransactionType.EXPENSE) }
    var isEnabled by remember { mutableStateOf(editingRule?.isEnabled ?: true) }

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    val categories by TransactionRepository.shared.categories.collectAsState()
    LaunchedEffect(categories, editingRule) {
        if (editingRule != null) {
            categoryName = editingRule.categoryName
        } else if (categoryName.isEmpty()) {
            categoryName = categories.firstOrNull()?.name ?: "Food & Dining"
        }
    }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val menuBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val menuBorder = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x0F000000))

    val isValid = name.trim().isNotBlank() && keyword.trim().isNotBlank()
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
                title = if (editingRule == null) "New Rule" else "Edit Rule",
                onCancel = { dismissWithAnimation {} },
                onSave = {
                    if (isValid) {
                        val newRule = editingRule?.copy(
                            name = name.trim(),
                            keyword = keyword.trim(),
                            matchType = matchType,
                            categoryName = categoryName,
                            transactionType = selectedTransactionType,
                            isEnabled = isEnabled
                        ) ?: SmartRule(
                            name = name.trim(),
                            keyword = keyword.trim(),
                            matchType = matchType,
                            categoryName = categoryName,
                            transactionType = selectedTransactionType,
                            isEnabled = isEnabled
                        )
                        dismissWithAnimation {
                            onSave(newRule)
                        }
                    }
                },
                saveEnabled = isValid,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDark = isDark
            )

            // 2. Rule Info Section Card
            Column {
                Text(
                    text = "RULE INFO",
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
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Rule name", color = textSecondary.copy(alpha = 0.5f), fontSize = 15.sp) },
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

                        TextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            placeholder = { Text("Keyword", color = textSecondary.copy(alpha = 0.5f), fontSize = 15.sp) },
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
                    }
                }
            }

            // 3. Match Condition Section Card
            Column {
                Text(
                    text = "MATCH CONDITION",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                com.centwise.core.design.components.CentwiseSegmentedControl(
                    items = RuleMatchType.entries,
                    selectedItem = matchType,
                    onItemSelected = { matchType = it },
                    itemLabel = { it.displayName },
                    modifier = Modifier.height(34.dp),
                    accent = accent,
                    isDark = isDark
                )
            }

            // 4. Assign To Form Card (Category & Transaction Type)
            Column {
                Text(
                    text = "ASSIGN TO",
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
                                        imageVector = CategoryIconHelper.iconFor(categoryName),
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = categoryName,
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
                                    val isSelected = categoryName == cat.name
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
                                            categoryName = cat.name
                                            showCategoryMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        // Transaction Type Picker Row
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTypeMenu = true }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Transaction Type", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = selectedTransactionType.displayName,
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
                                expanded = showTypeMenu,
                                onDismissRequest = { showTypeMenu = false },
                                offset = DpOffset(0.dp, 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = menuBg,
                                shadowElevation = 8.dp,
                                border = menuBorder
                            ) {
                                TransactionType.entries.forEach { type ->
                                    val isSelected = selectedTransactionType == type
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = type.displayName,
                                                style = CentwiseTypography.Body,
                                                color = textPrimary,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
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
                                            selectedTransactionType = type
                                            showTypeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Rule Enabled Toggle Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                shadowElevation = if (isDark) 4.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rule Enabled", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = accent,
                            checkedThumbColor = Color.White
                        )
                    )
                }
            }

            // Destructive Delete Button
            if (onDelete != null && editingRule != null) {
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
                        text = "Delete Rule",
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
fun AddEditRuleSheetPreview() {
    AddEditRuleSheet(onDismiss = {}, onSave = {})
}
