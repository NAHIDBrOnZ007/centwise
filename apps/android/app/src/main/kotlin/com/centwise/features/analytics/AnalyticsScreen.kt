package com.centwise.features.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.centwise.core.design.components.CategoryIconHelper
import com.centwise.core.design.components.TransactionRow
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.repository.TransactionRepository
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.settings.LanguagePrefs
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.centwise.core.design.components.ScopeChipItem
import com.centwise.core.design.components.ScopeTabItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.features.group.GroupRepository
import com.centwise.features.group.SharedGroup

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel(),
    isDark: Boolean = isSystemInDarkTheme()
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val transactionCount by viewModel.transactionCount.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val topMerchants by viewModel.topMerchants.collectAsState()
    val categories by TransactionRepository.shared.categories.collectAsState()
    val trendPoints by viewModel.monthlyTrends.collectAsState()

    val allGroups by GroupRepository.shared.allGroups.collectAsState()
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val scopeTabItems: List<ScopeTabItem> = remember(allGroups, LanguagePrefs.isBengali) {
        val list = mutableListOf<ScopeTabItem>()
        // 1. Personal scope
        list.add(ScopeTabItem.Personal(title = if (LanguagePrefs.isBengali) "ব্যক্তিগত" else "Personal"))
        // 2. Each individual group
        allGroups.forEach { g ->
            list.add(ScopeTabItem.SpecificGroup(g))
        }
        list
    }

    val activeSelectedId = remember(selectedGroupId) {
        if (selectedGroupId == null) {
            "scope_personal"
        } else {
            "scope_group_$selectedGroupId"
        }
    }

    val currentSelectedGroup = remember(allGroups, selectedGroupId) {
        allGroups.find { it.id == selectedGroupId }
    }

    // Group Analytics Computations (Powered by Rust Core)
    val groupDailyExpenses = currentSelectedGroup?.dailyExpenses ?: emptyList()
    val groupDeposits = currentSelectedGroup?.deposits ?: emptyList()

    val (periodStart, periodEnd) = remember(selectedPeriod) {
        val range = AnalyticsViewModel.getPeriodDateRange(selectedPeriod)
        range.first.time to range.second.time + 1
    }

    val typeFilter = remember(selectedType) {
        when (selectedType) {
            "Debit" -> "debit"
            "Credit" -> "credit"
            else -> "all"
        }
    }

    val monthsBack: UInt = if (selectedPeriod == "3 Months") 3u else 6u

    val rustGroupAnalytics = remember(
        currentSelectedGroup?.id,
        periodStart,
        periodEnd,
        monthsBack,
        typeFilter,
        currentSelectedGroup?.dailyExpenses?.size,
        currentSelectedGroup?.deposits?.size
    ) {
        currentSelectedGroup?.let {
            GroupRepository.shared.getGroupAnalytics(it.id, periodStart, periodEnd, monthsBack, typeFilter)
        }
    }

    val filteredGroupExpenses = remember(groupDailyExpenses, periodStart, periodEnd) {
        val matches = groupDailyExpenses.filter { it.timestamp in periodStart..periodEnd }
        if (matches.isNotEmpty()) matches else groupDailyExpenses
    }

    val effectiveExpense = if (currentSelectedGroup != null) {
        (rustGroupAnalytics?.totalExpenseMinor ?: 0L) / 100.0
    } else totalExpense

    val effectiveIncome = if (currentSelectedGroup != null) {
        (rustGroupAnalytics?.totalDepositMinor ?: 0L) / 100.0
    } else totalIncome

    val effectiveCount = if (currentSelectedGroup != null) {
        rustGroupAnalytics?.transactionCount?.toInt() ?: 0
    } else transactionCount

    val effectiveCategoryBreakdown: List<CategorySpendItem> = remember(
        currentSelectedGroup,
        rustGroupAnalytics,
        categoryBreakdown
    ) {
        if (currentSelectedGroup != null) {
            rustGroupAnalytics?.categoryBreakdown?.map { cat ->
                CategorySpendItem(
                    category = cat.categoryName,
                    totalAmount = cat.totalMinor / 100.0,
                    percentage = cat.percentage,
                    count = cat.itemCount.toInt()
                )
            } ?: emptyList()
        } else {
            categoryBreakdown
        }
    }

    val effectiveTopMerchants: List<MerchantSpendItem> = remember(
        currentSelectedGroup,
        rustGroupAnalytics,
        topMerchants
    ) {
        if (currentSelectedGroup != null) {
            rustGroupAnalytics?.topSpenders?.map { spender ->
                MerchantSpendItem(
                    merchantName = spender.merchant,
                    totalAmount = spender.totalMinor / 100.0,
                    transactionCount = spender.transactionCount.toInt()
                )
            } ?: emptyList()
        } else {
            topMerchants
        }
    }

    val effectiveTrendPoints: List<TrendPoint> = remember(
        currentSelectedGroup,
        rustGroupAnalytics,
        trendPoints
    ) {
        if (currentSelectedGroup != null) {
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val trends = rustGroupAnalytics?.monthlyTrends
            if (!trends.isNullOrEmpty()) {
                trends.map {
                    val monthIdx = (it.month.toInt() - 1).coerceIn(0, 11)
                    TrendPoint(monthNames[monthIdx], it.totalExpenseMinor / 100.0)
                }
            } else {
                listOf(TrendPoint("Current", (rustGroupAnalytics?.totalExpenseMinor ?: 0L) / 100.0))
            }
        } else {
            trendPoints
        }
    }

    var drillDownTitle by remember { mutableStateOf<String?>(null) }
    var drillDownTransactions by remember { mutableStateOf<List<com.centwise.data.models.TransactionItem>>(emptyList()) }
    var selectedTransaction by remember { mutableStateOf<com.centwise.data.models.TransactionItem?>(null) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val dividerColor = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)

    val periodDays = when (selectedPeriod) {
        "This Month" -> 30
        "Last Month" -> 30
        "3 Months" -> 90
        "6 Months" -> 180
        else -> 365
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item {
            Text(
                text = if (LanguagePrefs.isBengali) "বিশ্লেষণ" else "Analytics",
                style = CentwiseTypography.LargeTitle,
                color = textPrimary
            )
        }

        // Scope Switcher Chips (Personal & Individual Groups)
        if (scopeTabItems.size > 1) {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    items(scopeTabItems, key = { it.id }) { tabItem ->
                        val isActive = tabItem.id == activeSelectedId
                        ScopeChipItem(
                            item = tabItem,
                            isActive = isActive,
                            accent = accent,
                            isDark = isDark,
                            onClick = {
                                if (!isActive) {
                                    if (AppearancePrefs.hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    when (tabItem) {
                                        is ScopeTabItem.Personal -> {
                                            selectedGroupId = null
                                        }
                                        is ScopeTabItem.SpecificGroup -> {
                                            selectedGroupId = tabItem.group.id
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // 1. Filter Bar (Matching iOS AnalyticsScreen filterBar 1:1 with iOS tactile physics)
        item {
            var showPeriodMenu by remember { mutableStateOf(false) }
            var showTypeMenu by remember { mutableStateOf(false) }
            val periods = listOf("This Month", "Last Month", "3 Months", "6 Months", "All Time")
            val types = listOf("All", "Debit", "Credit")
            val menuBg = if (isDark) Color(0xFF2C2C2E) else Color.White
            val menuBorder = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x0F000000))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Period Dropdown Chip (Active with Accent Color)
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .iosBounceClick { showPeriodMenu = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = selectedPeriod,
                                style = CentwiseTypography.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                color = Color.White,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

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
                        periods.forEach { period ->
                            val isSelected = selectedPeriod == period
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
                                        text = period,
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
                                    viewModel.setPeriod(period)
                                    showPeriodMenu = false
                                }
                            )
                        }
                    }
                }

                // 2. Transaction Type Dropdown Chip
                val isTypeActive = selectedType != "All"
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (isTypeActive) accent else cardBg,
                        border = if (!isTypeActive) BorderStroke(1.dp, dividerColor) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .iosBounceClick { showTypeMenu = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = if (isTypeActive) Color.White else textPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (selectedType == "All") "Type" else selectedType,
                                style = CentwiseTypography.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                color = if (isTypeActive) Color.White else textPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (isTypeActive) Color.White.copy(alpha = 0.8f) else textSecondary.copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

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
                        types.forEach { type ->
                            val isSelected = selectedType == type
                            val typeIcon = when (type) {
                                "Debit" -> Icons.AutoMirrored.Filled.ArrowForward
                                "Credit" -> Icons.Default.ArrowDownward
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
                                        text = type,
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
            }
        }

        // 2. Period Summary Hero Card (Matching iOS AnalyticsSummaryCard 1:1)
        item {
            AnalyticsSummaryCard(
                spent = effectiveExpense,
                income = effectiveIncome,
                transactionCount = effectiveCount,
                topCategoryName = effectiveCategoryBreakdown.firstOrNull()?.category,
                periodDays = periodDays,
                isDark = isDark
            )
        }

        // 3. Spending Trends (Last 6 Months with Dynamic Accent Bars)
        item {
            SpendingTrendsChart(points = effectiveTrendPoints, isDark = isDark)
        }

        // 4. Category Pie / Donut Chart (Matching iOS CategoryPieChart 1:1)
        item {
            val slices = remember(effectiveCategoryBreakdown, categories) {
                effectiveCategoryBreakdown.mapIndexed { index, item ->
                    CategorySlice(
                        name = item.category,
                        value = item.totalAmount,
                        color = categories
                            .firstOrNull { it.name.equals(item.category, ignoreCase = true) }?.color
                            ?: CategorySliceColors.palette[index % CategorySliceColors.palette.size]
                    )
                }
            }
            CategoryPieChart(
                slices = slices,
                isDark = isDark
            )
        }

        // 5. Category Spending Breakdown List (Matching iOS CategoryBreakdownList 1:1 with iOS cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (LanguagePrefs.isBengali) "ক্যাটাগরি" else "Categories",
                    style = CentwiseTypography.Headline,
                    color = textPrimary
                )

                if (effectiveCategoryBreakdown.isEmpty()) {
                    Text(
                        if (LanguagePrefs.isBengali) "কোনো ক্যাটাগরি পাওয়া যায়নি" else "No categories to show",
                        style = CentwiseTypography.Subheadline,
                        color = textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        effectiveCategoryBreakdown.forEach { item ->
                            val catRecord = categories.firstOrNull { it.name.equals(item.category, ignoreCase = true) }
                            val catIcon = CategoryIconHelper.iconFor(item.category, catRecord?.icon)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cardBg)
                                    .iosBounceClick {
                                        drillDownTitle = item.category
                                        drillDownTransactions = if (currentSelectedGroup != null) {
                                            filteredGroupExpenses.filter { entry ->
                                                if (entry.items.isNotEmpty()) {
                                                    entry.items.any { categorizeGroupItem(it.name).equals(item.category, ignoreCase = true) }
                                                } else {
                                                    item.category.contains("General", ignoreCase = true) || item.category.contains("সাধারণ", ignoreCase = true)
                                                }
                                            }.map { entry ->
                                                TransactionItem(
                                                    id = entry.id,
                                                    title = if (entry.items.isNotEmpty()) {
                                                        entry.items.filter { categorizeGroupItem(it.name).equals(item.category, ignoreCase = true) }
                                                            .joinToString(", ") { it.displayName }
                                                    } else entry.displayNotes.ifBlank { "Group Expense" },
                                                    amount = entry.totalAmount,
                                                    type = TransactionType.EXPENSE,
                                                    category = item.category,
                                                    paymentMethod = "Cash",
                                                    timestamp = entry.timestamp,
                                                    note = "Spent by ${entry.spenderName}"
                                                )
                                            }
                                        } else {
                                            viewModel.transactionsForCategory(item.category)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Clean unboxed category icon tinted with active theme accent (Matching iOS 1:1)
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = item.category,
                                        tint = accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Category Name & Count
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.category,
                                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                        color = textPrimary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.count} ${if (LanguagePrefs.isBengali) "লেনদেন" else if (item.count == 1) "transaction" else "transactions"}",
                                        style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                                        color = textSecondary
                                    )
                                }

                                // Amount & Percentage
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.formatBDT(item.totalAmount),
                                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                        color = textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f%%", item.percentage * 100),
                                        style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                                        color = textSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Top Spending Merchants / Spenders
        item {
            if (effectiveTopMerchants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (currentSelectedGroup != null) {
                            if (LanguagePrefs.isBengali) "শীর্ষ খরচকারী" else "Top Spenders"
                        } else {
                            if (LanguagePrefs.isBengali) "শীর্ষ মার্চেন্ট" else "Top Merchants"
                        },
                        style = CentwiseTypography.Headline,
                        color = textPrimary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        effectiveTopMerchants.take(5).forEachIndexed { index, merchantItem ->
                            val rank = index + 1
                            val isTop3 = rank <= 3
                            val rankBg = if (isTop3) accent.copy(alpha = 0.14f) else (if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                            val rankColor = if (isTop3) accent else textSecondary

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cardBg)
                                    .iosBounceClick {
                                        drillDownTitle = merchantItem.merchantName
                                        drillDownTransactions = if (currentSelectedGroup != null) {
                                            filteredGroupExpenses.filter { entry ->
                                                entry.spenderName.equals(merchantItem.merchantName, ignoreCase = true)
                                            }.map { entry ->
                                                TransactionItem(
                                                    id = entry.id,
                                                    title = if (entry.items.isNotEmpty()) entry.items.joinToString(", ") { it.displayName } else entry.displayNotes.ifBlank { "Group Expense" },
                                                    amount = entry.totalAmount,
                                                    type = TransactionType.EXPENSE,
                                                    category = "Group Expense",
                                                    paymentMethod = "Cash",
                                                    timestamp = entry.timestamp,
                                                    note = "Spent by ${entry.spenderName}"
                                                )
                                            }
                                        } else {
                                            viewModel.transactionsForMerchant(merchantItem.merchantName)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Circular Rank Badge (Matching iOS rank circle 1:1)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(rankBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$rank",
                                        style = CentwiseTypography.Caption.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = rankColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Spender / Merchant Name & Count
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = merchantItem.merchantName,
                                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                        color = textPrimary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${merchantItem.transactionCount} ${if (LanguagePrefs.isBengali) "লেনদেন" else if (merchantItem.transactionCount == 1) "transaction" else "transactions"}",
                                        style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                                        color = textSecondary
                                    )
                                }

                                // Amount
                                Text(
                                    text = CurrencyFormatter.formatBDT(merchantItem.totalAmount, compact = true),
                                    style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    color = textPrimary
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 7. Analytics Drill-Down Modal Sheet (Matching iOS AnalyticsDrillDownSheet, hidden while viewing details to prevent stacked sheets)
    if (drillDownTitle != null && selectedTransaction == null) {
        AnalyticsDrillDownSheet(
            title = drillDownTitle!!,
            transactions = drillDownTransactions,
            onDismiss = {
                drillDownTitle = null
                drillDownTransactions = emptyList()
            },
            onTransactionClick = { tx ->
                selectedTransaction = tx
            },
            accent = accent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark
        )
    }

    // 8. Transaction Detail Sheet (Matching iOS AnalyticsDrillDownSheet with onEdit = null)
    if (selectedTransaction != null) {
        com.centwise.features.transactions.TransactionDetailSheet(
            transaction = selectedTransaction!!,
            onDismiss = { selectedTransaction = null },
            onEdit = null,
            onDelete = { txId ->
                TransactionRepository.shared.deleteTransaction(txId)
                selectedTransaction = null
                drillDownTitle?.let { title ->
                    val isCategory = effectiveCategoryBreakdown.any { it.category.equals(title, ignoreCase = true) }
                    drillDownTransactions = if (currentSelectedGroup != null) {
                        filteredGroupExpenses.filter { entry ->
                            if (isCategory) {
                                entry.items.any { categorizeGroupItem(it.name).equals(title, ignoreCase = true) }
                            } else {
                                entry.spenderName.equals(title, ignoreCase = true)
                            }
                        }.map { entry ->
                            TransactionItem(
                                id = entry.id,
                                title = if (entry.items.isNotEmpty()) entry.items.joinToString(", ") { it.displayName } else entry.displayNotes.ifBlank { "Group Expense" },
                                amount = entry.totalAmount,
                                type = TransactionType.EXPENSE,
                                category = if (isCategory) title else "Group Expense",
                                paymentMethod = "Cash",
                                timestamp = entry.timestamp,
                                note = "Spent by ${entry.spenderName}"
                            )
                        }
                    } else {
                        if (isCategory) {
                            viewModel.transactionsForCategory(title)
                        } else {
                            viewModel.transactionsForMerchant(title)
                        }
                    }
                }
            },
            isDark = isDark
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsDrillDownSheet(
    title: String,
    transactions: List<com.centwise.data.models.TransactionItem>,
    onDismiss: () -> Unit,
    onTransactionClick: (com.centwise.data.models.TransactionItem) -> Unit,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val dismissWithAnimation: () -> Unit = {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7),
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Navigation Bar (Title & Done Pill matching iOS Done button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )

                Surface(
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .iosBounceClick { dismissWithAnimation() }
                ) {
                    Text(
                        text = "Done",
                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            Text(
                text = "${transactions.size} TRANSACTIONS",
                style = CentwiseTypography.Caption,
                color = textSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )

            if (transactions.isEmpty()) {
                Text(
                    text = "No transactions found for this period.",
                    style = CentwiseTypography.Subheadline,
                    color = textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionRow(
                            transaction = tx,
                            onClick = { onTransactionClick(tx) },
                            showBackground = true,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnalyticsScreenPreview() {
    AnalyticsScreen()
}

private object CategorySliceColors {
    val palette = listOf(
        Color(0xFFF97316),
        Color(0xFF06B6D4),
        Color(0xFFEC4899),
        Color(0xFFEAB308),
        Color(0xFF8B5CF6),
        Color(0xFF10B981),
        Color(0xFF007AFF),
        Color(0xFFEF4444)
    )
}

private fun categorizeGroupItem(name: String): String {
    val lower = name.trim().lowercase()
    return when {
        lower.contains("চাল") || lower.contains("rice") || lower.contains("ডাল") || lower.contains("lentil") || lower.contains("আটা") || lower.contains("ময়দা") ->
            if (LanguagePrefs.isBengali) "চাল ও ডাল" else "Grains & Lentils"
        lower.contains("মুরগি") || lower.contains("chicken") || lower.contains("মাংস") || lower.contains("beef") || lower.contains("meat") ||
        lower.contains("মাছ") || lower.contains("fish") ->
            if (LanguagePrefs.isBengali) "মাছ ও মাংস" else "Meat & Fish"
        lower.contains("সবজি") || lower.contains("শাক") || lower.contains("আলু") || lower.contains("পেঁয়াজ") || lower.contains("রসুন") || lower.contains("টমেটো") || lower.contains("মরিচ") ->
            if (LanguagePrefs.isBengali) "শাকসবজি" else "Vegetables"
        lower.contains("তেল") || lower.contains("oil") || lower.contains("ঘি") || lower.contains("মশলা") || lower.contains("spice") ->
            if (LanguagePrefs.isBengali) "তেল ও মশলা" else "Oil & Spices"
        lower.contains("ডিম") || lower.contains("egg") || lower.contains("দুধ") || lower.contains("milk") ->
            if (LanguagePrefs.isBengali) "ডিম ও দুধ" else "Dairy & Eggs"
        lower.contains("বিদ্যুৎ") || lower.contains("electric") || lower.contains("গ্যাস") || lower.contains("gas") || lower.contains("ওয়াইফাই") || lower.contains("wifi") || lower.contains("পানি") || lower.contains("water") ->
            if (LanguagePrefs.isBengali) "বিল ও ইউটিলিটি" else "Bills & Utilities"
        lower.contains("বাস") || lower.contains("train") || lower.contains("ভাড়া") || lower.contains("fare") || lower.contains("flight") ->
            if (LanguagePrefs.isBengali) "যাতায়াত" else "Transport"
        else -> if (LanguagePrefs.isBengali) "মুদি ও বাজার" else "Groceries"
    }
}
