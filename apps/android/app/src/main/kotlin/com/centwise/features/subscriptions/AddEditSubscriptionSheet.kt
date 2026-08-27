package com.centwise.features.subscriptions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.SubscriptionItem
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val billingCycles = listOf("Weekly", "Monthly", "Quarterly", "Yearly")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionSheet(
    initialSubscription: SubscriptionItem? = null,
    onDismiss: () -> Unit,
    onSave: (SubscriptionItem) -> Unit,
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
    var dueDateMillis by remember {
        mutableStateOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)
    }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val sheetBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val fieldBg = if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000)

    val isValid = name.isNotBlank() && (amountText.toDoubleOrNull() != null && amountText.toDoubleOrNull()!! > 0)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initialSubscription == null) "New Subscription" else "Edit Subscription",
                style = CentwiseTypography.Title2,
                color = textPrimary
            )

            // 1. Hero Amount Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(fieldBg)
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RECURRING AMOUNT (BDT)",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "৳ ",
                        style = CentwiseTypography.LargeTitle.copy(fontSize = 32.sp),
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                    BasicTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        textStyle = CentwiseTypography.LargeTitle.copy(
                            fontSize = 32.sp,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        cursorBrush = SolidColor(accent),
                        decorationBox = { innerTextField ->
                            if (amountText.isEmpty()) {
                                Text(
                                    text = "0.00",
                                    style = CentwiseTypography.LargeTitle.copy(
                                        fontSize = 32.sp,
                                        color = textSecondary.copy(alpha = 0.4f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // 2. Service Name Input
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Service Name (e.g. Netflix, Spotify, Chorki, WiFi)", color = textSecondary.copy(alpha = 0.6f)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                modifier = Modifier.fillMaxWidth(),
                textStyle = CentwiseTypography.Body
            )

            // 3. Billing Cycle Segmented Control
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Billing Cycle", style = CentwiseTypography.Headline.copy(fontSize = 13.sp), color = textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(fieldBg)
                        .padding(4.dp)
                ) {
                    billingCycles.forEach { cycle ->
                        val isSelected = billingCycle == cycle
                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) accent else Color.Transparent,
                            animationSpec = spring(),
                            label = "cycle_pill_bg"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(pillBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { billingCycle = cycle }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cycle,
                                style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                                color = if (isSelected) Color.White else textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Next Due Date Info Pill
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = fieldBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Next Renewal Date", style = CentwiseTypography.Body, color = textSecondary)
                    Text(dateFormat.format(Date(dueDateMillis)), style = CentwiseTypography.Headline, color = textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (isValid && amount > 0) {
                        onSave(
                            SubscriptionItem(
                                name = name.trim(),
                                amount = amount,
                                billingCycle = billingCycle,
                                nextBillingDate = dateFormat.format(Date(dueDateMillis))
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White,
                    disabledContainerColor = fieldBg
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialSubscription == null) "Save Subscription" else "Update Subscription",
                    style = CentwiseTypography.Headline,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddEditSubscriptionSheetPreview() {
    AddEditSubscriptionSheet(onDismiss = {}, onSave = {})
}
