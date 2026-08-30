package com.centwise.features.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.centwise.core.design.components.ActionPillGroup
import com.centwise.core.design.components.CategoryIconHelper
import com.centwise.core.design.components.EmptyStateView
import com.centwise.core.design.components.TransactionRow
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.data.repository.TransactionRepository
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

@Composable
fun TransactionListScreen(
    onAddClick: () -> Unit = {},
    viewModel: TransactionsViewModel = viewModel(),
    isDark: Boolean = isSystemInDarkTheme()
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalNet by viewModel.totalNet.collectAsState()

    val categories by TransactionRepository.shared.categories.collectAsState()

    var selectedTransaction by remember { mutableStateOf<TransactionItem?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }
    var showPeriodMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val searchBg = if (isDark) CentwiseColors.DarkSearchBg else CentwiseColors.LightSearchBg
    val menuBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val menuBorder = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x0F000000))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title + Action Pill (+, ⇅, 📤)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        style = CentwiseTypography.LargeTitle,
                        color = textPrimary
                    )
                    ActionPillGroup(
                        onAddClick = onAddClick,
                        currentSortOrder = sortOrder,
                        onSortSelected = { viewModel.setSortOrder(it) },
                        onExportClick = { CsvExporter.shareExport(context) },
                        isDark = isDark
                    )
                }
            }

            // Search Bar Capsule
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearch(it) },
                    placeholder = {
                        Text(
                            text = "Search transactions",
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
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = searchBg,
                        unfocusedContainerColor = searchBg,
                        disabledContainerColor = searchBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    singleLine = true
                )
            }

            // Filter Chips (Period, Type, Category) - Each anchored inside its own Box with gap & iOS styled Dropdown
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Period Filter Box & Menu
                    Box(modifier = Modifier.weight(1f)) {
                        FilterButton(
                            icon = Icons.Default.CalendarToday,
                            text = selectedPeriod,
                            isActive = selectedPeriod != "All Time",
                            onClick = { showPeriodMenu = true },
                            accent = accent,
                            isDark = isDark
                        )

                        DropdownMenu(
                            expanded = showPeriodMenu,
                            onDismissRequest = { showPeriodMenu = false },
                            offset = DpOffset(0.dp, 8.dp),
                            shape = RoundedCornerShape(18.dp),
                            containerColor = menuBg,
                            shadowElevation = 8.dp,
                            border = menuBorder,
                            modifier = Modifier.heightIn(max = 440.dp)
                        ) {
                            listOf("This Month", "Last Month", "This Year", "All Time").forEach { p ->
                                val isSelected = selectedPeriod == p
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = p,
                                            style = CentwiseTypography.Body,
                                            color = textPrimary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 14.sp
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
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    onClick = {
                                        viewModel.setPeriod(p)
                                        showPeriodMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Type Filter Box & Menu
                    Box(modifier = Modifier.weight(1f)) {
                        FilterButton(
                            icon = Icons.Default.Menu,
                            text = selectedType?.displayName ?: "All Types",
                            isActive = selectedType != null,
                            onClick = { showTypeMenu = true },
                            accent = accent,
                            isDark = isDark
                        )

                        DropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false },
                            offset = DpOffset(0.dp, 8.dp),
                            shape = RoundedCornerShape(18.dp),
                            containerColor = menuBg,
                            shadowElevation = 8.dp,
                            border = menuBorder,
                            modifier = Modifier.heightIn(max = 440.dp)
                        ) {
                            val isAllSelected = selectedType == null
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        text = "All Types",
                                        style = CentwiseTypography.Body,
                                        color = textPrimary,
                                        fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                },
                                trailingIcon = {
                                    if (isAllSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                onClick = {
                                    viewModel.setTypeFilter(null)
                                    showTypeMenu = false
                                }
                            )
                            TransactionType.entries.forEach { type ->
                                val isSelected = selectedType == type
                                val typeIcon = when (type) {
                                    TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.ArrowForward
                                    TransactionType.INCOME, TransactionType.CREDIT -> Icons.Default.ArrowDownward
                                    else -> Icons.Default.Menu
                                }
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = typeIcon,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = type.displayName,
                                            style = CentwiseTypography.Body,
                                            color = textPrimary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 14.sp
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
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    onClick = {
                                        viewModel.setTypeFilter(type)
                                        showTypeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. Category Filter Box & Menu
                    Box(modifier = Modifier.weight(1f)) {
                        FilterButton(
                            icon = Icons.Default.Tune,
                            text = selectedCategory ?: "Category",
                            isActive = selectedCategory != null,
                            onClick = { showCategoryMenu = true },
                            accent = accent,
                            isDark = isDark
                        )

                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false },
                            offset = DpOffset(0.dp, 8.dp),
                            shape = RoundedCornerShape(18.dp),
                            containerColor = menuBg,
                            shadowElevation = 8.dp,
                            border = menuBorder,
                            modifier = Modifier.heightIn(max = 440.dp)
                        ) {
                            val isAllSelected = selectedCategory == null
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        text = "All Categories",
                                        style = CentwiseTypography.Body,
                                        color = textPrimary,
                                        fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                },
                                trailingIcon = {
                                    if (isAllSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                onClick = {
                                    viewModel.setCategoryFilter(null)
                                    showCategoryMenu = false
                                }
                            )
                            categories.forEach { cat ->
                                val isSelected = selectedCategory == cat.name
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = CategoryIconHelper.iconFor(cat.name),
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = cat.name,
                                            style = CentwiseTypography.Body,
                                            color = textPrimary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 14.sp
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
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    onClick = {
                                        viewModel.setCategoryFilter(cat.name)
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Totals Summary Card (3 Columns: Income, Expenses, Net)
            if (transactions.isNotEmpty()) {
                item {
                    com.centwise.core.design.components.TransactionTotalsCard(
                        income = totalIncome,
                        expense = totalExpense,
                        net = totalNet,
                        isDark = isDark
                    )
                }
            }

            // Transactions List or Empty State
            if (transactions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No Transactions Found",
                        description = if (searchQuery.isNotBlank() || selectedType != null || selectedCategory != null)
                            "No transactions matched the selected filters."
                        else "Add your first transaction to start tracking your finances.",
                        buttonText = "Add Transaction",
                        onButtonClick = onAddClick,
                        isDark = isDark
                    )
                }
            } else {
                val grouped = transactions.groupBy { tx ->
                    val monthFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US)
                    monthFormat.format(java.util.Date(tx.timestamp)).uppercase()
                }

                grouped.forEach { (monthKey, itemsInMonth) ->
                    item {
                        Text(
                            text = monthKey,
                            style = CentwiseTypography.Caption,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(itemsInMonth, key = { it.id }) { tx ->
                        TransactionRow(
                            transaction = tx,
                            onClick = { selectedTransaction = tx },
                            showBackground = true,
                            showChevron = true,
                            isDark = isDark
                        )
                    }
                }
            }
        }

        // Transaction Detail Sheet
        selectedTransaction?.let { tx ->
            TransactionDetailSheet(
                transaction = tx,
                onDismiss = { selectedTransaction = null },
                onDelete = { id ->
                    viewModel.deleteTransaction(id)
                    selectedTransaction = null
                },
                onEdit = { toEdit ->
                    selectedTransaction = null
                    editingTransaction = toEdit
                },
                isDark = isDark
            )
        }

        // Edit Transaction Sheet
        editingTransaction?.let { tx ->
            AddEditTransactionSheet(
                initialTransaction = tx,
                onDismiss = { editingTransaction = null },
                onSave = { updatedTx ->
                    val saved = viewModel.updateTransaction(updatedTx)
                    if (saved) editingTransaction = null
                    saved
                },
                isDark = isDark
            )
        }
    }
}

@Composable
private fun FilterButton(
    icon: ImageVector,
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    accent: Color,
    isDark: Boolean
) {
    val bg = if (isActive) accent else if (isDark) CentwiseColors.DarkSearchBg else CentwiseColors.LightSearchBg
    val contentColor = if (isActive) Color.White else if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = CentwiseTypography.Caption.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = contentColor
        )
        Spacer(modifier = Modifier.width(3.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(12.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionListScreenPreview() {
    TransactionListScreen()
}
