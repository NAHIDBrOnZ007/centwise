package com.centwise.core.design.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.AccountItem

@Composable
fun AccountCarousel(
    accounts: List<AccountItem>,
    onAccountClick: (AccountItem) -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Accounts",
            style = CentwiseTypography.Headline,
            color = textPrimary
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
        ) {
            items(accounts, key = { it.id }) { account ->
                AccountCarouselCard(
                    account = account,
                    onClick = { onAccountClick(account) },
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
private fun AccountCarouselCard(
    account: AccountItem,
    onClick: () -> Unit,
    isDark: Boolean
) {
    var isAmountHidden by remember { mutableStateOf(true) }

    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val (icon, typeLabel) = when {
        account.type.contains("Credit", ignoreCase = true) -> Icons.Default.CreditCard to "Credit"
        account.type.contains("MFS", ignoreCase = true) || account.type.contains("Wallet", ignoreCase = true) -> Icons.Default.PhoneAndroid to "Savings"
        account.type.contains("Bank", ignoreCase = true) -> Icons.Default.AccountBalance to "Bank"
        else -> Icons.Default.Savings to "Savings"
    }

    Surface(
        modifier = Modifier
            .width(156.dp)
            .height(130.dp)
            .shadow(
                elevation = if (isDark) 4.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = cardBg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = typeLabel,
                        style = CentwiseTypography.Caption,
                        color = textSecondary,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = account.name,
                    style = CentwiseTypography.Body,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1
                )

                if (account.accountNumber.isNotEmpty()) {
                    val masked = if (account.accountNumber.length > 4) "••" + account.accountNumber.takeLast(4) else account.accountNumber
                    Text(
                        text = masked,
                        style = CentwiseTypography.Caption,
                        color = textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = isAmountHidden,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "amountAnimation"
                ) { hidden ->
                    Text(
                        text = if (hidden) "••••••" else CurrencyFormatter.format(account.balance),
                        style = CentwiseTypography.Headline,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = { isAmountHidden = !isAmountHidden },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isAmountHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Balance",
                        tint = textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
