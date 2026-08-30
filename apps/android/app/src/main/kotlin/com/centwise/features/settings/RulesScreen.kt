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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.SmartRule
import com.centwise.data.repository.SmartRulesRepository
import com.centwise.data.repository.TransactionRepository

@Composable
fun RulesScreen(
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val repository = SmartRulesRepository.shared
    val rules by repository.rules.collectAsState()
    val categories by TransactionRepository.shared.categories.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<SmartRule?>(null) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val searchBg = if (isDark) CentwiseColors.DarkSearchBg else CentwiseColors.LightSearchBg

    val filteredRules = remember(rules, searchText) {
        val query = searchText.trim().lowercase()
        if (query.isEmpty()) rules
        else rules.filter {
            it.name.lowercase().contains(query) ||
            it.keyword.lowercase().contains(query) ||
            it.categoryName.lowercase().contains(query)
        }
    }

    // Group rules by category
    val groupedRules = remember(filteredRules, categories) {
        val catMap = categories.associateBy { it.name.lowercase() }
        filteredRules.groupBy { it.categoryName }.toList()
    }

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
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary
                )
            }
            Text(
                text = "Smart Rules",
                style = CentwiseTypography.Headline,
                color = textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Menu",
                        tint = accent
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("New Rule") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showAddSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Restore Default Rules") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            repository.refresh()
                        }
                    )
                }
            }
        }

        // Search bar
        if (rules.isNotEmpty()) {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = {
                    Text(
                        text = "Search rules",
                        style = CentwiseTypography.Body,
                        color = textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = searchBg,
                    unfocusedContainerColor = searchBg,
                    disabledContainerColor = searchBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (rules.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "No rules yet",
                            style = CentwiseTypography.Headline,
                            color = textPrimary
                        )
                        Text(
                            "Rules auto-categorize transactions when the SMS merchant name matches a keyword.",
                            style = CentwiseTypography.Caption,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Button(
                            onClick = { showAddSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Create Rule")
                        }
                    }
                }
            } else if (filteredRules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No rules found",
                            style = CentwiseTypography.Subheadline,
                            color = textSecondary
                        )
                    }
                }
            } else {
                groupedRules.forEach { (categoryName, rulesInSection) ->
                    item {
                        Text(
                            text = categoryName.uppercase(),
                            style = CentwiseTypography.Caption.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = textSecondary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                                .background(cardBg)
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            rulesInSection.forEachIndexed { index, rule ->
                                SmartRuleRow(
                                    rule = rule,
                                    accent = accent,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onToggle = { enabled -> repository.toggleRule(rule.id, enabled) },
                                    onClick = { editingRule = rule }
                                )
                                if (index < rulesInSection.lastIndex) {
                                    HorizontalDivider(
                                        color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)
                                    )
                                }
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
                repository.addRule(rule)
                showAddSheet = false
            }
        )
    }

    editingRule?.let { rule ->
        AddEditRuleSheet(
            editingRule = rule,
            onDismiss = { editingRule = null },
            onSave = { updated ->
                repository.updateRule(updated)
                editingRule = null
            },
            onDelete = {
                repository.deleteRule(rule.id)
                editingRule = null
            }
        )
    }
}

@Composable
private fun SmartRuleRow(
    rule: SmartRule,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Tag,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.name,
                style = CentwiseTypography.Body.copy(fontWeight = FontWeight.SemiBold),
                color = textPrimary
            )
            Text(
                text = "${rule.categoryName} • Keyword: ${rule.keyword}",
                style = CentwiseTypography.Caption,
                color = textSecondary
            )
        }

        Switch(
            checked = rule.isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = accent,
                checkedThumbColor = Color.White
            )
        )
    }
}
