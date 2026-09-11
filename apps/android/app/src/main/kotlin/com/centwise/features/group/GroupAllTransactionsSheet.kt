package com.centwise.features.group

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.CentwiseSegmentedControl
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.core.profile.UserPrefs
import com.centwise.features.settings.LanguagePrefs
import kotlinx.coroutines.launch

private sealed class GroupAllItem {
    abstract val sortDate: String

    data class DayExpense(val dayGroup: DayExpenseGroup) : GroupAllItem() {
        override val sortDate: String get() = dayGroup.date
    }

    data class Deposit(val deposit: MemberDeposit) : GroupAllItem() {
        override val sortDate: String get() = deposit.date
    }
}

private enum class GroupTxTab { ALL, EXPENSES, DEPOSITS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAllTransactionsSheet(
    group: SharedGroup,
    onDismiss: () -> Unit,
    onSelectDayGroup: (DayExpenseGroup) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBorder = if (isDark) Color(0x14FFFFFF) else Color(0x0F000000)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val tabs = remember { listOf(GroupTxTab.ALL, GroupTxTab.EXPENSES, GroupTxTab.DEPOSITS) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(GroupTxTab.ALL) }

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

    // Filter, consolidate by day, and group by Month
    val groupedItemsByMonth = remember(group, searchQuery, selectedTab) {
        val q = searchQuery.trim().lowercase()

        // 1. Consolidated Day Expenses (one card per day, consolidating all users on that day)
        val filteredDayGroups = group.groupedDailyExpenses.filter { dayGroup ->
            if (q.isEmpty()) true
            else {
                dayGroup.date.lowercase().contains(q) ||
                        dayGroup.smartTitle.lowercase().contains(q) ||
                        dayGroup.entries.any { entry ->
                            entry.spenderName.lowercase().contains(q) ||
                                    entry.notes.lowercase().contains(q) ||
                                    entry.items.any { item -> item.name.lowercase().contains(q) }
                        }
            }
        }

        // 2. Member Deposits
        val filteredDeposits = group.deposits.filter { dep ->
            if (q.isEmpty()) true
            else {
                dep.memberName.lowercase().contains(q) ||
                        dep.date.lowercase().contains(q) ||
                        dep.paymentMethod.lowercase().contains(q) ||
                        dep.note.lowercase().contains(q)
            }
        }

        when (selectedTab) {
            GroupTxTab.EXPENSES -> {
                filteredDayGroups
                    .sortedByDescending { parseSortDate(it.date) }
                    .groupBy { extractMonthKey(it.date) }
                    .mapValues { (_, dayGroups) ->
                        dayGroups.map { GroupAllItem.DayExpense(it) }
                    }
            }
            GroupTxTab.DEPOSITS -> {
                filteredDeposits
                    .sortedByDescending { parseSortDate(it.date) }
                    .groupBy { extractMonthKey(it.date) }
                    .mapValues { (_, deposits) ->
                        deposits.map { GroupAllItem.Deposit(it) }
                    }
            }
            GroupTxTab.ALL -> {
                val allItems = (filteredDayGroups.map { GroupAllItem.DayExpense(it) } +
                        filteredDeposits.map { GroupAllItem.Deposit(it) })
                    .sortedByDescending { parseSortDate(it.sortDate) }

                allItems.groupBy { extractMonthKey(it.sortDate) }
            }
        }
    }

    val totalItemCount = groupedItemsByMonth.values.sumOf { it.size }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bg,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Navigation Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0x28FFFFFF) else Color(0x16000000),
                    modifier = Modifier
                        .clip(CircleShape)
                        .iosBounceClick { dismissWithAnimation {} }
                ) {
                    Text(
                        text = GroupStrings.cancel,
                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = textPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }

                Text(
                    text = GroupStrings.allTransactionsTitle,
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )

                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (LanguagePrefs.isBengali) {
                            val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
                            totalItemCount.toString().map { if (it in '0'..'9') bengaliDigits[it - '0'] else it }.joinToString("")
                        } else "$totalItemCount",
                        style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // 2. Financial Mini Summary Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = GroupStrings.totalExpense,
                            style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatBDT(group.totalExpense, useBengaliNumerals = LanguagePrefs.isBengali),
                            style = CentwiseTypography.Headline.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = CentwiseColors.ExpenseRed
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(cardBorder)
                    )

                    Column {
                        Text(
                            text = GroupStrings.totalDeposits,
                            style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatBDT(group.totalDeposits, useBengaliNumerals = LanguagePrefs.isBengali),
                            style = CentwiseTypography.Headline.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = CentwiseColors.IncomeGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(cardBorder)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = GroupStrings.cashInHand,
                            style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatBDT(group.cashInHand, useBengaliNumerals = LanguagePrefs.isBengali),
                            style = CentwiseTypography.Headline.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = if (group.cashInHand >= 0) accent else CentwiseColors.ExpenseRed
                        )
                    }
                }
            }

            // 3. Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = GroupStrings.searchGroupTransactions,
                        style = CentwiseTypography.Body.copy(fontSize = 14.sp),
                        color = textSecondary.copy(alpha = 0.7f)
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
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    disabledContainerColor = cardBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                singleLine = true
            )

            // 4. Centwise Segmented Control (All | Expenses | Deposits)
            CentwiseSegmentedControl(
                items = tabs,
                selectedItem = selectedTab,
                onItemSelected = { selectedTab = it },
                itemLabel = {
                    when (it) {
                        GroupTxTab.ALL -> GroupStrings.allTab
                        GroupTxTab.EXPENSES -> GroupStrings.expensesTab
                        GroupTxTab.DEPOSITS -> GroupStrings.depositsTab
                    }
                },
                accent = accent,
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )

            // 5. Lazy Loaded Transactions List Grouped by Month
            if (groupedItemsByMonth.isEmpty() || groupedItemsByMonth.values.all { it.isEmpty() }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = GroupStrings.noTransactionsFound,
                            style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                            color = textPrimary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedItemsByMonth.forEach { (monthKey, itemsInMonth) ->
                        // Month Header with optional month total
                        item(key = "header_$monthKey") {
                            val totalText = when (selectedTab) {
                                GroupTxTab.EXPENSES -> {
                                    val sum = itemsInMonth.filterIsInstance<GroupAllItem.DayExpense>()
                                        .sumOf { it.dayGroup.totalAmount }
                                    "- " + CurrencyFormatter.formatBDT(sum, useBengaliNumerals = LanguagePrefs.isBengali)
                                }
                                GroupTxTab.DEPOSITS -> {
                                    val sum = itemsInMonth.filterIsInstance<GroupAllItem.Deposit>()
                                        .sumOf { it.deposit.amount }
                                    "+ " + CurrencyFormatter.formatBDT(sum, useBengaliNumerals = LanguagePrefs.isBengali)
                                }
                                GroupTxTab.ALL -> null
                            }
                            val totalColor = when (selectedTab) {
                                GroupTxTab.EXPENSES -> CentwiseColors.ExpenseRed
                                GroupTxTab.DEPOSITS -> CentwiseColors.IncomeGreen
                                GroupTxTab.ALL -> Color.Unspecified
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatMonthDisplay(monthKey),
                                    style = CentwiseTypography.Caption,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                                if (totalText != null) {
                                    Text(
                                        text = totalText,
                                        style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.Bold),
                                        color = totalColor
                                    )
                                }
                            }
                        }

                        items(itemsInMonth, key = { item ->
                            when (item) {
                                is GroupAllItem.DayExpense -> "exp_day_${item.dayGroup.date}"
                                is GroupAllItem.Deposit -> "dep_${item.deposit.id}"
                            }
                        }) { item ->
                            when (item) {
                                is GroupAllItem.DayExpense -> {
                                    val dayGroup = item.dayGroup
                                    GroupExpenseRow(
                                        dayGroup = dayGroup,
                                        onClick = {
                                            dismissWithAnimation {
                                                onSelectDayGroup(dayGroup)
                                            }
                                        },
                                        accent = accent,
                                        cardBg = cardBg,
                                        cardBorder = cardBorder,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        isDark = isDark
                                    )
                                }
                                is GroupAllItem.Deposit -> {
                                    val deposit = item.deposit
                                    GroupDepositRow(
                                        deposit = deposit,
                                        accent = accent,
                                        cardBg = cardBg,
                                        cardBorder = cardBorder,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean Single Row for a Member Deposit, matching TransactionRow.
 */
@Composable
private fun GroupDepositRow(
    deposit: MemberDeposit,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Member avatar with green deposit ring
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = UserPrefs.getAvatarResId(deposit.memberAvatar)),
                    contentDescription = deposit.memberName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, CentwiseColors.IncomeGreen.copy(alpha = 0.5f), CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Member name and payment method / date / note
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = GroupStrings.depositByMember(deposit.memberName),
                    style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = textPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CentwiseColors.IncomeGreen.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = deposit.paymentMethod,
                            style = CentwiseTypography.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = CentwiseColors.IncomeGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = " • ${formatDepositDisplayDate(deposit.date)}",
                        style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                        color = textSecondary
                    )
                    if (deposit.note.isNotBlank()) {
                        Text(
                            text = " • ${deposit.note}",
                            style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                            color = textSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount
            Text(
                text = "+ " + CurrencyFormatter.formatBDT(deposit.amount, useBengaliNumerals = LanguagePrefs.isBengali),
                style = CentwiseTypography.AmountSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = CentwiseColors.IncomeGreen
            )
        }
    }
}

private fun extractMonthKey(dateStr: String): String {
    val parts = dateStr.trim().split("\\s+".toRegex())
    if (parts.size >= 3) {
        val monthShort = parts[1]
        val year = parts[2]
        val fullMonth = when (monthShort.lowercase()) {
            "jan" -> "January"
            "feb" -> "February"
            "mar" -> "March"
            "apr" -> "April"
            "may" -> "May"
            "jun" -> "June"
            "jul" -> "July"
            "aug" -> "August"
            "sep" -> "September"
            "oct" -> "October"
            "nov" -> "November"
            "dec" -> "December"
            else -> monthShort
        }
        return "$fullMonth $year"
    }
    return dateStr
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

private fun formatDepositDisplayDate(dateStr: String): String {
    if (LanguagePrefs.isBengali) {
        return dateStr.replace("Sep", "সেপ্টেম্বর")
            .replace("Oct", "অক্টোবর")
            .replace("Nov", "নভেম্বর")
            .replace("Dec", "ডিসেম্বর")
            .replace("Jan", "জানুয়ারি")
            .replace("Feb", "ফেব্রুয়ারি")
            .replace("Mar", "মার্চ")
            .replace("Apr", "এপ্রিল")
            .replace("May", "মে")
            .replace("Jun", "জুন")
            .replace("Jul", "জুলাই")
            .replace("Aug", "আগস্ট")
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
    return dateStr
}

private fun parseSortDate(dateStr: String): Long {
    return try {
        val parts = dateStr.trim().split("\\s+".toRegex())
        if (parts.size >= 3) {
            val day = parts[0].toIntOrNull() ?: 1
            val month = when (parts[1].lowercase()) {
                "jan" -> 1
                "feb" -> 2
                "mar" -> 3
                "apr" -> 4
                "may" -> 5
                "jun" -> 6
                "jul" -> 7
                "aug" -> 8
                "sep" -> 9
                "oct" -> 10
                "nov" -> 11
                "dec" -> 12
                else -> 1
            }
            val year = parts[2].toIntOrNull() ?: 2026
            (year * 10000L) + (month * 100L) + day
        } else 0L
    } catch (e: Exception) {
        0L
    }
}
