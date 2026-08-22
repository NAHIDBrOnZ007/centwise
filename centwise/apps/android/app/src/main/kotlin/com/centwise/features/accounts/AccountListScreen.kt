package com.centwise.features.accounts

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.fakes.FakeTransactionRepository
import com.centwise.data.models.AccountItem

fun providerColor(providerName: String): Color = when (providerName.lowercase()) {
    "bkash" -> CentwiseColors.BKashPink
    "nagad" -> CentwiseColors.NagadOrange
    "rocket" -> CentwiseColors.RocketPurple
    "upay" -> CentwiseColors.UpayBlue
    "city bank" -> CentwiseColors.CityBankRed
    "brac bank" -> CentwiseColors.BracBankBlue
    "cash" -> CentwiseColors.IncomeGreen
    else -> Color(0xFF64748B)
}

fun providerIcon(providerName: String): ImageVector = when (providerName.lowercase()) {
    "bkash", "nagad", "rocket", "upay" -> Icons.Default.PhoneAndroid
    "cash" -> Icons.Default.Savings
    else -> Icons.Default.AccountBalance
}

@Composable
fun AccountListScreen(
    onBackClick: () -> Unit = {},
    onAccountClick: (AccountItem) -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val repository = FakeTransactionRepository.shared
    val accounts by repository.accounts.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    val accent = com.centwise.features.settings.AccentOptions
        .byName(com.centwise.features.settings.AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    val totalBalance = accounts.sumOf { it.balance }

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
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = textPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onBackClick() }
                    .padding(10.dp)
            )
            Text(text = "Accounts & Wallets", style = CentwiseTypography.Headline, color = textPrimary)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Add",
                style = CentwiseTypography.Body,
                color = accent,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showAddSheet = true }
                    .padding(10.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Total Balance Hero
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(18.dp)
                ) {
                    Text("Total Connected Balance", style = CentwiseTypography.Subheadline, color = textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = CurrencyFormatter.formatBDT(totalBalance),
                        style = CentwiseTypography.HeroAmount,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "${accounts.size} accounts • Stored 100% on device",
                        style = CentwiseTypography.Caption,
                        color = textSecondary
                    )
                }
            }

            item {
                Text("Wallets & Bank Accounts", style = CentwiseTypography.Headline, color = textPrimary)
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    accounts.forEachIndexed { index, account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAccountClick(account) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val tint = providerColor(account.providerName)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(tint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = providerIcon(account.providerName),
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(account.name, style = CentwiseTypography.Body, color = textPrimary)
                                Text(
                                    text = "${account.type} • •• ${account.accountNumber.takeLast(4)}",
                                    style = CentwiseTypography.Caption,
                                    color = textSecondary
                                )
                            }

                            Text(
                                text = CurrencyFormatter.formatBDT(account.balance, compact = true),
                                style = CentwiseTypography.AmountMedium,
                                color = textPrimary
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = textSecondary
                            )
                        }

                        if (index < accounts.lastIndex) {
                            HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditAccountSheet(
            onDismiss = { showAddSheet = false },
            onSave = { account ->
                FakeTransactionRepository.shared.addAccount(account)
                showAddSheet = false
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AccountListScreenPreview() {
    AccountListScreen()
}
