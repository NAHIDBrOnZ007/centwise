package com.centwise.core.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.features.transactions.TransactionSortOrder

/**
 * Idiomatic Jetpack Compose Action Pill Group matching iOS TransactionListView 2-button toolbar (More Menu + Add Button).
 */
@Composable
fun ActionPillGroup(
    onAddClick: () -> Unit,
    currentSortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    onSortSelected: (TransactionSortOrder) -> Unit = {},
    onExportClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val pillBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val menuBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val menuBorder = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x0F000000))
    val accent = com.centwise.features.settings.AccentOptions.byName(com.centwise.features.settings.AppearancePrefs.accentName).color

    var showMoreMenu by remember { mutableStateOf(false) }

    Surface(
        shape = CircleShape,
        color = pillBg,
        shadowElevation = if (isDark) 4.dp else 1.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. More Actions Button (Sort + Export Menu Matching iOS ellipsis.circle)
            Box {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .iosBounceClick { showMoreMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Actions",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    offset = DpOffset(0.dp, 8.dp),
                    shape = RoundedCornerShape(18.dp),
                    containerColor = menuBg,
                    shadowElevation = 8.dp,
                    border = menuBorder,
                    modifier = Modifier.heightIn(max = 440.dp)
                ) {
                    // Sort options header
                    Text(
                        text = "SORT BY",
                        style = CentwiseTypography.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    TransactionSortOrder.entries.forEach { order ->
                        val isSelected = currentSortOrder == order
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = order.displayName,
                                    style = CentwiseTypography.Body,
                                    color = textPrimary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp
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
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            onClick = {
                                onSortSelected(order)
                                showMoreMenu = false
                            }
                        )
                    }

                    HorizontalDivider(
                        color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Export option
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = {
                            Text(
                                text = "Export CSV",
                                style = CentwiseTypography.Body,
                                color = textPrimary,
                                fontSize = 14.sp
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        onClick = {
                            showMoreMenu = false
                            onExportClick()
                        }
                    )
                }
            }

            // 2. Add Transaction Button (+)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .iosBounceClick { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActionPillGroupPreview() {
    ActionPillGroup(
        onAddClick = {}
    )
}
