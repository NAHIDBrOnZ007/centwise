package com.centwise.features.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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

    val categories by TransactionRepository.shared.categories.collectAsState()
    LaunchedEffect(categories, editingRule) {
        if (editingRule != null) {
            categoryName = editingRule.categoryName
        } else if (categoryName.isEmpty()) {
            categoryName = categories.firstOrNull()?.name.orEmpty()
        }
    }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val sheetBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val fieldBg = if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000)

    val isValid = name.isNotBlank() && keyword.isNotBlank()
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingRule == null) "New Rule" else "Edit Rule",
                    style = CentwiseTypography.Title2,
                    color = textPrimary
                )

                if (editingRule != null && onDelete != null) {
                    IconButton(onClick = {
                        onDelete()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CentwiseColors.ExpenseRed)
                    }
                }
            }

            // 1. Rule Name Input
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Rule Name (e.g. Foodpanda Food Override)", color = textSecondary.copy(alpha = 0.6f)) },
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

            // 2. Keyword Input
            TextField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = { Text("SMS Keyword (e.g. Foodpanda, Pathao, Netflix)", color = textSecondary.copy(alpha = 0.6f)) },
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

            // 3. Match Condition Segmented Toggle
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Match Condition", style = CentwiseTypography.Headline.copy(fontSize = 13.sp), color = textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(fieldBg)
                        .padding(4.dp)
                ) {
                    RuleMatchType.entries.forEach { type ->
                        val isSelected = matchType == type
                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) accent else Color.Transparent,
                            animationSpec = spring(),
                            label = "match_pill_bg"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(pillBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { matchType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.displayName,
                                style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                                color = if (isSelected) Color.White else textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 4. Category Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Assign Category", style = CentwiseTypography.Headline.copy(fontSize = 13.sp), color = textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = categoryName == cat.name
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) accent else fieldBg,
                            modifier = Modifier.clickable { categoryName = cat.name }
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

            Spacer(modifier = Modifier.height(4.dp))

            // 5. Save Button
            Button(
                onClick = {
                    if (isValid) {
                        onSave(
                            editingRule?.copy(
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
                    text = if (editingRule == null) "Save Rule" else "Update Rule",
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditRuleSheetPreview() {
    AddEditRuleSheet(onDismiss = {}, onSave = {})
}
