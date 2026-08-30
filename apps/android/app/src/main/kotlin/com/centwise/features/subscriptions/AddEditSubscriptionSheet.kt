package com.centwise.features.subscriptions

import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.SubscriptionItem
import com.centwise.features.accounts.providerIcon
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val billingCycles = listOf("Weekly", "Monthly", "Quarterly", "Yearly")
private val providers = listOf("bKash", "Nagad", "Rocket", "Upay", "BRAC Bank", "City Bank", "Cash")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionSheet(
    initialSubscription: SubscriptionItem? = null,
    onDismiss: () -> Unit,
    onSave: (SubscriptionItem) -> Unit,
    onDelete: (() -> Unit)? = null,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var name by remember { mutableStateOf(initialSubscription?.name ?: "") }
    var amountText by remember {
        mutableStateOf(
            if (initialSubscription != null && initialSubscription.amount > 0)
                if (initialSubscription.amount % 1.0 == 0.0) initialSubscription.amount.toLong().toString() else initialSubscription.amount.toString()
            else ""
        )
    }
    var billingCycle by remember { mutableStateOf(initialSubscription?.billingCycle ?: "Monthly") }
    var selectedProvider by remember { mutableStateOf("bKash") }
    var isActive by remember { mutableStateOf(initialSubscription?.isActive ?: true) }
    var dueDateMillis by remember {
        mutableStateOf(initialSubscription?.nextDueEpochMs?.takeIf { it > 0 } ?: (System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000))
    }

    var showCycleMenu by remember { mutableStateOf(false) }
    var showProviderMenu by remember { mutableStateOf(false) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else Color(0xFFF2F2F7)
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val menuBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val menuBorder = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x0F000000))

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = name.trim().isNotBlank() && parsedAmount > 0

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

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
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Navigation Top Bar (Rounded Pill Buttons for Cancel & Save)
            com.centwise.core.design.components.ModalSheetTopBar(
                title = if (initialSubscription == null) "New Subscription" else "Edit Subscription",
                onCancel = { dismissWithAnimation {} },
                onSave = {
                    if (isValid) {
                        val newSub = SubscriptionItem(
                            id = initialSubscription?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name.trim(),
                            amount = parsedAmount,
                            billingCycle = billingCycle,
                            nextBillingDate = dateFormat.format(Date(dueDateMillis)),
                            nextDueEpochMs = dueDateMillis,
                            isActive = isActive
                        )
                        dismissWithAnimation {
                            onSave(newSub)
                        }
                    }
                },
                saveEnabled = isValid,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDark = isDark
            )

            // 2. Subscription Info Form Card
            Column {
                Text(
                    text = "SUBSCRIPTION INFO",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    shadowElevation = if (isDark) 4.dp else 1.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Service name", color = textSecondary.copy(alpha = 0.5f), fontSize = 15.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            textStyle = CentwiseTypography.Body.copy(fontSize = 15.sp)
                        )

                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Amount (৳)", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                placeholder = { Text("0.00", color = textSecondary.copy(alpha = 0.5f), fontSize = 15.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                ),
                                modifier = Modifier.weight(1f),
                                textStyle = CentwiseTypography.Body.copy(
                                    fontSize = 15.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            )
                        }
                    }
                }
            }

            // 3. Billing Section Card (Cycle & Due Date)
            Column {
                Text(
                    text = "BILLING",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    shadowElevation = if (isDark) 4.dp else 1.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        // Cycle Picker Row
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCycleMenu = true }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cycle", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(billingCycle, style = CentwiseTypography.Body, color = textSecondary, fontSize = 15.sp)
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = textSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showCycleMenu,
                                onDismissRequest = { showCycleMenu = false },
                                offset = DpOffset(0.dp, 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = menuBg,
                                shadowElevation = 8.dp,
                                border = menuBorder
                            ) {
                                billingCycles.forEach { cycle ->
                                    val isSelected = billingCycle == cycle
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = cycle,
                                                style = CentwiseTypography.Body,
                                                color = textPrimary,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
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
                                        onClick = {
                                            billingCycle = cycle
                                            showCycleMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                        // Next Due Date Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Next due date", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)
                            Text(
                                text = dateFormat.format(Date(dueDateMillis)),
                                style = CentwiseTypography.Body,
                                color = textSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // 4. Paid From Section Card
            Column {
                Text(
                    text = "PAID FROM",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    shadowElevation = if (isDark) 4.dp else 1.dp
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProviderMenu = true }
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Account", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = providerIcon(selectedProvider),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(selectedProvider, style = CentwiseTypography.Body, color = textSecondary, fontSize = 15.sp)
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = textSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showProviderMenu,
                            onDismissRequest = { showProviderMenu = false },
                            offset = DpOffset(0.dp, 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            containerColor = menuBg,
                            shadowElevation = 8.dp,
                            border = menuBorder
                        ) {
                            providers.forEach { prov ->
                                val isSelected = selectedProvider == prov
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = providerIcon(prov),
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = prov,
                                                style = CentwiseTypography.Body,
                                                color = textPrimary,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
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
                                    onClick = {
                                        selectedProvider = prov
                                        showProviderMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Active Status Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                shadowElevation = if (isDark) 4.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active", style = CentwiseTypography.Body, color = textPrimary, fontSize = 15.sp)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = accent,
                            checkedThumbColor = Color.White
                        )
                    )
                }
            }

            // Destructive Delete Button
            if (onDelete != null && initialSubscription != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDelete()
                            onDismiss()
                        }
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "Delete Subscription",
                        style = CentwiseTypography.Headline,
                        fontWeight = FontWeight.SemiBold,
                        color = CentwiseColors.ExpenseRed,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditSubscriptionSheetPreview() {
    AddEditSubscriptionSheet(onDismiss = {}, onSave = {})
}
