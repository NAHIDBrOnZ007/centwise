package com.centwise.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography

private data class FaqItem(val question: String, val answer: String)
private data class FaqGroup(val icon: ImageVector, val iconColor: Color, val title: String, val items: List<FaqItem>)

@Composable
fun FAQScreen(
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    val groups = listOf(
        FaqGroup(
            Icons.Default.ExpandMore,
            CentwiseColors.AccentBlue,
            "SMS Tracking",
            listOf(
                FaqItem(
                    "How does Centwise track my transactions?",
                    "Centwise reads bank and MFS SMS messages on your device and turns them into transactions automatically in the background."
                ),
                FaqItem(
                    "Which providers are supported?",
                    "Major Bangladeshi banks and MFS services including bKash, Nagad, Rocket, Upay, and leading bank cards."
                ),
                FaqItem(
                    "Why was an SMS not tracked?",
                    "OTP, promotional, and non-transaction messages are ignored on purpose. If a real transaction was missed, add it manually."
                ),
                FaqItem(
                    "Does it work with Bangla-language SMS?",
                    "Yes. Centwise parses both Bangla and English messages, including Bangla numerals."
                )
            )
        ),
        FaqGroup(
            Icons.Default.ExpandLess,
            CentwiseColors.IncomeGreen,
            "Privacy & Data",
            listOf(
                FaqItem(
                    "Is my financial data secure?",
                    "Yes. All parsing and storage happens on your device. Centwise has no servers and does not upload your SMS anywhere by default."
                ),
                FaqItem(
                    "Can I back up my data?",
                    "Yes. You can create a local backup and export transactions to CSV from Settings."
                ),
                FaqItem(
                    "How do I delete all my data?",
                    "Use the data deletion option in Settings. This permanently removes all transactions and accounts from the device."
                )
            )
        ),
        FaqGroup(
            Icons.Default.ExpandMore,
            CentwiseColors.RocketPurple,
            "Accounts & Providers",
            listOf(
                FaqItem(
                    "How are accounts detected?",
                    "Accounts are created automatically from SMS using the provider and the last four digits of your card or wallet."
                ),
                FaqItem(
                    "What is a manual account?",
                    "A manual account lets you track cash or anything without SMS. You update the balance yourself."
                ),
                FaqItem(
                    "Why does my balance look wrong?",
                    "Balances come from the balance mentioned in SMS. If a message is missing, add the transaction manually."
                )
            )
        ),
        FaqGroup(
            Icons.Default.ExpandMore,
            CentwiseColors.NagadOrange,
            "Budgets & Analytics",
            listOf(
                FaqItem(
                    "How do category budgets work?",
                    "Set a monthly limit for a category. Centwise tracks spending against it and warns you as you approach the limit."
                ),
                FaqItem(
                    "What are Smart Rules?",
                    "Rules automatically categorize transactions when the merchant name matches a keyword."
                )
            )
        )
    )

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
            Text(text = "FAQ", style = CentwiseTypography.Headline, color = textPrimary)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groups.forEach { group ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(group.iconColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = group.iconColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(group.title, style = CentwiseTypography.Headline, color = textPrimary)
                    }

                    group.items.forEach { item ->
                        FaqExpandableRow(
                            item = item,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accent = accent,
                            isDark = isDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FaqExpandableRow(
    item: FaqItem,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    isDark: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium))
            .background(if (isDark) Color(0x0FFFFFFF) else Color(0x0A000000))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.question,
                style = CentwiseTypography.Body,
                color = textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Text(
                text = item.answer,
                style = CentwiseTypography.Subheadline,
                color = textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FAQScreenPreview() {
    FAQScreen()
}
