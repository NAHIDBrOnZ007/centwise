package com.centwise.features.group

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GroupRepository private constructor() {

    private val _allGroups = MutableStateFlow<List<SharedGroup>>(createDefaultMockGroups())
    val allGroups: StateFlow<List<SharedGroup>> = _allGroups.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String>(_allGroups.value.firstOrNull()?.id ?: "")
    val selectedGroupId: StateFlow<String> = _selectedGroupId.asStateFlow()

    private val _currentGroup = MutableStateFlow<SharedGroup?>(_allGroups.value.firstOrNull())
    val currentGroup: StateFlow<SharedGroup?> = _currentGroup.asStateFlow()

    fun selectGroup(groupId: String) {
        val found = _allGroups.value.find { it.id == groupId } ?: return
        _selectedGroupId.value = groupId
        // Mark activity as read upon selection
        val marked = found.copy(hasNewActivity = false)
        _allGroups.value = _allGroups.value.map { if (it.id == groupId) marked else it }
        _currentGroup.value = marked
    }

    private fun updateActiveGroup(updated: SharedGroup) {
        _allGroups.value = _allGroups.value.map {
            if (it.id == updated.id) updated else it
        }
        if (_selectedGroupId.value == updated.id) {
            _currentGroup.value = updated
        }
    }

    fun createGroup(name: String, type: GroupType, description: String, monthlyTarget: Double, creatorName: String) {
        val creator = GroupMember(
            id = UUID.randomUUID().toString(),
            name = creatorName.ifBlank { "You" },
            avatar = "avatar_1",
            isAdmin = true,
            depositPaid = monthlyTarget,
            totalSpentShare = 0.0
        )
        val newGroup = SharedGroup(
            id = "group_" + System.currentTimeMillis(),
            name = name.ifBlank { "আমাদের গ্রুপ" },
            type = type,
            description = description.ifBlank { "" },
            joinCode = "GRP-" + (1000..9999).random(),
            monthName = getCurrentMonthYear(),
            monthlyTarget = monthlyTarget,
            members = listOf(creator),
            dailyExpenses = emptyList(),
            deposits = listOf(
                MemberDeposit(
                    memberId = creator.id,
                    memberName = creator.name,
                    memberAvatar = creator.avatar,
                    amount = monthlyTarget,
                    date = getCurrentDateFormatted(),
                    paymentMethod = "Cash",
                    note = "Initial deposit"
                )
            ),
            hasNewActivity = false
        )
        _allGroups.value = listOf(newGroup) + _allGroups.value
        _selectedGroupId.value = newGroup.id
        _currentGroup.value = newGroup
    }

    fun joinGroup(joinCode: String, newMemberName: String): Boolean {
        val group = _currentGroup.value ?: _allGroups.value.firstOrNull() ?: return false
        val newMember = GroupMember(
            id = UUID.randomUUID().toString(),
            name = newMemberName.ifBlank { "New Member" },
            avatar = "avatar_5",
            depositPaid = group.monthlyTarget,
            totalSpentShare = 0.0
        )
        val updated = group.copy(
            members = group.members + newMember
        )
        updateActiveGroup(updated)
        return true
    }

    fun addExpenseEntry(
        spenderId: String,
        items: List<ExpenseItem>,
        notes: String = ""
    ) {
        val group = _currentGroup.value ?: return
        if (items.isEmpty()) return

        val spender = group.members.find { it.id == spenderId } ?: group.members.firstOrNull() ?: return
        val total = items.sumOf { it.price }
        val memberCount = if (group.members.isNotEmpty()) group.members.size else 1
        val splitPerMember = total / memberCount

        val entry = DailyExpenseEntry(
            id = UUID.randomUUID().toString(),
            date = getCurrentDateFormatted(),
            timestamp = System.currentTimeMillis(),
            spenderId = spender.id,
            spenderName = spender.name,
            spenderAvatar = spender.avatar,
            items = items,
            totalAmount = total,
            splitPerMember = splitPerMember,
            notes = notes
        )

        val updatedMembers = group.members.map { member ->
            member.copy(totalSpentShare = member.totalSpentShare + splitPerMember)
        }

        val updated = group.copy(
            members = updatedMembers,
            dailyExpenses = listOf(entry) + group.dailyExpenses
        )
        updateActiveGroup(updated)
    }

    fun updateExpenseEntry(
        entryId: String,
        spenderId: String,
        items: List<ExpenseItem>,
        notes: String = ""
    ) {
        val group = _currentGroup.value ?: return
        val existingIndex = group.dailyExpenses.indexOfFirst { it.id == entryId }
        if (existingIndex < 0 || items.isEmpty()) return

        val oldEntry = group.dailyExpenses[existingIndex]
        val spender = group.members.find { it.id == spenderId } ?: group.members.firstOrNull() ?: return
        val total = items.sumOf { it.price }
        val memberCount = if (group.members.isNotEmpty()) group.members.size else 1
        val splitPerMember = total / memberCount

        val oldSplit = oldEntry.splitPerMember
        val diffSplit = splitPerMember - oldSplit

        val updatedEntry = oldEntry.copy(
            spenderId = spender.id,
            spenderName = spender.name,
            spenderAvatar = spender.avatar,
            items = items,
            totalAmount = total,
            splitPerMember = splitPerMember,
            notes = notes
        )

        val updatedMembers = group.members.map { member ->
            member.copy(totalSpentShare = maxOf(0.0, member.totalSpentShare + diffSplit))
        }

        val updatedExpenses = group.dailyExpenses.toMutableList()
        updatedExpenses[existingIndex] = updatedEntry

        val updated = group.copy(
            members = updatedMembers,
            dailyExpenses = updatedExpenses
        )
        updateActiveGroup(updated)
    }

    fun deleteExpenseEntry(entryId: String) {
        val group = _currentGroup.value ?: return
        val target = group.dailyExpenses.find { it.id == entryId } ?: return

        val splitShare = target.splitPerMember
        val updatedMembers = group.members.map { member ->
            member.copy(totalSpentShare = maxOf(0.0, member.totalSpentShare - splitShare))
        }

        val updated = group.copy(
            members = updatedMembers,
            dailyExpenses = group.dailyExpenses.filter { it.id != entryId }
        )
        updateActiveGroup(updated)
    }

    fun deleteExpenseEntriesForDate(date: String) {
        val group = _currentGroup.value ?: return
        val targets = group.dailyExpenses.filter { it.date == date }
        if (targets.isEmpty()) return

        val totalSplitToRemove = targets.sumOf { it.splitPerMember }
        val updatedMembers = group.members.map { member ->
            member.copy(totalSpentShare = maxOf(0.0, member.totalSpentShare - totalSplitToRemove))
        }

        val updated = group.copy(
            members = updatedMembers,
            dailyExpenses = group.dailyExpenses.filter { it.date != date }
        )
        updateActiveGroup(updated)
    }

    fun addMemberDeposit(
        memberId: String,
        amount: Double,
        paymentMethod: String = "Cash",
        note: String = ""
    ) {
        val group = _currentGroup.value ?: return
        val member = group.members.find { it.id == memberId } ?: return

        val newDeposit = MemberDeposit(
            id = UUID.randomUUID().toString(),
            memberId = member.id,
            memberName = member.name,
            memberAvatar = member.avatar,
            amount = amount,
            date = getCurrentDateFormatted(),
            paymentMethod = paymentMethod,
            note = note
        )

        val updatedMembers = group.members.map {
            if (it.id == memberId) it.copy(depositPaid = it.depositPaid + amount) else it
        }

        val updated = group.copy(
            members = updatedMembers,
            deposits = listOf(newDeposit) + group.deposits
        )
        updateActiveGroup(updated)
    }

    fun resetToMockData() {
        val mocks = createDefaultMockGroups()
        _allGroups.value = mocks
        _selectedGroupId.value = mocks.first().id
        _currentGroup.value = mocks.first()
    }

    fun clearGroupForTesting() {
        _allGroups.value = emptyList()
        _selectedGroupId.value = ""
        _currentGroup.value = null
    }

    private fun getCurrentDateFormatted(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
        return sdf.format(Date())
    }

    private fun getCurrentMonthYear(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        return sdf.format(Date())
    }

    companion object {
        val shared = GroupRepository()

        private fun createDefaultMockGroups(): List<SharedGroup> {
            return listOf(
                createMessGroup(),
                createTourGroup(),
                createFamilyGroup()
            )
        }

        private fun createMessGroup(): SharedGroup {
            val members = listOf(
                GroupMember(
                    id = "m1",
                    name = "Faysal (You)",
                    avatar = "avatar_1",
                    phone = "+880 1711 000001",
                    isAdmin = true,
                    depositPaid = 5000.0,
                    totalSpentShare = 3690.0
                ),
                GroupMember(
                    id = "m2",
                    name = "Tanvir",
                    avatar = "avatar_2",
                    isAdmin = false,
                    depositPaid = 5000.0,
                    totalSpentShare = 3690.0
                ),
                GroupMember(
                    id = "m3",
                    name = "Sakib",
                    avatar = "avatar_3",
                    isAdmin = false,
                    depositPaid = 2000.0,
                    totalSpentShare = 3690.0
                ),
                GroupMember(
                    id = "m4",
                    name = "Rafiq",
                    avatar = "avatar_4",
                    isAdmin = false,
                    depositPaid = 4000.0,
                    totalSpentShare = 3690.0
                ),
                GroupMember(
                    id = "m5",
                    name = "Nayeem",
                    avatar = "avatar_5",
                    isAdmin = false,
                    depositPaid = 5000.0,
                    totalSpentShare = 3690.0
                )
            )

            val dailyExpenses = listOf(
                DailyExpenseEntry(
                    id = "e1",
                    date = "11 Sep 2026",
                    timestamp = System.currentTimeMillis() - 3600000 * 2,
                    spenderId = "m2",
                    spenderName = "Tanvir",
                    spenderAvatar = "avatar_2",
                    items = listOf(
                        ExpenseItem(name = "মিনিকেট চাল", quantity = "৫ কেজি", price = 350.0),
                        ExpenseItem(name = "ব্রয়লার মুরগি", quantity = "২ কেজি", price = 520.0),
                        ExpenseItem(name = "রূপচাঁদা সয়াবিন তেল", quantity = "২ লিটার", price = 380.0),
                        ExpenseItem(name = "আলু, পেঁয়াজ ও রসুন", quantity = "৩ কেজি", price = 160.0),
                        ExpenseItem(name = "কাঁচাবাজার ও শাকসবজি", quantity = "প্যাকেট", price = 240.0)
                    ),
                    totalAmount = 1650.0,
                    splitPerMember = 330.0,
                    notes = "আজকের দুপুরের ও রাতের রান্নার বাজার"
                ),
                DailyExpenseEntry(
                    id = "e1_b",
                    date = "11 Sep 2026",
                    timestamp = System.currentTimeMillis() - 3600000 * 5,
                    spenderId = "m3",
                    spenderName = "Sakib",
                    spenderAvatar = "avatar_3",
                    items = listOf(
                        ExpenseItem(name = "ফার্মের ডিম ও দুধ", quantity = "১ ডজন ও ১ লিটার", price = 270.0),
                        ExpenseItem(name = "বিস্কুট ও সকালের নাস্তা", quantity = "প্যাক", price = 180.0)
                    ),
                    totalAmount = 450.0,
                    splitPerMember = 90.0,
                    notes = "সকালের নাস্তা ও ডিম-দুধ"
                ),
                DailyExpenseEntry(
                    id = "e2",
                    date = "10 Sep 2026",
                    timestamp = System.currentTimeMillis() - 86400000L,
                    spenderId = "m1",
                    spenderName = "Faysal (You)",
                    spenderAvatar = "avatar_1",
                    items = listOf(
                        ExpenseItem(name = "ফার্মের ডিম", quantity = "২ ডজন", price = 320.0),
                        ExpenseItem(name = "দেশি মসুর ডাল", quantity = "২ কেজি", price = 280.0),
                        ExpenseItem(name = "তাজা রুই মাছ", quantity = "১.৫ কেজি", price = 450.0),
                        ExpenseItem(name = "ধনেপাতা, কাঁচামরিচ ও লেবু", quantity = "মিক্স", price = 150.0)
                    ),
                    totalAmount = 1200.0,
                    splitPerMember = 240.0,
                    notes = "শুক্রবারের স্পেশাল মাছ ও ডিম"
                ),
                DailyExpenseEntry(
                    id = "e3",
                    date = "08 Sep 2026",
                    timestamp = System.currentTimeMillis() - 86400000L * 3,
                    spenderId = "m4",
                    spenderName = "Rafiq",
                    spenderAvatar = "avatar_4",
                    items = listOf(
                        ExpenseItem(name = "গরুর মাংস", quantity = "১ কেজি", price = 800.0),
                        ExpenseItem(name = "চিনিগুঁড়া পোলাও চাল", quantity = "২ কেজি", price = 300.0),
                        ExpenseItem(name = "পেঁয়াজ, আদা ও রসুন বাটা", quantity = "প্যাক", price = 250.0),
                        ExpenseItem(name = "তীর সয়াবিন তেল", quantity = "১ লিটার", price = 190.0),
                        ExpenseItem(name = "মিষ্টি দই", quantity = "১ হাড়ি", price = 310.0),
                        ExpenseItem(name = "শসা, গাজর ও সালাদ", quantity = "মিক্স", price = 300.0)
                    ),
                    totalAmount = 2150.0,
                    splitPerMember = 430.0,
                    notes = "সবার জন্য স্পেশাল রান্না"
                ),
                DailyExpenseEntry(
                    id = "e4",
                    date = "05 Sep 2026",
                    timestamp = System.currentTimeMillis() - 86400000L * 6,
                    spenderId = "m5",
                    spenderName = "Nayeem",
                    spenderAvatar = "avatar_5",
                    items = listOf(
                        ExpenseItem(name = "নাজিরশাইল চাল", quantity = "৫ কেজি", price = 420.0),
                        ExpenseItem(name = "পাঙ্গাস মাছ", quantity = "২ কেজি", price = 440.0),
                        ExpenseItem(name = "আলু ও পটল", quantity = "৩ কেজি", price = 190.0),
                        ExpenseItem(name = "সয়াবিন তেল ও মশলা", quantity = "প্যাক", price = 400.0)
                    ),
                    totalAmount = 1450.0,
                    splitPerMember = 290.0,
                    notes = "সাপ্তাহিক বাজার ও নিত্যপ্রয়োজনীয় জিনিস"
                )
            )

            val mockDeposits = listOf(
                MemberDeposit(
                    id = "d1",
                    memberId = "m1",
                    memberName = "Faysal (You)",
                    memberAvatar = "avatar_1",
                    amount = 5000.0,
                    date = "01 Sep 2026",
                    paymentMethod = "bKash",
                    note = "মাসিক মেস ফান্ড জমা"
                ),
                MemberDeposit(
                    id = "d2",
                    memberId = "m2",
                    memberName = "Tanvir",
                    memberAvatar = "avatar_2",
                    amount = 5000.0,
                    date = "01 Sep 2026",
                    paymentMethod = "Bank",
                    note = "সেপ্টেম্বরের পুরো বাজেট"
                ),
                MemberDeposit(
                    id = "d3",
                    memberId = "m3",
                    memberName = "Sakib",
                    memberAvatar = "avatar_3",
                    amount = 2000.0,
                    date = "02 Sep 2026",
                    paymentMethod = "Cash",
                    note = "আংশিক জমা (বাকি ৩,০০০)"
                ),
                MemberDeposit(
                    id = "d4",
                    memberId = "m4",
                    memberName = "Rafiq",
                    memberAvatar = "avatar_4",
                    amount = 4000.0,
                    date = "02 Sep 2026",
                    paymentMethod = "Nagad",
                    note = "চলতি মাসের ফান্ড"
                ),
                MemberDeposit(
                    id = "d5",
                    memberId = "m5",
                    memberName = "Nayeem",
                    memberAvatar = "avatar_5",
                    amount = 5000.0,
                    date = "03 Sep 2026",
                    paymentMethod = "bKash",
                    note = "অগ্রিম মেস ফান্ড"
                )
            )

            return SharedGroup(
                id = "group_dhanmondi_4b",
                name = "ধানমন্ডি মেস ও ফ্ল্যাট",
                type = GroupType.MESS,
                description = "Flat 4B, Road 27",
                joinCode = "GRP-7842",
                monthName = "September 2026",
                monthlyTarget = 5000.0,
                members = members,
                dailyExpenses = dailyExpenses,
                deposits = mockDeposits,
                hasNewActivity = false
            )
        }

        private fun createTourGroup(): SharedGroup {
            val members = listOf(
                GroupMember(
                    id = "m1",
                    name = "Faysal (You)",
                    avatar = "avatar_1",
                    isAdmin = true,
                    depositPaid = 8000.0,
                    totalSpentShare = 6125.0
                ),
                GroupMember(
                    id = "m2",
                    name = "Tanvir",
                    avatar = "avatar_2",
                    depositPaid = 8000.0,
                    totalSpentShare = 6125.0
                ),
                GroupMember(
                    id = "m3",
                    name = "Sakib",
                    avatar = "avatar_3",
                    depositPaid = 8000.0,
                    totalSpentShare = 6125.0
                ),
                GroupMember(
                    id = "m4",
                    name = "Rafiq",
                    avatar = "avatar_4",
                    depositPaid = 8000.0,
                    totalSpentShare = 6125.0
                )
            )

            val dailyExpenses = listOf(
                DailyExpenseEntry(
                    id = "tour_e1",
                    date = "11 Sep 2026",
                    timestamp = System.currentTimeMillis() - 3600000,
                    spenderId = "m2",
                    spenderName = "Tanvir",
                    spenderAvatar = "avatar_2",
                    items = listOf(
                        ExpenseItem(name = "রিসোর্ট বুকিং (২ রাত)", quantity = "রুম", price = 12000.0),
                        ExpenseItem(name = "শ্রীমঙ্গল লোকাল জিপ ও জ্বালানি", quantity = "সারাদিন", price = 4500.0)
                    ),
                    totalAmount = 16500.0,
                    splitPerMember = 4125.0,
                    notes = "রিসোর্ট ও দর্শনীয় স্থানের গাড়ি রিজার্ভ"
                ),
                DailyExpenseEntry(
                    id = "tour_e2",
                    date = "10 Sep 2026",
                    timestamp = System.currentTimeMillis() - 86400000L,
                    spenderId = "m3",
                    spenderName = "Sakib",
                    spenderAvatar = "avatar_3",
                    items = listOf(
                        ExpenseItem(name = "জয়ন্তিকা এক্সপ্রেস ট্রেনের টিকিট", quantity = "৪ আসন", price = 3200.0),
                        ExpenseItem(name = "পানসী রেস্টুরেন্ট ডিনার ও চা", quantity = "রাত", price = 4800.0)
                    ),
                    totalAmount = 8000.0,
                    splitPerMember = 2000.0,
                    notes = "ট্রেন জার্নি ও স্পেশাল ডিনার"
                )
            )

            val deposits = listOf(
                MemberDeposit(id = "td1", memberId = "m1", memberName = "Faysal (You)", memberAvatar = "avatar_1", amount = 8000.0, date = "08 Sep 2026", paymentMethod = "bKash", note = "ট্যুর অ্যাডভান্স জমা"),
                MemberDeposit(id = "td2", memberId = "m2", memberName = "Tanvir", memberAvatar = "avatar_2", amount = 8000.0, date = "08 Sep 2026", paymentMethod = "Nagad", note = "ট্যুর অ্যাডভান্স জমা"),
                MemberDeposit(id = "td3", memberId = "m3", memberName = "Sakib", memberAvatar = "avatar_3", amount = 8000.0, date = "09 Sep 2026", paymentMethod = "Bank", note = "ট্যুর বাজেট জমা"),
                MemberDeposit(id = "td4", memberId = "m4", memberName = "Rafiq", memberAvatar = "avatar_4", amount = 8000.0, date = "09 Sep 2026", paymentMethod = "bKash", note = "ট্যুর ফান্ড জমা")
            )

            return SharedGroup(
                id = "group_sylhet_tour",
                name = "সিলেট ট্যুর ২০২৬",
                type = GroupType.TRIP,
                description = "Sylhet & Sreemangal 4-day Tour",
                joinCode = "GRP-3310",
                monthName = "September 2026",
                monthlyTarget = 8000.0,
                members = members,
                dailyExpenses = dailyExpenses,
                deposits = deposits,
                hasNewActivity = true // Shows new activity badge!
            )
        }

        private fun createFamilyGroup(): SharedGroup {
            val members = listOf(
                GroupMember(
                    id = "m1",
                    name = "Faysal (You)",
                    avatar = "avatar_1",
                    isAdmin = true,
                    depositPaid = 15000.0,
                    totalSpentShare = 8350.0
                ),
                GroupMember(
                    id = "m_abbu",
                    name = "আব্বু",
                    avatar = "avatar_4",
                    depositPaid = 25000.0,
                    totalSpentShare = 8350.0
                ),
                GroupMember(
                    id = "m_ammu",
                    name = "আম্মু",
                    avatar = "avatar_3",
                    depositPaid = 0.0,
                    totalSpentShare = 8350.0
                )
            )

            val dailyExpenses = listOf(
                DailyExpenseEntry(
                    id = "fam_e1",
                    date = "09 Sep 2026",
                    timestamp = System.currentTimeMillis() - 86400000L * 2,
                    spenderId = "m1",
                    spenderName = "Faysal (You)",
                    spenderAvatar = "avatar_1",
                    items = listOf(
                        ExpenseItem(name = "মাসিক মুদি ও চাল-ডাল", quantity = "৫০ কেজি", price = 8500.0),
                        ExpenseItem(name = "ডেসকো বিদ্যুৎ ও ওয়াসা বিল", quantity = "বিল", price = 4200.0)
                    ),
                    totalAmount = 12700.0,
                    splitPerMember = 4233.0,
                    notes = "মাসিক প্রয়োজনীয় জিনিস ও ইউটিলিটি বিল"
                )
            )

            val deposits = listOf(
                MemberDeposit(id = "fd1", memberId = "m1", memberName = "Faysal (You)", memberAvatar = "avatar_1", amount = 15000.0, date = "01 Sep 2026", paymentMethod = "Bank", note = "মাসিক সংসার খরচ"),
                MemberDeposit(id = "fd2", memberId = "m_abbu", memberName = "আব্বু", memberAvatar = "avatar_4", amount = 25000.0, date = "01 Sep 2026", paymentMethod = "Bank", note = "পেনশন ফান্ড জমা")
            )

            return SharedGroup(
                id = "group_family_budget",
                name = "আমাদের পরিবার",
                type = GroupType.FAMILY,
                description = "Monthly Household & Family Budget",
                joinCode = "GRP-9901",
                monthName = "September 2026",
                monthlyTarget = 15000.0,
                members = members,
                dailyExpenses = dailyExpenses,
                deposits = deposits,
                hasNewActivity = false
            )
        }
    }
}
