package com.centwise.features.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.ReviewQueueItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.data.repository.ReviewQueueRepository
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewQueueScreen(
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = ReviewQueueRepository.shared
    val items by repository.items.collectAsState()
    var editingItem by remember { mutableStateOf<ReviewQueueItem?>(null) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarBackButton(onBackClick = onBackClick, isDark = isDark)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Review Queue",
                style = CentwiseTypography.Headline,
                color = textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (items.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${items.size} pending",
                        style = CentwiseTypography.Caption,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (items.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CentwiseColors.IncomeGreen.copy(alpha = 0.12f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                tint = CentwiseColors.IncomeGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Text(
                        text = "All Caught Up!",
                        style = CentwiseTypography.Headline,
                        color = textPrimary
                    )
                    Text(
                        text = "No pending SMS messages in your review queue. Financial SMS messages are automatically converted into transactions.",
                        style = CentwiseTypography.Body,
                        color = textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Button(
                        onClick = {
                            val hasReadSms = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.READ_SMS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                            if (hasReadSms) {
                                android.widget.Toast.makeText(context, "Scanning SMS inbox...", android.widget.Toast.LENGTH_SHORT).show()
                                coroutineScope.launch(Dispatchers.IO) {
                                    val result = com.centwise.core.scanner.HistoricalSmsScanner.scanInbox(context.applicationContext)
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Scan complete: Found ${result.transactionsImported} transactions",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                activity?.let {
                                    androidx.core.app.ActivityCompat.requestPermissions(
                                        it,
                                        arrayOf(android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS),
                                        1001
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan SMS Inbox",
                            style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ReviewQueueCard(
                        item = item,
                        cardBg = cardBg,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accent = accent,
                        isDark = isDark,
                        onDismiss = { repository.dismissItem(item.id) },
                        onEdit = { editingItem = item }
                    )
                }
            }
        }
    }

    // Modal Sheet for Converting/Editing
    editingItem?.let { item ->
        AddEditTransactionSheet(
            initialTransaction = TransactionItem(
                title = item.candidateParty ?: "${item.sender} Transaction",
                amount = item.candidateAmount ?: 0.0,
                type = item.candidateType ?: TransactionType.EXPENSE,
                category = "General",
                paymentMethod = item.sender,
                rawSms = item.rawSms,
                reference = item.reference
            ),
            onDismiss = { editingItem = null },
            onSave = { transaction ->
                repository.confirmAsTransaction(item, transaction)
            }
        )
    }
}

@Composable
private fun ReviewQueueCard(
    item: ReviewQueueItem,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .background(cardBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.sender,
                        style = CentwiseTypography.Caption,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = dateStr,
                    style = CentwiseTypography.Caption,
                    color = textSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isDark) Color(0x1FFFFFFF) else Color(0x0D000000)
            ) {
                Text(
                    text = item.reason,
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Raw SMS message
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDark) Color(0x14FFFFFF) else Color(0x08000000))
                .padding(12.dp)
        ) {
            Text(
                text = item.rawSms,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = textPrimary
            )
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dismiss", style = CentwiseTypography.Caption, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Convert to Tx", style = CentwiseTypography.Caption, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewQueueScreenPreview() {
    ReviewQueueScreen()
}
