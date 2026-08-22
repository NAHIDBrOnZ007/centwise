package com.centwise.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.SmartRule

@Composable
fun RulesScreen(
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    var rules by remember {
        mutableStateOf(
            listOf(
                SmartRule(name = "Foodpanda is Food", keyword = "Foodpanda", categoryName = "Food & Dining"),
                SmartRule(name = "Pathao is Transport", keyword = "Pathao", categoryName = "Transport"),
                SmartRule(name = "Daraz is Shopping", keyword = "Daraz", categoryName = "Shopping", isEnabled = false)
            )
        )
    }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<SmartRule?>(null) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = textPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onBackClick() }
                    .padding(10.dp)
            )
            Text(text = "Smart Rules", style = CentwiseTypography.Headline, color = textPrimary)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Add",
                style = CentwiseTypography.Body,
                color = accent,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showAddSheet = true }
                    .padding(10.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (rules.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "No rules yet",
                            style = CentwiseTypography.Headline,
                            color = textPrimary
                        )
                        Text(
                            "Rules auto-categorize transactions when the merchant name matches a keyword.",
                            style = CentwiseTypography.Caption,
                            color = textSecondary
                        )
                        Button(
                            onClick = { showAddSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Rule")
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "${rules.size} rule${if (rules.size == 1) "" else "s"}",
                        style = CentwiseTypography.Caption,
                        color = textSecondary
                    )
                }

                items(rules) { rule ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                            .clickable { editingRule = rule }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.name, style = CentwiseTypography.Body, color = textPrimary)
                                Text(rule.summary, style = CentwiseTypography.Caption, color = textSecondary)
                            }
                            Switch(
                                checked = rule.isEnabled,
                                onCheckedChange = { enabled ->
                                    rules = rules.map {
                                        if (it.id == rule.id) it.copy(isEnabled = enabled) else it
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = accent)
                            )
                        }

                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                rule.transactionType.displayName,
                                style = CentwiseTypography.Caption,
                                color = if (rule.transactionType == com.centwise.data.models.TransactionType.INCOME)
                                    CentwiseColors.IncomeGreen else CentwiseColors.ExpenseRed
                            )
                            IconButton(onClick = { editingRule = rule }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = CentwiseColors.ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditRuleSheet(
            onDismiss = { showAddSheet = false },
            onSave = { rule ->
                rules = listOf(rule) + rules
                showAddSheet = false
            }
        )
    }

    editingRule?.let { rule ->
        AddEditRuleSheet(
            editingRule = rule,
            onDismiss = { editingRule = null },
            onSave = { updated ->
                rules = rules.map { if (it.id == updated.id) updated else it }
                editingRule = null
            },
            onDelete = {
                rules = rules.filter { it.id != rule.id }
                editingRule = null
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RulesScreenPreview() {
    RulesScreen()
}
