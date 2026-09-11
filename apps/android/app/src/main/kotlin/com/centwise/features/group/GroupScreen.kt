package com.centwise.features.group

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.centwise.core.design.components.iosBounceClick
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.core.profile.UserPrefs
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.settings.LanguagePrefs

@Composable
fun GroupScreen(
    viewModel: GroupViewModel = viewModel(),
    isDark: Boolean = isSystemInDarkTheme()
) {
    val haptic = LocalHapticFeedback.current
    val currentGroup by viewModel.currentGroup.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()
    val hasAnyOtherGroupNewActivity = remember(allGroups, currentGroup) {
        allGroups.any { it.id != currentGroup?.id && it.hasNewActivity }
    }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBorder = if (isDark) Color(0x14FFFFFF) else Color(0x0F000000)

    // Sheets & Detail Selection
    var showGroupSwitcherSheet by remember { mutableStateOf(false) }
    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var showAddDepositSheet by remember { mutableStateOf(false) }
    var showInviteSheet by remember { mutableStateOf(false) }
    var showCreateJoinSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var selectedDayGroup by remember { mutableStateOf<DayExpenseGroup?>(null) }
    var selectedExpense by remember { mutableStateOf<DailyExpenseEntry?>(null) }
    var editingExpense by remember { mutableStateOf<DailyExpenseEntry?>(null) }
    var showAllTransactionsSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        if (currentGroup == null) {
            // Unjoined / First-Time State
            GroupEmptyOnboardingState(
                onCreateClick = { showCreateJoinSheet = true },
                onJoinClick = { showCreateJoinSheet = true },
                onRestoreDemo = { viewModel.resetToMockData() },
                accent = accent,
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                cardBorder = cardBorder,
                isDark = isDark
            )
        } else {
            val group = currentGroup!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header (Title, Subtitle & Action Pills)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showGroupSwitcherSheet = true
                                        if (AppearancePrefs.hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                            ) {
                                GroupIconBadge(
                                    type = group.type,
                                    size = 36.dp,
                                    iconSize = 20.dp,
                                    accent = accent
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = group.displayName,
                                        style = CentwiseTypography.LargeTitle.copy(fontSize = 21.sp),
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Switch Group",
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (hasAnyOtherGroupNewActivity) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(accent)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${group.monthName} • ${group.members.size} " +
                                        (if (LanguagePrefs.isBengali) "জন সদস্য" else "members") +
                                        if (group.description.isNotBlank()) " • ${group.description}" else "",
                                style = CentwiseTypography.Caption,
                                color = textSecondary
                            )
                        }

                        // Top Action Icons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // QR Invite Pill
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.14f))
                                    .border(1.dp, accent.copy(alpha = 0.3f), CircleShape)
                                    .clickable {
                                        showInviteSheet = true
                                        if (AppearancePrefs.hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "Invite",
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "QR",
                                    style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.Bold),
                                    color = accent
                                )
                            }

                            // Options Menu
                            Box {
                                IconButton(
                                    onClick = { showOptionsMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showOptionsMenu,
                                    onDismissRequest = { showOptionsMenu = false },
                                    modifier = Modifier.background(cardBg)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (LanguagePrefs.isBengali) "নতুন গ্রুপ তৈরি বা জয়েন" else "Create or Join Group", color = textPrimary) },
                                        leadingIcon = {
                                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = accent)
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            showCreateJoinSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (LanguagePrefs.isBengali) "ডেমো ডাটা রিসেট" else "Reset Demo Data", color = textPrimary) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = accent)
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            viewModel.resetToMockData()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (LanguagePrefs.isBengali) "খালি অবস্থা দেখুন (পরীক্ষার জন্য)" else "View Empty State (Test)", color = CentwiseColors.ExpenseRed) },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CentwiseColors.ExpenseRed)
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            viewModel.clearGroupForTesting()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Financial Overview Card
                item {
                    GroupOverviewCard(
                        group = group,
                        accent = accent,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        isDark = isDark
                    )
                }

                // 2.5. Weekly Spending Breakdown Card
                item {
                    WeeklySpendingCard(
                        group = group,
                        accent = accent,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        isDark = isDark
                    )
                }

                // 3. Member Balances Section
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = GroupStrings.memberBalances,
                                style = CentwiseTypography.Headline,
                                color = textPrimary
                            )

                            // Quick Add Deposit Pill in header (Uses Dynamic Theme Accent)
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.12f))
                                    .clickable {
                                        showAddDepositSheet = true
                                        if (AppearancePrefs.hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = GroupStrings.addDepositPill,
                                    style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.Bold),
                                    color = accent
                                )
                            }
                        }

                        // Horizontal Members Carousel
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val activeSpenderIds = group.activeSpenderIdsToday
                            items(group.members) { member ->
                                val isSpenderToday = member.id in activeSpenderIds
                                MemberBalanceCard(
                                    member = member,
                                    isSpenderToday = isSpenderToday,
                                    accent = accent,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onDepositClick = { showAddDepositSheet = true }
                                )
                            }
                        }
                    }
                }

                // 4. Weekly Expenses Section Header with "View all" Button (Matching HomeScreen 1:1, No Leading Icon)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = GroupStrings.thisWeekExpenses,
                            style = CentwiseTypography.Headline,
                            color = textPrimary
                        )

                        // "View all" / "সব দেখুন" Action Button
                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .iosBounceClick {
                                    showAllTransactionsSheet = true
                                    if (AppearancePrefs.hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = GroupStrings.viewAll,
                                    style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.Bold),
                                    color = accent
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Daily Expenses List Items (One Day = One Card, Tight 8.dp Gap, Date inside card)
                if (group.dailyExpenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = GroupStrings.noExpensesTitle,
                                    style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                                    color = textPrimary
                                )
                                Text(
                                    text = GroupStrings.noExpensesSubtitle,
                                    style = CentwiseTypography.Caption,
                                    color = textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val recentDayGroups = group.groupedDailyExpenses.take(5)
                            recentDayGroups.forEach { dayGroup ->
                                GroupExpenseRow(
                                    dayGroup = dayGroup,
                                    onClick = {
                                        selectedDayGroup = dayGroup
                                        if (AppearancePrefs.hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        }
                    }
                }
            }

            // Floating Action Button (+) with Dynamic Theme Accent - Matching HomeScreen 1:1
            FloatingActionButton(
                onClick = {
                    showAddExpenseSheet = true
                    if (AppearancePrefs.hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                shape = CircleShape,
                containerColor = accent,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 90.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Active Bottom Sheets
        if (showAddExpenseSheet && currentGroup != null) {
            AddExpenseSheet(
                group = currentGroup!!,
                onDismiss = { showAddExpenseSheet = false },
                onSave = { spenderId, items, notes ->
                    viewModel.addExpenseEntry(spenderId, items, notes)
                },
                accent = accent,
                isDark = isDark
            )
        }

        if (showAddDepositSheet && currentGroup != null) {
            AddDepositSheet(
                group = currentGroup!!,
                onDismiss = { showAddDepositSheet = false },
                onSave = { memberId, amount, method, note ->
                    viewModel.addMemberDeposit(memberId, amount, method, note)
                },
                accent = accent,
                isDark = isDark
            )
        }

        if (showInviteSheet && currentGroup != null) {
            GroupInviteSheet(
                group = currentGroup!!,
                onDismiss = { showInviteSheet = false },
                onJoinAnother = { code ->
                    viewModel.joinGroup(code, "New Member")
                },
                accent = accent,
                isDark = isDark
            )
        }

        if (showCreateJoinSheet) {
            CreateJoinGroupSheet(
                onDismiss = { showCreateJoinSheet = false },
                onCreateGroup = { name, type, desc, target, creatorName ->
                    viewModel.createGroup(name, type, desc, target, creatorName)
                },
                onJoinGroup = { code, memberName ->
                    viewModel.joinGroup(code, memberName)
                },
                accent = accent,
                isDark = isDark
            )
        }

        // Expense Details Sheet for Selected Day (Consolidated Day Group)
        selectedDayGroup?.let { dayGroup ->
            GroupExpenseDetailSheet(
                dayGroup = dayGroup,
                memberCount = currentGroup?.members?.size ?: 1,
                onDismiss = { selectedDayGroup = null },
                onDeleteEntry = { id ->
                    viewModel.deleteExpenseEntry(id)
                    selectedDayGroup = null
                },
                onDeleteAllForDate = { date ->
                    viewModel.deleteExpenseEntriesForDate(date)
                    selectedDayGroup = null
                },
                onEditEntry = { toEdit ->
                    selectedDayGroup = null
                    editingExpense = toEdit
                },
                accent = accent,
                isDark = isDark
            )
        }

        // Single Expense Details Sheet (from All Transactions search/filter)
        selectedExpense?.let { expense ->
            GroupExpenseDetailSheet(
                expense = expense,
                memberCount = currentGroup?.members?.size ?: 1,
                onDismiss = { selectedExpense = null },
                onDelete = { id ->
                    viewModel.deleteExpenseEntry(id)
                    selectedExpense = null
                },
                onEdit = { toEdit ->
                    selectedExpense = null
                    editingExpense = toEdit
                },
                accent = accent,
                isDark = isDark
            )
        }

        // Edit Expense Sheet (Matching AddEditTransactionSheet 1:1)
        editingExpense?.let { expense ->
            AddExpenseSheet(
                group = currentGroup!!,
                initialExpense = expense,
                onDismiss = { editingExpense = null },
                onSave = { spenderId, items, notes ->
                    viewModel.updateExpenseEntry(expense.id, spenderId, items, notes)
                    editingExpense = null
                },
                accent = accent,
                isDark = isDark
            )
        }

        // All Group Transactions Sheet (Search, Segmented Tabs, LazyColumn)
        if (showAllTransactionsSheet && currentGroup != null) {
            GroupAllTransactionsSheet(
                group = currentGroup!!,
                onDismiss = { showAllTransactionsSheet = false },
                onSelectDayGroup = { dayGroup ->
                    showAllTransactionsSheet = false
                    selectedDayGroup = dayGroup
                },
                accent = accent,
                isDark = isDark
            )
        }

        // Group Switcher Modal Bottom Sheet
        if (showGroupSwitcherSheet && currentGroup != null) {
            GroupSwitcherSheet(
                allGroups = allGroups,
                currentGroupId = currentGroup!!.id,
                onSelectGroup = { groupId ->
                    viewModel.selectGroup(groupId)
                },
                onCreateNewGroup = {
                    showCreateJoinSheet = true
                },
                onJoinGroup = {
                    showCreateJoinSheet = true
                },
                onDismiss = { showGroupSwitcherSheet = false },
                accent = accent,
                isDark = isDark
            )
        }
    }
}

/**
 * Overview card matching SpendingSummaryCard: Hero amount on top, horizontal divider,
 * and 4-column stats breakdown with vertical colored stripes (Total Deposits, Weekly, Daily, Cash in Hand).
 */
@Composable
private fun GroupOverviewCard(
    group: SharedGroup,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .padding(18.dp)
    ) {
        // Hero: Total Group Expense (Matching SpendingSummaryCard 1:1)
        Text(
            text = GroupStrings.totalExpense,
            style = CentwiseTypography.Subheadline,
            color = textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = CurrencyFormatter.formatBDT(group.totalExpense, useBengaliNumerals = LanguagePrefs.isBengali),
            style = CentwiseTypography.HeroAmount,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(
            color = if (isDark) Color(0x14FFFFFF) else Color(0x0D000000),
            thickness = 1.dp
        )
        Spacer(modifier = Modifier.height(14.dp))

        // 4-Column Stats Breakdown with Vertical Colored Stripes (Deposits, Weekly, Daily, Cash in Hand)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Total Deposits
            StatColumn(
                label = GroupStrings.totalDeposits,
                amount = group.totalDeposits,
                stripeColor = CentwiseColors.IncomeGreen,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )

            // 2. Weekly (This Week / Weekly Expense)
            StatColumn(
                label = GroupStrings.weekly,
                amount = group.weeklyExpense,
                stripeColor = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )

            // 3. Daily Average
            StatColumn(
                label = GroupStrings.dailyAverage,
                amount = group.averageDailyCost,
                stripeColor = accent.copy(alpha = 0.65f),
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )

            // 4. Cash in Hand
            StatColumn(
                label = GroupStrings.cashInHand,
                amount = group.cashInHand,
                stripeColor = if (group.cashInHand >= 0) CentwiseColors.SavedTeal else CentwiseColors.ExpenseRed,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Visual Weekly Spending Breakdown Card with 4-week bars of the current month.
 */
@Composable
private fun WeeklySpendingCard(
    group: SharedGroup,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean
) {
    val weeklyBreakdown = remember(group.dailyExpenses) { group.weeklyBreakdown() }
    val maxWeekAmount = remember(weeklyBreakdown) {
        weeklyBreakdown.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    }

    Surface(
        shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = GroupStrings.weeklyBreakdown,
                        style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                        color = textPrimary
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = CurrencyFormatter.formatBDT(group.currentWeekExpense, useBengaliNumerals = LanguagePrefs.isBengali) + " " + GroupStrings.thisWeek,
                        style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = cardBorder, thickness = 0.8.dp)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                weeklyBreakdown.forEach { (weekLabel, amount) ->
                    val fraction = (amount / maxWeekAmount).toFloat().coerceIn(0f, 1f)
                    val isCurrent = weekLabel.contains("2") || (amount > 0 && fraction > 0.5f)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = weekLabel,
                                style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                                color = if (amount > 0) textPrimary else textSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatBDT(amount, useBengaliNumerals = LanguagePrefs.isBengali),
                                style = CentwiseTypography.Caption.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (amount > 0) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (amount > 0) textPrimary else textSecondary
                            )
                        }

                        // Progress track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0F000000))
                        ) {
                            if (fraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isCurrent) accent else accent.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    amount: Double,
    stripeColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(stripeColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = CurrencyFormatter.formatBDT(amount, showSign = false, compact = true, useBengaliNumerals = LanguagePrefs.isBengali),
                style = CentwiseTypography.AmountSmall,
                color = textPrimary
            )
            Text(
                text = label,
                style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                color = textSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * Member card showing deposit, share, and positive/negative balance.
 */
@Composable
private fun MemberBalanceCard(
    member: GroupMember,
    isSpenderToday: Boolean = false,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onDepositClick: () -> Unit
) {
    val isPositive = member.balance >= 0

    Column(
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .background(cardBg)
            .border(
                width = if (isSpenderToday) 1.5.dp else 1.dp,
                color = if (isSpenderToday) accent.copy(alpha = 0.45f) else cardBorder,
                shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge)
            )
            .clickable { onDepositClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Messenger Story-Style Avatar Ring
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isSpenderToday) 2.dp else 1.dp,
                            color = if (isSpenderToday) accent else Color.Transparent,
                            shape = CircleShape
                        )
                        .padding(if (isSpenderToday) 2.5.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = UserPrefs.getAvatarResId(member.avatar)),
                        contentDescription = member.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                // Active Story Indicator Dot (bottom right of avatar)
                if (isSpenderToday) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .border(1.5.dp, cardBg, CircleShape)
                    )
                }
            }

            Column {
                Text(
                    text = member.name,
                    style = CentwiseTypography.Headline.copy(fontSize = 14.sp),
                    color = textPrimary,
                    maxLines = 1
                )
                if (isSpenderToday) {
                    Text(
                        text = GroupStrings.shoppedToday,
                        style = CentwiseTypography.Caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent,
                        maxLines = 1
                    )
                } else if (member.isAdmin) {
                    Text(
                        text = GroupStrings.adminBadge,
                        style = CentwiseTypography.Caption.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent
                    )
                }
            }
        }

        HorizontalDivider(color = cardBorder, thickness = 0.6.dp)

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = GroupStrings.depositLabel, style = CentwiseTypography.Caption, color = textSecondary)
                Text(
                    text = CurrencyFormatter.formatBDT(member.depositPaid, useBengaliNumerals = LanguagePrefs.isBengali),
                    style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = textPrimary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = GroupStrings.spentLabel, style = CentwiseTypography.Caption, color = textSecondary)
                Text(
                    text = CurrencyFormatter.formatBDT(member.totalSpentShare, useBengaliNumerals = LanguagePrefs.isBengali),
                    style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = textSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isPositive) accent.copy(alpha = 0.12f)
                    else CentwiseColors.ExpenseRed.copy(alpha = 0.12f)
                )
                .padding(vertical = 5.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPositive) GroupStrings.surplusPrefix + CurrencyFormatter.formatBDT(member.balance, useBengaliNumerals = LanguagePrefs.isBengali)
                else GroupStrings.duePrefix + CurrencyFormatter.formatBDT(kotlin.math.abs(member.balance), useBengaliNumerals = LanguagePrefs.isBengali),
                style = CentwiseTypography.Caption.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = if (isPositive) accent else CentwiseColors.ExpenseRed
            )
        }
    }
}

/**
 * Single clean row for a consolidated day's expenses, matching TransactionRow 1:1.
 * Title displays smart item names, subtitle displays date and item count (no buyer name clutter),
 * leading unboxed icon tinted with dynamic accent, amount in expense red.
 * Clicking row triggers iOS bounce click and opens the consolidated detail modal sheet.
 */
@Composable
fun GroupExpenseRow(
    dayGroup: DayExpenseGroup,
    onClick: () -> Unit,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val subtitleText = remember(dayGroup.displayDate, dayGroup.totalItemCount) {
        GroupStrings.daySummarySubtitle(dayGroup.displayDate, dayGroup.totalItemCount)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .iosBounceClick { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clean unboxed category icon tinted with active theme accent (Matching TransactionRow 1:1)
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "Groceries",
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title (Smart Item Names) & Subtitle (Date • Item count, no user name)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dayGroup.smartTitle,
                    style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = textPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitleText,
                    style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                    color = textSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount
            Text(
                text = "- " + CurrencyFormatter.formatBDT(dayGroup.totalAmount, useBengaliNumerals = LanguagePrefs.isBengali),
                style = CentwiseTypography.AmountSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = CentwiseColors.ExpenseRed
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Chevron >
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = textSecondary.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun GroupExpenseRow(
    entry: DailyExpenseEntry,
    onClick: () -> Unit,
    accent: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    GroupExpenseRow(
        dayGroup = DayExpenseGroup(date = entry.date, entries = listOf(entry)),
        onClick = onClick,
        accent = accent,
        cardBg = cardBg,
        cardBorder = cardBorder,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        isDark = isDark,
        modifier = modifier
    )
}

/**
 * Onboarding / Empty view when no group has been created or joined yet.
 */
@Composable
private fun GroupEmptyOnboardingState(
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    onRestoreDemo: () -> Unit,
    accent: Color,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    cardBorder: Color,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f))
                .border(2.dp, accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = GroupStrings.onboardingTitle,
            style = CentwiseTypography.Title1.copy(fontWeight = FontWeight.Bold),
            color = textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = GroupStrings.onboardingSubtitle,
            style = CentwiseTypography.Body,
            color = textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AddHome,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = GroupStrings.createGroupButton,
                style = CentwiseTypography.Headline.copy(fontSize = 16.sp),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onJoinClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, accent)
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = accent
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = GroupStrings.scanQrButton,
                style = CentwiseTypography.Headline.copy(fontSize = 16.sp),
                color = accent
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onRestoreDemo) {
            Text(
                text = GroupStrings.loadDemoButton,
                style = CentwiseTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = textSecondary
            )
        }
    }
}
