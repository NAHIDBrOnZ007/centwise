package com.centwise.features.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.SubscriptionItem
import com.centwise.data.repository.TransactionRepository
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

@Composable
fun SubscriptionsScreen(
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val repository = TransactionRepository.shared
    val subscriptions by repository.subscriptions.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<SubscriptionItem?>(null) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    val totalMonthly = subscriptions.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarBackButton(onBackClick = onBackClick, isDark = isDark)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Subscriptions", style = CentwiseTypography.Headline, color = textPrimary)
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = accent,
                modifier = Modifier
                    .clip(CircleShape)
                    .iosBounceClick { showAddSheet = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Add",
                        style = CentwiseTypography.Headline.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Total Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(18.dp)
                ) {
                    Text("Monthly Recurring Bills", style = CentwiseTypography.Subheadline, color = textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = CurrencyFormatter.formatBDT(totalMonthly),
                        style = CentwiseTypography.HeroAmount,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "${subscriptions.size} Active Subscriptions",
                        style = CentwiseTypography.Caption,
                        color = textSecondary
                    )
                }
            }

            item {
                Text("Your Subscriptions", style = CentwiseTypography.Headline, color = textPrimary)
            }

            if (subscriptions.isEmpty()) {
                item {
                    Text(
                        text = "No active subscriptions.",
                        style = CentwiseTypography.Subheadline,
                        color = textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(subscriptions, key = { it.id }) { subscription ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                            .clickable { editingSubscription = subscription }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subscriptions,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(subscription.name, style = CentwiseTypography.Body, color = textPrimary)
                            Text(
                                "Due on ${subscription.nextBillingDate}",
                                style = CentwiseTypography.Caption,
                                color = textSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.formatBDT(subscription.amount, compact = true),
                                style = CentwiseTypography.AmountMedium,
                                color = textPrimary
                            )
                            Text(
                                subscription.billingCycle,
                                style = CentwiseTypography.Caption,
                                color = textSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFFC7C7CC),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditSubscriptionSheet(
            onDismiss = { showAddSheet = false },
            onSave = { subscription ->
                repository.addSubscription(subscription)
                showAddSheet = false
            },
            isDark = isDark
        )
    }

    editingSubscription?.let { sub ->
        AddEditSubscriptionSheet(
            initialSubscription = sub,
            onDismiss = { editingSubscription = null },
            onSave = { updated ->
                repository.updateSubscription(updated)
                editingSubscription = null
            },
            onDelete = {
                repository.deleteSubscription(sub.id)
                editingSubscription = null
            },
            isDark = isDark
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SubscriptionsScreenPreview() {
    SubscriptionsScreen()
}
