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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
            Text(text = "Categories", style = CentwiseTypography.Headline, color = textPrimary)
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
                            tint = category.color,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        if (index < systemCategories.lastIndex) {
                            HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                        }
                    }
                }
            }

            item {
                Text(
                    "Custom Categories",
                    style = CentwiseTypography.Headline,
                    color = textPrimary
                )
            }

            if (customCategories.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No custom categories yet",
                            style = CentwiseTypography.Subheadline,
                            color = textSecondary
                        )
                        Text(
                            "Tap Add to create your own category",
                            style = CentwiseTypography.Caption,
                            color = textSecondary
                        )
                    }
                }
            } else {
                items(customCategories) { category ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                            .clickable { editingCategory = category }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        CategoryRow(
                            category = category,
                            badge = null,
                            tint = category.color,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        IconButton(onClick = { TransactionRepository.shared.deleteCategory(category.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CentwiseColors.ExpenseRed)
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditCategorySheet(
            onDismiss = { showAddSheet = false },
            onSave = { newCategory ->
                TransactionRepository.shared.insertCategory(newCategory)
                showAddSheet = false
            }
        )
    }

    editingCategory?.let { category ->
        AddEditCategorySheet(
            editingCategory = category,
            onDismiss = { editingCategory = null },
            onSave = { updated ->
                TransactionRepository.shared.updateCategory(updated)
                editingCategory = null
            }
        )
    }
}

@Composable
private fun CategoryRow(
    category: CategoryOption,
    badge: String?,
    tint: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = category.name.take(1), style = CentwiseTypography.Body, color = tint)
        }

        Text(
            text = category.name,
            style = CentwiseTypography.Body,
            color = textPrimary,
            modifier = Modifier.weight(1f)
        )

        if (badge != null) {
            Text(
                text = badge,
                style = CentwiseTypography.Caption,
                color = textSecondary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoriesScreenPreview() {
    CategoriesScreen()
}
