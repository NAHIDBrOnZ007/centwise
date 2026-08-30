package com.centwise.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String
)

object SupportedCurrencies {
    val all = listOf(
        CurrencyInfo("BDT", "Bangladeshi Taka", "৳"),
        CurrencyInfo("USD", "US Dollar", "$"),
        CurrencyInfo("EUR", "Euro", "€"),
        CurrencyInfo("GBP", "British Pound", "£"),
        CurrencyInfo("INR", "Indian Rupee", "₹"),
        CurrencyInfo("JPY", "Japanese Yen", "¥"),
        CurrencyInfo("CNY", "Chinese Yuan", "¥"),
        CurrencyInfo("AUD", "Australian Dollar", "A$"),
        CurrencyInfo("CAD", "Canadian Dollar", "C$"),
        CurrencyInfo("SGD", "Singapore Dollar", "S$"),
        CurrencyInfo("MYR", "Malaysian Ringgit", "RM"),
        CurrencyInfo("SAR", "Saudi Riyal", "﷼"),
        CurrencyInfo("AED", "UAE Dirham", "د.إ"),
        CurrencyInfo("QAR", "Qatari Riyal", "﷼")
    )
}

object CurrencyPrefs {
    private const val PREFS_NAME = "centwise_settings"

    fun selectedCode(context: android.content.Context): String =
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getString("currencyCode", "BDT") ?: "BDT"

    fun setSelectedCode(context: android.content.Context, code: String) {
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit().putString("currencyCode", code).apply()
    }
}

@Composable
fun CurrencyPickerScreen(
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    var selectedCode by remember { mutableStateOf(CurrencyPrefs.selectedCode(context)) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val searchBg = if (isDark) CentwiseColors.DarkSearchBg else CentwiseColors.LightSearchBg

    val filtered = SupportedCurrencies.all.filter {
        it.code.lowercase().contains(searchText.lowercase()) ||
                it.name.lowercase().contains(searchText.lowercase())
    }

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
            Text(text = "Currency", style = CentwiseTypography.Headline, color = textPrimary)
        }

        // Search Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium))
                .background(searchBg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(18.dp)
            )
            BasicTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                textStyle = TextStyle(color = textPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (searchText.isEmpty()) {
                        Text("Search currency", style = CentwiseTypography.Subheadline, color = textSecondary)
                    }
                    innerTextField()
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { currency ->
                val isSelected = currency.code == selectedCode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium))
                        .background(cardBg)
                        .iosBounceClick {
                            selectedCode = currency.code
                            CurrencyPrefs.setSelectedCode(context, currency.code)
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = currency.symbol,
                        style = CentwiseTypography.Title2,
                        color = accent,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.12f)),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currency.code, style = CentwiseTypography.Body, color = textPrimary)
                        Text(currency.name, style = CentwiseTypography.Caption, color = textSecondary)
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = "No currencies found",
                        style = CentwiseTypography.Subheadline,
                        color = textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CurrencyPickerScreenPreview() {
    CurrencyPickerScreen()
}
