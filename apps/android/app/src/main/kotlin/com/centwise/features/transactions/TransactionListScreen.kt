package com.centwise.features.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.centwise.core.design.components.CategoryIconHelper
import com.centwise.core.design.components.DismissKeyboardOnScroll
import com.centwise.core.design.components.EmptyStateView
import com.centwise.core.design.components.TransactionTotalsCard
import com.centwise.core.design.components.clearFocusOnTapOutside
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.data.repository.TransactionRepository
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.settings.LanguagePrefs
import kotlinx.coroutines.launch

@Composable
fun TransactionListScreen(
    onAddClick: () -> Unit = {},
    viewModel: TransactionsViewModel = viewModel(),
    isDark: Boolean = isSystemInDarkTheme()
) {
    val coroutineScope = rememberCoroutineScope()
    val allPersonalTransactions by TransactionRepository.shared.transactions.collectAsState()
    val categories by TransactionRepository.shared.categories.collectAsState()

    // Filter states
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    // Detail & edit sheet states
    var selectedTransaction by remember { mutableStateOf<TransactionItem?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }

    var showPeriodMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val searchBg = if (isDark) CentwiseColors.DarkSearchBg else CentwiseColors.LightSearchBg
    val menuBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val menuBorder = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x0F000000))

    val listState = rememberLazyListState()
    DismissKeyboardOnScroll(listState)

    val rawTransactions = allPersonalTransactions

    // Apply Period, Search, and Category filters (base set for financial totals)
    val periodAndSearchFilteredItems = remember(rawTransactions, searchQuery, selectedPeriod, selectedCategory) {
        val cal = java.util.Calendar.getInstance()
        val now = java.util.Date()
        cal.time = now

        var items = rawTransactions

        // 1. Period filter
        when (selectedPeriod) {
            "This Month" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                items = items.filter { it.timestamp >= start }
            }
            "Last Month" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val thisMonthStart = cal.timeInMillis
                cal.add(java.util.Calendar.MONTH, -1)
                val lastMonthStart = cal.timeInMillis
                items = items.filter { it.timestamp in lastMonthStart until thisMonthStart }
            }
            "This Year" -> {
                cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                items = items.filter { it.timestamp >= start }
            }
        }

        // 2. Search filter
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            items = items.filter { item ->
                item.title.contains(q, ignoreCase = true) ||
                    item.category.contains(q, ignoreCase = true) ||
                    (item.note?.contains(q, ignoreCase = true) == true) ||
                    item.paymentMethod.contains(q, ignoreCase = true)
            }
        }

        // 3. Category filter
        if (selectedCategory != null) {
            items = items.filter { it.category == selectedCategory }
        }

        items
    }

    // Totals calculations (Income includes both INCOME and CREDIT / refunds)
    val totalIncome = remember(periodAndSearchFilteredItems) {
        periodAndSearchFilteredItems.filter { it.type == TransactionType.INCOME || it.type == TransactionType.CREDIT }.sumOf { it.amount }
    }
    val totalExpense = remember(periodAndSearchFilteredItems) {
        periodAndSearchFilteredItems.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val totalNet = totalIncome - totalExpense

    // Apply Type filter and Sort order for the transaction list
    val filteredTransactions = remember(periodAndSearchFilteredItems, selectedType, sortOrder) {
        var items = periodAndSearchFilteredItems
        if (selectedType != null) {
            items = items.filter { it.type == selectedType }
        }
        when (sortOrder) {
            TransactionSortOrder.DATE_DESC -> items.sortedByDescending { it.timestamp }
            TransactionSortOrder.DATE_ASC -> items.sortedBy { it.timestamp }
            TransactionSortOrder.AMOUNT_DESC -> items.sortedByDescending { it.amount }
            TransactionSortOrder.AMOUNT_ASC -> items.sortedBy { it.amount }
        }
    }

    // Month grouping
    val monthFormat = remember { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US) }
    val groupedItemsByMonth = remember(filteredTransactions) {
        filteredTransactions.groupBy { item ->
            monthFormat.format(java.util.Date(item.timestamp)).uppercase()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .clearFocusOnTapOutside()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Screen Title Header (Clean and minimal, matching all app tabs)
            item {
                Text(
                    text = if (LanguagePrefs.isBengali) "লেনদেন" else "Transactions",
                    style = CentwiseTypography.LargeTitle,
                    color = textPrimary
                )
            }

            // 3. Compact Slim Search Bar (Height: 36.dp, permanently visible)
            item {
                Surface(
                    shape = CircleShape,
                    color = searchBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (LanguagePrefs.isBengali) "লেনদেন খুঁজুন..." else "Search transactions...",
                                    style = CentwiseTypography.Caption.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = textSecondary
                                )
                            }

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearch(it) },
                                textStyle = CentwiseTypography.Body.copy(
                                    fontSize = 13.sp,
                                    color = textPrimary
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(accent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(textSecondary.copy(alpha = 0.2f))
                                    .clickable { viewModel.updateSearch("") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = textPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Filter Chips (Period, Type, Category) - Exact same as before / image
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
                                            imageVector = CategoryIconHelper.iconFor(cat.name, cat.icon),
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

            // 5. Financial Summary Card (Constant position!)
            item {
                TransactionTotalsCard(
                    income = totalIncome,
                    expense = totalExpense,
                    net = totalNet,
                    isDark = isDark
                )
            }

            // 6. Chronological Transactions List or Empty State
            if (filteredTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = if (LanguagePrefs.isBengali) "কোনো লেনদেন পাওয়া যায়নি" else "No Transactions Found",
                        description = if (searchQuery.isNotBlank() || selectedType != null || selectedCategory != null)
                            (if (LanguagePrefs.isBengali) "ফিল্টারের সাথে কোনো লেনদেন মেলেনি।" else "No transactions matched the selected filters.")
                        else (if (LanguagePrefs.isBengali) "ট্র্যাকিং শুরু করতে প্রথম লেনদেন যোগ করুন।" else "Add your first transaction to start tracking your finances."),
                        buttonText = if (LanguagePrefs.isBengali) "লেনদেন যোগ করুন" else "Add Transaction",
                        onButtonClick = onAddClick,
                        isDark = isDark
                    )
                }
            } else {
                groupedItemsByMonth.forEach { (monthKey, itemsInMonth) ->
                    item(key = "header_$monthKey") {
                        Text(
                            text = formatMonthDisplay(monthKey),
                            style = CentwiseTypography.Caption,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(itemsInMonth, key = { it.id }) { item ->
                        TransactionRow(
                            item = item,
                            onClick = { selectedTransaction = item },
                            accent = accent,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            cardBg = cardBg,
                            isDark = isDark
                        )
                    }
                }
            }
        }

        // Personal Transaction Detail Sheet
        selectedTransaction?.let { tx ->
            TransactionDetailSheet(
                transaction = tx,
                onDismiss = { selectedTransaction = null },
                onEdit = {
                    editingTransaction = tx
                    selectedTransaction = null
                },
                onDelete = {
                    viewModel.deleteTransaction(tx.id)
                    selectedTransaction = null
                },
                isDark = isDark
            )
        }

        // Personal Edit Transaction Sheet
        editingTransaction?.let { tx ->
            AddEditTransactionSheet(
                initialTransaction = tx,
                onDismiss = { editingTransaction = null },
                onSave = { updated ->
                    coroutineScope.launch {
                        viewModel.updateTransaction(updated)
                    }
                    true
                },
                isDark = isDark
            )
        }

    }
}


@Composable
private fun TransactionRow(
    item: TransactionItem,
    onClick: () -> Unit,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    isDark: Boolean
) {
    val isIncome = item.type == TransactionType.INCOME
    val amountColor = if (isIncome) CentwiseColors.IncomeGreen else CentwiseColors.ExpenseRed
    val amountPrefix = if (isIncome) "+ " else "- "

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .iosBounceClick { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIconHelper.iconFor(item.category),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Subtitle with Badge
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${item.category} • ${item.paymentMethod}",
                    style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Amount and Chevron
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = amountPrefix + CurrencyFormatter.formatBDT(item.amount, useBengaliNumerals = LanguagePrefs.isBengali),
                style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = amountColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
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
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

private fun formatMonthDisplay(monthKey: String): String {
    if (LanguagePrefs.isBengali) {
        return monthKey.replace("January", "জানুয়ারি")
            .replace("February", "ফেব্রুয়ারি")
            .replace("March", "মার্চ")
            .replace("April", "এপ্রিল")
            .replace("May", "মে")
            .replace("June", "জুন")
            .replace("July", "জুলাই")
            .replace("August", "আগস্ট")
            .replace("September", "সেপ্টেম্বর")
            .replace("October", "অক্টোবর")
            .replace("November", "নভেম্বর")
            .replace("December", "ডিসেম্বর")
            .replace("0", "০")
            .replace("1", "১")
            .replace("2", "২")
            .replace("3", "৩")
            .replace("4", "৪")
            .replace("5", "৫")
            .replace("6", "৬")
            .replace("7", "৭")
            .replace("8", "৮")
            .replace("9", "৯")
    }
    return monthKey
}

