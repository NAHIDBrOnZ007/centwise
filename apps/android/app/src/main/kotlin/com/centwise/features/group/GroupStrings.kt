package com.centwise.features.group

import com.centwise.features.settings.LanguagePrefs

object GroupStrings {
    private val bn: Boolean
        get() = LanguagePrefs.isBengali

    val tabTitle: String get() = if (bn) "গ্রুপ" else "Group"

    // Overview Card
    val totalExpense: String get() = if (bn) "গ্রুপের মোট খরচ" else "Total Group Expense"
    val dailyAverage: String get() = if (bn) "গড় দৈনিক" else "Daily Average"
    val perDay: String get() = if (bn) " / দিন" else " / day"
    val totalDeposits: String get() = if (bn) "মোট ফান্ড জমা" else "Total Deposits"
    val cashInHand: String get() = if (bn) "হাতে জমা ফান্ড (ক্যাশ)" else "Cash in Hand"

    // Member Section
    val memberBalances: String get() = if (bn) "সদস্যদের ব্যালেন্স ও জমা" else "Member Balances & Deposits"
    val addDepositPill: String get() = if (bn) "জমা যোগ" else "Add Deposit"
    val depositLabel: String get() = if (bn) "জমা:" else "Deposit:"
    val spentLabel: String get() = if (bn) "খরচ:" else "Spent:"
    val surplusPrefix: String get() = if (bn) "অবশিষ্ট: " else "Surplus: "
    val duePrefix: String get() = if (bn) "বাকি: " else "Due: "
    val adminBadge: String get() = if (bn) "অ্যাডমিন" else "Admin"

    // Daily Expenses Section
    val dailyExpensesHeader: String get() = if (bn) "দৈনিক খরচের শিট" else "Daily Expense Sheet"
    fun daysOfExpenses(count: Int): String = if (bn) "${count} দিনের খরচ" else "${count} days of expenses"
    fun spentItemsSubtitle(name: String, itemCount: Int): String =
        if (bn) "${name} খরচ করেছেন • ${itemCount}টি আইটেম" else "${name} spent • ${itemCount} items"
    val perMemberShare: String get() = if (bn) "জনপ্রতি ভাগ: " else "Per-member share: "
    val itemizedDetailsHeader: String get() = if (bn) "খরচের ফর্দ (আইটেম তালিকা)" else "Itemized Expense Details"
    val notePrefix: String get() = if (bn) "বিবরণ: " else "Note: "

    // Empty Expenses
    val noExpensesTitle: String get() = if (bn) "এখনও কোনো খরচ যোগ করা হয়নি" else "No expenses logged yet"
    val noExpensesSubtitle: String get() = if (bn) "নিচের (+) বাটনে ট্যাপ করে আজকের বাজার বা খরচ যোগ করুন।" else "Tap the (+) button below to log today's groceries or group expenses."

    // Sheets Common
    val cancel: String get() = if (bn) "বাতিল" else "Cancel"
    val save: String get() = if (bn) "সংরক্ষণ" else "Save"
    val done: String get() = if (bn) "সম্পন্ন" else "Done"
    val confirmDeposit: String get() = if (bn) "জমা করুন" else "Confirm Deposit"
    val create: String get() = if (bn) "তৈরি করুন" else "Create"
    val join: String get() = if (bn) "যুক্ত হোন" else "Join"

    // Add Expense Sheet
    val addExpenseTitle: String get() = if (bn) "গ্রুপের খরচ যোগ করুন" else "Add Group Expense"
    val whoPaidQuestion: String get() = if (bn) "কে খরচ / বাজার করেছেন?" else "Who paid / shopped?"
    val quickItemLabel: String get() = if (bn) "আইটেম ট্যাপ করুন:" else "Tap common items:"
    val expenseListTitle: String get() = if (bn) "খরচের ফর্দ (আইটেম তালিকা)" else "Expense Item List"
    fun itemCountBadge(count: Int): String = if (bn) "${count} টি আইটেম" else "${count} items"
    val itemNamePlaceholder: String get() = if (bn) "যেমন: বাজার / বিল" else "e.g. Groceries / Bill"
    val quantityPlaceholder: String get() = if (bn) "পরিমাণ" else "Qty"
    val pricePlaceholder: String get() = if (bn) "৳ টাকা" else "৳ Price"
    val addAnotherItem: String get() = if (bn) "+ আরও আইটেম যোগ করুন" else "+ Add Another Item"
    val totalExpenseAmount: String get() = if (bn) "মোট খরচের পরিমাণ:" else "Total Expense Amount:"
    fun perMemberSplitLabel(count: Int): String = if (bn) "জনপ্রতি ভাগ (${count} জন সদস্য):" else "Per-member share (${count} members):"
    val perPersonSuffix: String get() = if (bn) " / জন" else " / person"
    val optionalNotesLabel: String get() = if (bn) "মন্তব্য / নোট (ঐচ্ছিক)" else "Notes & Details (Optional)"
    val optionalNotesPlaceholder: String get() = if (bn) "যেমন: স্পেশাল বাজার, বা বিদ্যুৎ বিল পরিশোধ..." else "e.g. Weekend groceries, or electricity bill..."

    // Add Deposit Sheet
    val addDepositTitle: String get() = if (bn) "সদস্যের জমা / অবদান যোগ করুন" else "Add Member Deposit"
    val whoDepositingQuestion: String get() = if (bn) "কোন সদস্য ফান্ডে জমা দিচ্ছেন?" else "Which member is depositing?"
    val depositAmountLabel: String get() = if (bn) "জমার পরিমাণ" else "Deposit Amount"
    val paymentMethodLabel: String get() = if (bn) "পেমেন্ট মাধ্যম" else "Payment Method"
    val depositNotesLabel: String get() = if (bn) "মন্তব্য / ট্রানজেকশন আইডি (ঐচ্ছিক)" else "Note / TrxID (Optional)"
    val depositNotesPlaceholder: String get() = if (bn) "যেমন: bKash TrxID বা ক্যাশ জমা..." else "e.g. bKash TrxID or Cash deposit..."

    // Invite Sheet
    val inviteTitle: String get() = if (bn) "সদস্য ইনভাইট করুন" else "Invite Members"
    val joinTitle: String get() = if (bn) "গ্রুপে যুক্ত হোন" else "Join Group"
    val myGroupQrTab: String get() = if (bn) "আমার গ্রুপ QR" else "My Group QR"
    val scanQrTab: String get() = if (bn) "QR স্ক্যান করুন" else "Scan QR"
    fun memberCountText(type: GroupType, count: Int): String =
        if (bn) "${type.titleBn} • ${count} জন সদস্য" else "${if (type == GroupType.FAMILY) "Family" else "Group"} • ${count} members"
    val groupJoinCodeLabel: String get() = if (bn) "গ্রুপ জয়েন কোড" else "Group Join Code"
    val copyCode: String get() = if (bn) "কপি করুন" else "Copy Code"
    val copied: String get() = if (bn) "কপি হয়েছে" else "Copied!"
    val inviteHint: String get() = if (bn) "পরিবার বা গ্রুপের অন্য সদস্যরা তাদের ফোনে সেন্টওয়াইজ অ্যাপ দিয়ে এই QR স্ক্যান করলেই সরাসরি যুক্ত হতে পারবে।" else "Family members or roommates can scan this QR code using their Centwise app to join instantly."
    val scannerInstruction: String get() = if (bn) "অন্য ফোনের QR কোড ধরুন" else "Point camera at other phone's QR code"
    val enterCodeLabel: String get() = if (bn) "অথবা গ্রুপ কোড লিখে যুক্ত হোন:" else "Or enter group code to join:"

    // Onboarding / Empty State
    val onboardingTitle: String get() = if (bn) "পরিবার ও গ্রুপের যৌথ হিসাব" else "Shared Group & Family Finances"
    val onboardingSubtitle: String get() = if (bn) "পরিবার, মেস, ফ্ল্যাটমেট বা ট্যুরের সবার দৈনিক বাজার খরচ, ডিপোজিট এবং ব্যালেন্স রাখুন সম্পূর্ণ স্বচ্ছ ও সহজ উপায়ে।" else "Effortlessly track daily groceries, shared expenses, deposits, and balances for family, roommates, mess, or trips."
    val createGroupButton: String get() = if (bn) "নতুন গ্রুপ তৈরি করুন" else "Create New Group"
    val scanQrButton: String get() = if (bn) "QR কোড স্ক্যান করে জয়েন করুন" else "Scan QR to Join"
    val loadDemoButton: String get() = if (bn) "নমুনা গ্রুপ ডাটা দিয়ে চালু করুন" else "Load Sample Group Data"

    // Create / Join Sheet
    val newGroupTab: String get() = if (bn) "নতুন গ্রুপ" else "New Group"
    val joinCodeTab: String get() = if (bn) "কোড দিয়ে জয়েন" else "Join by Code"
    val selectGroupType: String get() = if (bn) "গ্রুপের ধরন নির্বাচন করুন:" else "Select Group Type:"
    val groupNameLabel: String get() = if (bn) "গ্রুপের নাম" else "Group Name"
    val groupNamePlaceholder: String get() = if (bn) "যেমন: আমাদের সংসার / ধানমন্ডি মেস" else "e.g. Our Family / Flat 4B"
    val descriptionLabel: String get() = if (bn) "ঠিকানা / বর্ণনা (ঐচ্ছিক)" else "Address / Description (Optional)"
    val monthlyTargetLabel: String get() = if (bn) "মাসিক জনপ্রতি জমা / বাজেট টার্গেট (৳)" else "Monthly Deposit Target per Member (৳)"
    val yourNameLabel: String get() = if (bn) "আপনার নাম (অ্যাডমিন)" else "Your Name (Admin)"
    val memberNameLabel: String get() = if (bn) "আপনার নাম" else "Your Name"
    val enterMemberNamePlaceholder: String get() = if (bn) "সদস্যের নাম লিখুন" else "Enter your name"

    // Default Group Suggestions
    fun defaultGroupNameFor(type: GroupType): String = when (type) {
        GroupType.FAMILY -> if (bn) "আমাদের সংসার" else "Our Family"
        GroupType.MESS -> if (bn) "ব্যাচেলর মেস" else "Bachelor Mess"
        GroupType.FLATMATES -> if (bn) "রুমমেট শেয়ার" else "Flatmates Living"
        GroupType.TRIP -> if (bn) "কক্সবাজার ট্যুর" else "Cox's Bazar Trip"
        GroupType.OTHER -> if (bn) "শেয়ার্ড গ্রুপ" else "Shared Group"
    }

    fun groupTypeDisplay(type: GroupType): String = if (bn) type.titleBn else when (type) {
        GroupType.FAMILY -> "Family"
        GroupType.MESS -> "Bachelor Mess"
        GroupType.FLATMATES -> "Roommates"
        GroupType.TRIP -> "Tour & Trip"
        GroupType.OTHER -> "Other Group"
    }

    // Expense Details Sheet
    val expenseDetailsTitle: String get() = if (bn) "খরচের বিবরণ" else "Expense Details"
    val edit: String get() = if (bn) "এডিট" else "Edit"
    val editExpenseTitle: String get() = if (bn) "খরচ এডিট করুন" else "Edit Expense"
    val deleteExpense: String get() = if (bn) "খরচ ডিলিট করুন" else "Delete Expense"
    val deleteConfirmTitle: String get() = if (bn) "খরচ ডিলিট করতে চান?" else "Delete Expense?"
    val deleteConfirmMessage: String get() = if (bn) "এই খরচটি ডিলিট করলে সদস্যদের খরচের ভাগ স্বয়ংক্রিয়ভাবে সমন্বয় করা হবে।" else "Deleting this expense will automatically adjust member balances."
    val deleteAllForDate: String get() = if (bn) "এই দিনের সব খরচ মুছুন" else "Delete All for this Date"
    val deleteAllForDateConfirmTitle: String get() = if (bn) "এই দিনের সব খরচ মুছবেন?" else "Delete All Expenses for this Date?"
    val deleteAllForDateConfirmMessage: String get() = if (bn) "এই দিনের সকল খরচ ডিলিট করা হবে এবং সদস্যদের হিসাব সমন্বয় করা হবে।" else "All expenses recorded on this date will be deleted and member balances adjusted."
    val paidByLabel: String get() = if (bn) "যিনি খরচ করেছেন" else "Paid By"
    val contributorsLabel: String get() = if (bn) "যাঁরা খরচ করেছেন" else "Contributors"
    val splitBreakdownLabel: String get() = if (bn) "হিসাব ও অটো-স্প্লিট" else "Split Breakdown"
    val itemsSummary: String get() = if (bn) "বাজার / খরচের ফর্দ" else "Purchased Items"

    fun daySummarySubtitle(date: String, count: Int): String {
        val countText = if (bn) {
            val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
            val converted = count.toString().map { if (it in '0'..'9') bengaliDigits[it - '0'] else it }.joinToString("")
            "${converted}টি জিনিস"
        } else {
            if (count <= 1) "$count item" else "$count items"
        }
        return "$date • $countText"
    }

    // Weekly & All Transactions
    val weekly: String get() = if (bn) "সাপ্তাহিক" else "Weekly"
    val thisWeek: String get() = if (bn) "এই সপ্তাহে" else "This Week"
    val weeklyAverage: String get() = if (bn) "সাপ্তাহিক গড়" else "Weekly Avg"
    val perWeek: String get() = if (bn) " / সপ্তাহ" else " / week"
    val weeklyBreakdown: String get() = if (bn) "সাপ্তাহিক খরচের হিসাব" else "Weekly Spending Breakdown"
    val viewAll: String get() = if (bn) "সব দেখুন" else "View all"
    val thisWeekExpenses: String get() = if (bn) "এই সপ্তাহের খরচ" else "This Week's Expenses"
    val allTransactionsTitle: String get() = if (bn) "গ্রুপের সব লেনদেন" else "All Group Transactions"
    val allTab: String get() = if (bn) "সব" else "All"
    val expensesTab: String get() = if (bn) "খরচ" else "Expenses"
    val depositsTab: String get() = if (bn) "জমা" else "Deposits"
    val searchGroupTransactions: String get() = if (bn) "খরচ, জমা বা সদস্য খুঁজুন..." else "Search expenses, deposits, or members..."
    val noTransactionsFound: String get() = if (bn) "কোনো লেনদেন পাওয়া যায়নি" else "No transactions found"
    fun depositByMember(name: String): String = if (bn) "${name}-এর ফান্ড জমা" else "Deposit by ${name}"

    // Group Switcher Sheet & Active Story Strings
    val switchGroupTitle: String get() = if (bn) "গ্রুপ পরিবর্তন করুন" else "Switch Group"
    val yourGroups: String get() = if (bn) "আপনার গ্রুপসমূহ" else "Your Groups"
    val activeGroup: String get() = if (bn) "চলমান গ্রুপ" else "Active Group"
    val newBadge: String get() = if (bn) "নতুন" else "New"
    val shoppedToday: String get() = if (bn) "আজ বাজার করেছেন" else "Shopped today"
    val createNewGroupBtn: String get() = if (bn) "+ নতুন গ্রুপ তৈরি করুন" else "+ Create New Group"
    val joinGroupCodeBtn: String get() = if (bn) "কোড দিয়ে যুক্ত হন" else "Join with Code"
    val settledUp: String get() = if (bn) "হিসাব সমান (৳০)" else "Settled up (৳0)"
    val inviteShort: String get() = if (bn) "ইনভাইট" else "Invite"
    val groupQrCode: String get() = if (bn) "গ্রুপ কিউআর কোড" else "Group QR Code"
    val groupInfoAndInvite: String get() = if (bn) "গ্রুপের তথ্য ও কিউআর" else "Group Info & QR"
    fun membersCountText(count: Int): String {
        return if (bn) {
            val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
            val converted = count.toString().map { if (it in '0'..'9') bengaliDigits[it - '0'] else it }.joinToString("")
            "${converted} জন সদস্য"
        } else {
            if (count <= 1) "$count member" else "$count members"
        }
    }
}
