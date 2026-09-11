package com.centwise.features.group

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.DismissKeyboardOnScroll
import com.centwise.core.design.components.ModalSheetTopBar
import com.centwise.core.design.components.clearFocusOnTapOutside
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.core.profile.UserPrefs
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.settings.LanguagePrefs
import kotlinx.coroutines.launch

data class EditableExpenseItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String = "",
    var quantity: String = "",
    var priceText: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    group: SharedGroup,
    onDismiss: () -> Unit,
    onSave: (spenderId: String, items: List<ExpenseItem>, notes: String) -> Unit,
    initialExpense: DailyExpenseEntry? = null,
    accent: Color = AccentOptions.byName(AppearancePrefs.accentName).color,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val haptic = LocalHapticFeedback.current
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val inputBg = if (isDark) Color(0xFF1E2633) else Color(0xFFF0F3F6)
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var selectedSpenderId by remember {
        mutableStateOf(initialExpense?.spenderId ?: group.members.firstOrNull()?.id ?: "")
    }

    val itemsList = remember {
        if (initialExpense != null && initialExpense.items.isNotEmpty()) {
            mutableStateListOf<EditableExpenseItem>().apply {
                addAll(initialExpense.items.map {
                    EditableExpenseItem(
                        name = it.name,
                        quantity = it.quantity,
                        priceText = if (it.price % 1.0 == 0.0) it.price.toInt().toString() else it.price.toString()
                    )
                })
            }
        } else {
            mutableStateListOf(
                EditableExpenseItem(name = if (LanguagePrefs.isBengali) "বাজার / খরচ" else "Groceries / Expense", quantity = "", priceText = ""),
                EditableExpenseItem(name = "", quantity = "", priceText = "")
            )
        }
    }

    var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }

    val quickSuggestions = when (group.type) {
        GroupType.FAMILY -> if (LanguagePrefs.isBengali)
            listOf("চাল ও ডাল", "মুরগি ও মাছ", "তেল ও মশলা", "শাকসবজি", "ডিম ও দুধ", "বিদ্যুৎ বিল", "গ্যাস সিলিন্ডার", "নাস্তা")
        else
            listOf("Groceries", "Chicken & Meat", "Oil & Spices", "Vegetables", "Eggs & Milk", "Electricity Bill", "Gas Bill", "Snacks")

        GroupType.TRIP -> if (LanguagePrefs.isBengali)
            listOf("হোটেল ভাড়া", "দুপুরের খাবার", "রাতের খাবার", "বাস / ট্রেন টিকিট", "গাড়ি ভাড়া", "স্ন্যাক্স ও চা", "এন্ট্রি ফি")
        else
            listOf("Hotel Stay", "Lunch", "Dinner", "Train/Bus Tickets", "Car Rental", "Snacks & Tea", "Entry Fee")

        else -> if (LanguagePrefs.isBengali)
            listOf("চাল", "মুরগি", "তেল", "ডিম", "মসুর ডাল", "রুই মাছ", "সবজি", "ওয়াইফাই বিল", "গ্যাস বিল")
        else
            listOf("Rice", "Chicken", "Cooking Oil", "Eggs", "Lentils", "Fish", "Vegetables", "WiFi Bill", "Gas Bill")
    }

    val validItems = itemsList.filter { it.name.isNotBlank() && (it.priceText.toDoubleOrNull() ?: 0.0) > 0 }
    val totalAmount = itemsList.sumOf { it.priceText.toDoubleOrNull() ?: 0.0 }
    val memberCount = if (group.members.isNotEmpty()) group.members.size else 1
    val splitPerMember = if (totalAmount > 0) totalAmount / memberCount else 0.0
    val canSave = validItems.isNotEmpty()

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bg,
        dragHandle = {
            Box(modifier = Modifier.clearFocusOnTapOutside()) {
                BottomSheetDefaults.DragHandle()
            }
        }
    ) {
        val scrollState = rememberScrollState()
        DismissKeyboardOnScroll(scrollState)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clearFocusOnTapOutside()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModalSheetTopBar(
                title = if (initialExpense != null) GroupStrings.editExpenseTitle else GroupStrings.addExpenseTitle,
                onCancel = { dismissWithAnimation {} },
                onSave = {
                    if (canSave) {
                        if (AppearancePrefs.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        val domainItems = validItems.map {
                            ExpenseItem(
                                name = it.name.trim(),
                                quantity = it.quantity.trim(),
                                price = it.priceText.toDoubleOrNull() ?: 0.0
                            )
                        }
                        dismissWithAnimation {
                            onSave(selectedSpenderId, domainItems, notes.trim())
                        }
                    }
                },
                saveEnabled = canSave,
                saveLabel = if (initialExpense != null) (if (LanguagePrefs.isBengali) "আপডেট" else "Update") else GroupStrings.save,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDark = isDark
            )

            // 1. Who Paid / Shopped
            Column {
                Text(
                    text = if (LanguagePrefs.isBengali) "কে খরচ করেছেন?" else "PAID BY",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    shadowElevation = if (isDark) 4.dp else 1.dp
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(group.members) { member ->
                            val isSelected = member.id == selectedSpenderId
                            val chipBg by animateColorAsState(
                                targetValue = if (isSelected) accent else if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000),
                                label = "spender_bg"
                            )
                            val textColor = if (isSelected) Color.White else textPrimary

                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(chipBg)
                                    .clickable {
                                        selectedSpenderId = member.id
                                        if (AppearancePrefs.hapticsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = UserPrefs.getAvatarResId(member.avatar)),
                                    contentDescription = member.name,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = member.name,
                                    style = CentwiseTypography.Subheadline.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick Item Suggestions
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (LanguagePrefs.isBengali) "ঝটপট আইটেম যোগ করুন" else "QUICK SUGGESTIONS",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickSuggestions) { suggestion ->
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000),
                            modifier = Modifier.clickable {
                                val emptyItem = itemsList.find { it.name.isBlank() }
                                if (emptyItem != null) {
                                    val idx = itemsList.indexOf(emptyItem)
                                    itemsList[idx] = emptyItem.copy(name = suggestion)
                                } else {
                                    itemsList.add(EditableExpenseItem(name = suggestion))
                                }
                                if (AppearancePrefs.hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = GroupItemIconHelper.iconForItem(suggestion),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "+ $suggestion",
                                    style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = accent
                                )
                            }
                        }
                    }
                }
            }

            // 3. Dynamic Items Table Card
            Column {
                Text(
                    text = if (LanguagePrefs.isBengali) "বাজার / খরচের তালিকা" else "EXPENSE ITEMS",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    shadowElevation = if (isDark) 4.dp else 1.dp
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = GroupStrings.expenseListTitle,
                                    style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                                    color = textPrimary
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = accent.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = GroupStrings.itemCountBadge(validItems.size),
                                    style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.Bold),
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        itemsList.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = GroupItemIconHelper.iconForItem(item.name),
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (item.name.isEmpty()) {
                                                Text(
                                                    text = GroupStrings.itemNamePlaceholder,
                                                    style = CentwiseTypography.Subheadline,
                                                    color = textSecondary.copy(alpha = 0.5f)
                                                )
                                            }
                                            BasicTextField(
                                                value = item.name,
                                                onValueChange = { newName ->
                                                    itemsList[index] = item.copy(name = newName)
                                                },
                                                textStyle = CentwiseTypography.Subheadline.copy(color = textPrimary),
                                                cursorBrush = SolidColor(accent),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                                    modifier = Modifier.weight(0.8f)
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                                        if (item.quantity.isEmpty()) {
                                            Text(
                                                text = GroupStrings.quantityPlaceholder,
                                                style = CentwiseTypography.Subheadline,
                                                color = textSecondary.copy(alpha = 0.5f)
                                            )
                                        }
                                        BasicTextField(
                                            value = item.quantity,
                                            onValueChange = { newQty ->
                                                itemsList[index] = item.copy(quantity = newQty)
                                            },
                                            textStyle = CentwiseTypography.Subheadline.copy(color = textPrimary),
                                            cursorBrush = SolidColor(accent),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0x14FFFFFF) else Color(0x08000000),
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                                        if (item.priceText.isEmpty()) {
                                            Text(
                                                text = GroupStrings.pricePlaceholder,
                                                style = CentwiseTypography.Subheadline,
                                                color = textSecondary.copy(alpha = 0.5f)
                                            )
                                        }
                                        BasicTextField(
                                            value = item.priceText,
                                            onValueChange = { newPrice ->
                                                val filtered = newPrice.filter { it.isDigit() || it == '.' }
                                                itemsList[index] = item.copy(priceText = filtered)
                                            },
                                            textStyle = CentwiseTypography.Subheadline.copy(
                                                color = accent,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            cursorBrush = SolidColor(accent),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                if (itemsList.size > 1) {
                                    IconButton(
                                        onClick = {
                                            itemsList.removeAt(index)
                                            if (AppearancePrefs.hapticsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = CentwiseColors.ExpenseRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    itemsList.add(EditableExpenseItem())
                                    if (AppearancePrefs.hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 11.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = GroupStrings.addAnotherItem,
                                    style = CentwiseTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                                    color = accent
                                )
                            }
                        }
                    }
                }
            }

            // 4. Live Calculation & Auto-Split Banner
            Column {
                Text(
                    text = if (LanguagePrefs.isBengali) "হিসাব ও অটো-স্প্লিট" else "CALCULATION & SPLIT",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (totalAmount > 0) accent.copy(alpha = 0.10f) else cardBg,
                    shadowElevation = if (isDark) 4.dp else 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = GroupStrings.totalExpenseAmount,
                                style = CentwiseTypography.Headline.copy(fontSize = 16.sp),
                                color = textPrimary
                            )
                            Text(
                                text = CurrencyFormatter.formatBDT(totalAmount, useBengaliNumerals = LanguagePrefs.isBengali),
                                style = CentwiseTypography.Title2.copy(fontWeight = FontWeight.Bold),
                                color = if (totalAmount > 0) accent else textSecondary
                            )
                        }

                        HorizontalDivider(
                            color = if (totalAmount > 0) accent.copy(alpha = 0.2f) else if (isDark) Color(0x14FFFFFF) else Color(0x0A000000),
                            thickness = 0.8.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = GroupStrings.perMemberSplitLabel(memberCount),
                                style = CentwiseTypography.Caption,
                                color = textSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatBDT(splitPerMember, useBengaliNumerals = LanguagePrefs.isBengali) + GroupStrings.perPersonSuffix,
                                style = CentwiseTypography.Subheadline.copy(fontWeight = FontWeight.Bold),
                                color = if (splitPerMember > 0) accent else textSecondary
                            )
                        }
                    }
                }
            }

            // 5. Notes (Optional)
            Column {
                Text(
                    text = if (LanguagePrefs.isBengali) "নোট (ঐচ্ছিক)" else "NOTES (OPTIONAL)",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    shadowElevation = if (isDark) 4.dp else 1.dp
                ) {
                    TextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = {
                            Text(
                                GroupStrings.optionalNotesPlaceholder,
                                color = textSecondary.copy(alpha = 0.5f),
                                fontSize = 15.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        textStyle = CentwiseTypography.Body.copy(fontSize = 15.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
