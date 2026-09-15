package com.centwise.core.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.features.group.SharedGroup

sealed class ScopeTabItem {
    abstract val id: String
    abstract val title: String
    abstract val icon: ImageVector

    data class Personal(
        override val id: String = "scope_personal",
        override val title: String,
        override val icon: ImageVector = Icons.Default.Person
    ) : ScopeTabItem()

    data class SpecificGroup(
        val group: SharedGroup,
        override val id: String = "scope_group_${group.id}",
        override val title: String = group.displayName,
        override val icon: ImageVector = group.type.vectorIcon
    ) : ScopeTabItem()
}

/**
 * Sleek modern fintech pill chip for switching scope at the top.
 * Exact same style as GroupChipItem in Group tab: direct on screen without white container card.
 */
@Composable
fun ScopeChipItem(
    item: ScopeTabItem,
    isActive: Boolean,
    accent: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val shortTitle = remember(item) {
        when (item) {
            is ScopeTabItem.Personal -> item.title
            is ScopeTabItem.SpecificGroup -> {
                val name = item.group.displayName.trim()
                if (name.startsWith("Our Family", ignoreCase = true)) {
                    "Family"
                } else {
                    name.split(" ").firstOrNull() ?: name
                }
            }
        }
    }

    val activePillBg = if (isDark) accent.copy(alpha = 0.22f) else accent.copy(alpha = 0.14f)
    val inactiveColor = if (isDark) {
        CentwiseColors.DarkTextSecondary.copy(alpha = 0.70f)
    } else {
        CentwiseColors.LightTextSecondary.copy(alpha = 0.70f)
    }

    Surface(
        shape = CircleShape,
        color = if (isActive) activePillBg else (if (isDark) CentwiseColors.DarkSurface else Color.White),
        shadowElevation = if (isActive) 0.dp else (if (isDark) 2.dp else 1.dp),
        border = if (isActive) null else BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0x14000000)),
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .iosBounceClick { onClick() }
    ) {
        Row(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .padding(horizontal = if (isActive) 14.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = shortTitle,
                tint = if (isActive) accent else inactiveColor,
                modifier = Modifier.size(18.dp)
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                       shrinkHorizontally(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Text(
                    text = shortTitle,
                    style = CentwiseTypography.Caption.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = accent,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
