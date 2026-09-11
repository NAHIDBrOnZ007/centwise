package com.centwise.features.group

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.centwise.features.settings.LanguagePrefs
import java.util.UUID

enum class GroupType(val titleBn: String, val icon: String, val vectorIcon: ImageVector) {
    MESS("মেস / ব্যাচেলর", "🏢", Icons.Default.Apartment),
    FAMILY("পারিবারিক সংসার", "🏠", Icons.Default.Home),
    FLATMATES("ফ্ল্যাটমেট ও রুম", "🛋️", Icons.Default.Weekend),
    TRIP("ট্যুর ও ভ্রমণ", "✈️", Icons.Default.Flight),
    OTHER("অন্যান্য গ্রুপ", "👥", Icons.Default.Groups);

    val titleEn: String
        get() = when (this) {
            MESS -> "Bachelor Mess"
            FAMILY -> "Family Household"
            FLATMATES -> "Roommates Living"
            TRIP -> "Tour & Trip"
            OTHER -> "Shared Group"
        }

    val displayTitle: String
        get() = if (LanguagePrefs.isBengali) titleBn else titleEn
}

object GroupItemIconHelper {
    fun iconForItem(name: String): ImageVector {
        val lower = name.trim().lowercase()
        return when {
            // Rice & Grains
            lower.contains("চাল") || lower.contains("rice") || lower.contains("পোলাও") ||
                    lower.contains("ডাল") || lower.contains("lentil") || lower.contains("খিচুড়ি") ||
                    lower.contains("আটা") || lower.contains("ময়দা") || lower.contains("flour") -> Icons.Default.Restaurant

            // Meat & Poultry
            lower.contains("মুরগি") || lower.contains("chicken") || lower.contains("মাংস") ||
                    lower.contains("meat") || lower.contains("beef") || lower.contains("mutton") ||
                    lower.contains("খাসি") || lower.contains("গরু") || lower.contains("খাসির") ||
                    lower.contains("গরুর") -> Icons.Default.Restaurant

            // Fish & Seafood
            lower.contains("মাছ") || lower.contains("fish") || lower.contains("রুই") ||
                    lower.contains("পাঙ্গাস") || lower.contains("ইলিশ") || lower.contains("চিংড়ি") ||
                    lower.contains("কাতলা") || lower.contains("তেলাপিয়া") -> Icons.Default.SetMeal

            // Vegetables & Greens
            lower.contains("সবজি") || lower.contains("শাক") || lower.contains("vegetable") ||
                    lower.contains("আলু") || lower.contains("potato") || lower.contains("পটল") ||
                    lower.contains("পেঁয়াজ") || lower.contains("onion") || lower.contains("রসুন") ||
                    lower.contains("garlic") || lower.contains("আদা") || lower.contains("ginger") ||
                    lower.contains("টমেটো") || lower.contains("tomato") || lower.contains("কাঁচামরিচ") ||
                    lower.contains("মরিচ") || lower.contains("chili") || lower.contains("সালাদ") ||
                    lower.contains("salad") || lower.contains("শসা") || lower.contains("লেবু") -> Icons.Default.Eco

            // Oil, Ghee & Spices
            lower.contains("তেল") || lower.contains("oil") || lower.contains("ঘি") ||
                    lower.contains("ghee") || lower.contains("সয়াবিন") || lower.contains("সরিষা") -> Icons.Default.Opacity
            lower.contains("মশলা") || lower.contains("spice") || lower.contains("হলুদ") ||
                    lower.contains("জিরা") || lower.contains("ধনে") || lower.contains("লবণ") ||
                    lower.contains("salt") -> Icons.Default.Kitchen

            // Eggs & Dairy
            lower.contains("ডিম") || lower.contains("egg") -> Icons.Default.Egg
            lower.contains("দুধ") || lower.contains("milk") || lower.contains("দই") ||
                    lower.contains("yogurt") || lower.contains("ছানা") || lower.contains("পনির") ||
                    lower.contains("cheese") || lower.contains("মাখন") || lower.contains("butter") -> Icons.Default.LocalDrink

            // Bills & Utilities
            lower.contains("বিদ্যুৎ") || lower.contains("electric") || lower.contains("কারেন্ট") ||
                    lower.contains("power") -> Icons.Default.Bolt
            lower.contains("গ্যাস") || lower.contains("gas") || lower.contains("সিলিন্ডার") ||
                    lower.contains("cylinder") -> Icons.Default.LocalFireDepartment
            lower.contains("ওয়াইফাই") || lower.contains("wifi") || lower.contains("ইন্টারনেট") ||
                    lower.contains("internet") || lower.contains("নেট") || lower.contains("ব্রডব্যান্ড") -> Icons.Default.Wifi
            lower.contains("পানি") || lower.contains("water") || lower.contains("ওয়াসা") ||
                    lower.contains("wasa") -> Icons.Default.WaterDrop

            // Snacks, Breakfast & Tea
            lower.contains("নাস্তা") || lower.contains("snack") || lower.contains("চা") ||
                    lower.contains("tea") || lower.contains("কফি") || lower.contains("coffee") ||
                    lower.contains("বিস্কুট") || lower.contains("biscuit") || lower.contains("রুটি") ||
                    lower.contains("bread") || lower.contains("পরোটা") || lower.contains("মিষ্টি") ||
                    lower.contains("sweet") || lower.contains("ফল") || lower.contains("fruit") -> Icons.Default.Coffee

            // Cleaning & Household essentials
            lower.contains("সাবান") || lower.contains("soap") || lower.contains("ডিটারজেন্ট") ||
                    lower.contains("detergent") || lower.contains("হুইল") || lower.contains("সার্ফ") ||
                    lower.contains("শ্যাম্পু") || lower.contains("shampoo") || lower.contains("পেস্ট") ||
                    lower.contains("paste") || lower.contains("হারপিক") || lower.contains("টিস্যু") ||
                    lower.contains("tissue") -> Icons.Default.CleaningServices

            // Transport & Travel
            lower.contains("বাস") || lower.contains("bus") || lower.contains("গাড়ি") ||
                    lower.contains("car") || lower.contains("সিএনজি") || lower.contains("cng") ||
                    lower.contains("রিকশা") || lower.contains("rickshaw") || lower.contains("ভাড়া") ||
                    lower.contains("fare") -> Icons.Default.DirectionsBus
            lower.contains("ট্রেন") || lower.contains("train") || lower.contains("টিকিট") ||
                    lower.contains("ticket") -> Icons.Default.ConfirmationNumber
            lower.contains("ফ্লাইট") || lower.contains("flight") || lower.contains("বিমান") ||
                    lower.contains("plane") -> Icons.Default.Flight
            lower.contains("হোটেল") || lower.contains("hotel") || lower.contains("রিসোর্ট") ||
                    lower.contains("resort") || lower.contains("কটেজ") || lower.contains("stay") -> Icons.Default.Hotel

            // Default Groceries / Shopping Bag
            else -> Icons.Default.ShoppingBag
        }
    }
}

data class ExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: String = "",
    val price: Double
) {
    val displayName: String
        get() = if (!LanguagePrefs.isBengali) {
            when (name) {
                "মিনিকেট চাল" -> "Miniket Rice"
                "ব্রয়লার মুরগি" -> "Broiler Chicken"
                "রূপচাঁদা সয়াবিন তেল" -> "Rupchanda Soybean Oil"
                "আলু, পেঁয়াজ ও রসুন" -> "Potatoes, Onions & Garlic"
                "কাঁচাবাজার ও শাকসবজি" -> "Fresh Vegetables & Greens"
                "গরুর মাংস" -> "Beef Meat"
                "পোলাও চাল" -> "Polao Rice"
                "মসুর ডাল ও মশলা" -> "Red Lentils & Spices"
                "খাসির মাংস" -> "Mutton Meat"
                "বাসমতী চাল" -> "Basmati Rice"
                "টক দই ও ঘি" -> "Yogurt & Ghee"
                "সালাদ ও মসলাপাতি" -> "Salad & Spices"
                "নাজিরশাইল চাল" -> "Nazirshail Rice"
                "পাঙ্গাস মাছ" -> "Pangas Fish"
                "আলু ও পটল" -> "Potatoes & Pointed Gourd"
                "সয়াবিন তেল ও মশলা" -> "Cooking Oil & Spices"
                else -> name
            }
        } else name

    val displayQuantity: String
        get() = if (!LanguagePrefs.isBengali) {
            quantity.replace("কেজি", "kg")
                .replace("লিটার", "L")
                .replace("প্যাকেট", "pack")
                .replace("প্যাক", "pack")
                .replace("৫", "5")
                .replace("২", "2")
                .replace("৩", "3")
                .replace("১", "1")
        } else quantity
}

data class DailyExpenseEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val spenderId: String,
    val spenderName: String,
    val spenderAvatar: String = "avatar_1",
    val items: List<ExpenseItem>,
    val totalAmount: Double = items.sumOf { it.price },
    val splitPerMember: Double = 0.0,
    val notes: String = ""
) {
    val displayNotes: String
        get() = if (!LanguagePrefs.isBengali) {
            when (notes) {
                "আজকের দুপুরের ও রাতের রান্নার বাজার" -> "Lunch and dinner cooking groceries"
                "শুক্রবার ছুটির দিনের দাওয়াত ও স্পেশাল বাজার" -> "Friday holiday feast & special groceries"
                "সবার জন্য স্পেশাল রান্না" -> "Special feast dinner for members"
                "সাপ্তাহিক বাজার ও নিত্যপ্রয়োজনীয় জিনিস" -> "Weekly essentials and groceries"
                else -> notes
            }
        } else notes
}

data class DayExpenseGroup(
    val date: String,
    val entries: List<DailyExpenseEntry>
) {
    val id: String get() = "day_$date"
    val totalAmount: Double get() = entries.sumOf { it.totalAmount }
    val allItems: List<ExpenseItem> get() = entries.flatMap { it.items }
    val totalItemCount: Int get() = allItems.size
    val spenders: List<String> get() = entries.map { it.spenderName }.distinct()

    val smartTitle: String
        get() {
            return if (allItems.isNotEmpty()) {
                val names = allItems.map { it.displayName }.filter { it.isNotBlank() }.distinct()
                if (names.size <= 3) {
                    names.joinToString(", ")
                } else {
                    names.take(3).joinToString(", ") + " +${names.size - 3}"
                }
            } else {
                val notesList = entries.mapNotNull { it.displayNotes.takeIf { n -> n.isNotBlank() } }
                if (notesList.isNotEmpty()) {
                    notesList.joinToString(", ")
                } else {
                    if (LanguagePrefs.isBengali) "দৈনিক বাজার খরচ" else "Daily Group Expense"
                }
            }
        }

    val displayDate: String
        get() = if (LanguagePrefs.isBengali) {
            date.replace("Sep", "সেপ্টেম্বর")
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
        } else date
}

data class MemberDeposit(
    val id: String = UUID.randomUUID().toString(),
    val memberId: String,
    val memberName: String,
    val memberAvatar: String = "avatar_1",
    val amount: Double,
    val date: String,
    val paymentMethod: String = "Cash",
    val note: String = ""
)

data class GroupMember(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val avatar: String = "avatar_1",
    val phone: String = "",
    val isAdmin: Boolean = false,
    val depositPaid: Double = 0.0,
    val totalSpentShare: Double = 0.0
) {
    val balance: Double
        get() = depositPaid - totalSpentShare
}

data class SharedGroup(
    val id: String = "group_dhanmondi_4b",
    val name: String = "ধানমন্ডি মেস ও ফ্ল্যাট",
    val type: GroupType = GroupType.MESS,
    val description: String = "Flat 4B, Road 27",
    val joinCode: String = "GRP-7842",
    val monthName: String = "September 2026",
    val monthlyTarget: Double = 5000.0,
    val members: List<GroupMember> = emptyList(),
    val dailyExpenses: List<DailyExpenseEntry> = emptyList(),
    val deposits: List<MemberDeposit> = emptyList(),
    val hasNewActivity: Boolean = false
) {
    val displayName: String
        get() = if (!LanguagePrefs.isBengali) {
            when (name) {
                "ধানমন্ডি মেস ও ফ্ল্যাট", "ধানমন্ডি ফ্ল্যাট ও মেস" -> "Dhanmondi Flat & Mess"
                "সিলেট ট্যুর ২০২৬", "সিলেট ট্যুর", "Sylhet Tour" -> "Sylhet Tour 2026"
                "আমাদের পরিবার", "ফ্যামিলি বাজেট", "Family Budget" -> "Our Family Budget"
                else -> name
            }
        } else name

    val userBalance: Double
        get() {
            val userMember = members.firstOrNull { it.name.contains("You") || it.name.contains("আপনি") || it.id == "m1" }
                ?: members.firstOrNull()
            return userMember?.balance ?: 0.0
        }

    val activeSpenderIdsToday: Set<String>
        get() {
            val latestDate = dailyExpenses.firstOrNull()?.date ?: return emptySet()
            return dailyExpenses.filter { it.date == latestDate }.map { it.spenderId }.toSet()
        }

    val groupedDailyExpenses: List<DayExpenseGroup>
        get() {
            // Group by date preserving order of recent occurrences
            val map = linkedMapOf<String, MutableList<DailyExpenseEntry>>()
            dailyExpenses.forEach { entry ->
                map.getOrPut(entry.date) { mutableListOf() }.add(entry)
            }
            return map.map { (date, entries) -> DayExpenseGroup(date = date, entries = entries) }
        }

    val totalDeposits: Double
        get() = members.sumOf { it.depositPaid }

    val totalExpense: Double
        get() = dailyExpenses.sumOf { it.totalAmount }

    val cashInHand: Double
        get() = totalDeposits - totalExpense

    val averageDailyCost: Double
        get() = if (dailyExpenses.isNotEmpty()) totalExpense / dailyExpenses.size else 0.0

    val weeklyExpense: Double
        get() = if (currentWeekExpense > 0) currentWeekExpense else averageDailyCost * 7

    val currentWeekExpense: Double
        get() {
            val breakdown = weeklyBreakdown()
            // Return current week's total (Week 2 for Sep 8-14) or max active week
            return breakdown.getOrNull(1)?.second ?: (averageDailyCost * 7)
        }

    fun weeklyBreakdown(): List<Pair<String, Double>> {
        var w1 = 0.0
        var w2 = 0.0
        var w3 = 0.0
        var w4 = 0.0

        dailyExpenses.forEach { entry ->
            val day = entry.date.trim().split(" ").firstOrNull()?.toIntOrNull() ?: 1
            when (day) {
                in 1..7 -> w1 += entry.totalAmount
                in 8..14 -> w2 += entry.totalAmount
                in 15..21 -> w3 += entry.totalAmount
                else -> w4 += entry.totalAmount
            }
        }
        return listOf(
            (if (LanguagePrefs.isBengali) "১ম সপ্তাহ (১-৭)" else "Week 1 (1-7)") to w1,
            (if (LanguagePrefs.isBengali) "২য় সপ্তাহ (৮-১৪)" else "Week 2 (8-14)") to w2,
            (if (LanguagePrefs.isBengali) "৩য় সপ্তাহ (১৫-২১)" else "Week 3 (15-21)") to w3,
            (if (LanguagePrefs.isBengali) "৪র্থ সপ্তাহ (২২+)" else "Week 4 (22+)") to w4
        )
    }
}
