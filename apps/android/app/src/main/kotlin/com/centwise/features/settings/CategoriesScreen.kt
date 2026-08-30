package com.centwise.features.settings

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.CategoryIconHelper
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.CategoryOption
import com.centwise.data.repository.TransactionRepository

@Composable
fun CategoriesScreen(
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryOption?>(null) }
    val categories by TransactionRepository.shared.categories.collectAsState()
    val systemCategories = categories.filter { it.isSystem }
    val customCategories = categories.filterNot { it.isSystem }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

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
            Text(text = "Categories", style = CentwiseTypography.Headline, color = textPrimary)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "System Categories",
                    style = CentwiseTypography.Headline,
                    color = textPrimary
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    systemCategories.forEachIndexed { index, category ->
                        CategoryRow(
                            category = category,
                            badge = "Default",
                            tint = accent,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = isDark
                        )
                        if (index < systemCategories.lastIndex) {
                            HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                        }
                    }
                }
            }

            if (customCategories.isNotEmpty()) {
                item {
                    Text(
                        "Custom Categories",
                        style = CentwiseTypography.Headline,
                        color = textPrimary
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        customCategories.forEachIndexed { index, category ->
                            CategoryRow(
                                category = category,
                                badge = null,
                                tint = accent,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                isDark = isDark,
                                onClick = { editingCategory = category }
                            )
                            if (index < customCategories.lastIndex) {
                                HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditCategorySheet(
            onDismiss = { showAddSheet = false },
            onSave = { category ->
                TransactionRepository.shared.insertCategory(category)
                showAddSheet = false
            },
            isDark = isDark
        )
    }

    editingCategory?.let { category ->
        AddEditCategorySheet(
            editingCategory = category,
            onDismiss = { editingCategory = null },
            onSave = { updated ->
                TransactionRepository.shared.updateCategory(updated)
                editingCategory = null
            },
            onDelete = {
                TransactionRepository.shared.deleteCategory(category.id)
                editingCategory = null
            },
            isDark = isDark
        )
    }
}

@Composable
private fun CategoryRow(
    category: CategoryOption,
    badge: String?,
    tint: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.iosBounceClick { onClick() } else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Clean unboxed category icon (width 28, height 28) matching iOS CategoriesScreen
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIconHelper.iconFor(category.name, category.icon),
                contentDescription = category.name,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = category.name,
            style = CentwiseTypography.Body.copy(fontWeight = FontWeight.Medium),
            color = textPrimary,
            modifier = Modifier.weight(1f)
        )

        if (badge != null) {
            Surface(
                shape = CircleShape,
                color = if (isDark) Color(0x1FFFFFFF) else Color(0x0F000000)
            ) {
                Text(
                    text = badge,
                    style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                    color = textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoriesScreenPreview() {
    CategoriesScreen()
}
