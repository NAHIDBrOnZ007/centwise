package com.centwise.features.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.data.repository.TransactionRepository
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val repository = TransactionRepository.shared

    val transactions by repository.transactions.collectAsState()
    val accounts by repository.accounts.collectAsState()
    val budgets by repository.budgets.collectAsState()
    val subscriptions by repository.subscriptions.collectAsState()

    var showLoadDemoDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val dbFile = context.noBackupFilesDir.resolve("centwise.db")
    val dbSizeString = if (dbFile.exists()) {
        val bytes = dbFile.length()
        val kb = bytes / 1024.0
        if (kb < 1024) String.format("%.1f KB", kb) else String.format("%.2f MB", kb / 1024.0)
    } else "Clean DB"

    if (showLoadDemoDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDemoDialog = false },
            title = { Text("Load Demo Sample Data?", color = textPrimary) },
            text = {
                Text(
                    "This will populate your database with realistic sample transactions, accounts, budgets, and subscriptions for previewing Centwise.",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLoadDemoDialog = false
                        val summary = CentwiseRustBackend.loadDemoData()
                        val message = if (summary != null) {
                            repository.clearLegacyStorage()
                            repository.loadFromRust()
                            "Rust demo data loaded: ${summary.transactions} transactions"
                        } else {
                            "Rust core is unavailable; demo data was not loaded"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Load Demo Data", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoadDemoDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Wipe Database & Reset?", color = textPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete all transactions, budgets, and subscriptions? This action cannot be undone.",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        repository.resetToEmptyDatabase()
                        Toast.makeText(context, "Database wiped. Starting clean.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Wipe Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data & Storage", color = textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        },
        containerColor = bg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = CentwiseSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Database Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CentwiseColors.IncomeGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Database",
                                tint = CentwiseColors.IncomeGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Local Storage",
                                    style = CentwiseTypography.Headline,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CentwiseColors.IncomeGreen)
                                )
                            }
                            Text(
                                text = "Encrypted On-Device Database",
                                style = CentwiseTypography.Caption,
                                color = textSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = textSecondary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = dbSizeString,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                                color = textPrimary
                            )
                        }
                    }
                }
            }

            // Data Records Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "CURRENT DATABASE RECORDS",
                            style = CentwiseTypography.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = textSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricPill(
                                modifier = Modifier.weight(1f),
                                title = "Transactions",
                                count = "${transactions.size}",
                                icon = Icons.Default.ReceiptLong,
                                color = CentwiseColors.PrimaryEmerald,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                            MetricPill(
                                modifier = Modifier.weight(1f),
                                title = "Accounts",
                                count = "${accounts.size}",
                                icon = Icons.Default.AccountBalance,
                                color = Color(0xFF007AFF),
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricPill(
                                modifier = Modifier.weight(1f),
                                title = "Budgets",
                                count = "${budgets.size}",
                                icon = Icons.Default.PieChart,
                                color = Color(0xFFFF9500),
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                            MetricPill(
                                modifier = Modifier.weight(1f),
                                title = "Subscriptions",
                                count = "${subscriptions.size}",
                                icon = Icons.Default.Subscriptions,
                                color = Color(0xFF5856D6),
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }
                    }
                }
            }

            // Actions Section
            item {
                Text(
                    text = "DATA & BACKUP OPTIONS",
                    style = CentwiseTypography.Caption.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = textSecondary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                ) {
                    ActionRow(
                        icon = Icons.Default.AutoAwesome,
                        iconColor = accent,
                        title = "Load Demo Sample Data",
                        subtitle = "Populate realistic transactions for testing",
                        onClick = { showLoadDemoDialog = true },
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )

                    HorizontalDivider(color = textSecondary.copy(alpha = 0.12f), modifier = Modifier.padding(start = 56.dp))

                    ActionRow(
                        icon = Icons.Default.Share,
                        iconColor = Color(0xFF007AFF),
                        title = "Export Data to CSV",
                        subtitle = "Share spreadsheet with all records",
                        onClick = {
                            com.centwise.features.transactions.CsvExporter.shareExport(context)
                        },
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )

                    HorizontalDivider(color = textSecondary.copy(alpha = 0.12f), modifier = Modifier.padding(start = 56.dp))

                    ActionRow(
                        icon = Icons.Default.DeleteForever,
                        iconColor = Color.Red,
                        title = "Reset Database (Start Clean)",
                        subtitle = "Permanently wipe all transactions and start fresh",
                        onClick = { showResetDialog = true },
                        textPrimary = Color.Red,
                        textSecondary = textSecondary
                    )
                }
            }

            // Security notice
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = CentwiseColors.PrimaryEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "All transactions and accounts are stored 100% locally on your device in your private SQLite database. No financial data ever leaves your phone.",
                        style = CentwiseTypography.Caption,
                        color = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    modifier: Modifier = Modifier,
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = textSecondary.copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = count,
                    style = CentwiseTypography.Headline,
                    color = textPrimary
                )
                Text(
                    text = title,
                    style = CentwiseTypography.Caption,
                    color = textSecondary
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = CentwiseTypography.Body.copy(fontWeight = FontWeight.SemiBold),
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = CentwiseTypography.Caption,
                color = textSecondary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
