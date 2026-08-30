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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.repository.TransactionRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Idiomatic Jetpack Compose Data & Storage Screen matching iOS DataManagementScreen 1:1.
 * Features clean inset-grouped sections for Local Storage, Current Records, Data & Backup, and Destructive Reset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val repository = TransactionRepository.shared
    val coroutineScope = rememberCoroutineScope()

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
    val dividerColor = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)

    val dbFile = context.noBackupFilesDir.resolve("centwise.db")
    val dbSizeString = if (dbFile.exists()) {
        val bytes = dbFile.length()
        val kb = bytes / 1024.0
        if (kb < 1024) String.format("%.1f KB", kb) else String.format("%.2f MB", kb / 1024.0)
    } else "Clean DB"

    if (showLoadDemoDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDemoDialog = false },
            title = { Text("Load Demo Sample Data?", style = CentwiseTypography.Headline, color = textPrimary) },
            text = {
                Text(
                    "This will populate your database with realistic sample transactions, accounts, budgets, and subscriptions for previewing Centwise.",
                    style = CentwiseTypography.Body,
                    color = textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLoadDemoDialog = false
                        val summary = CentwiseRustBackend.loadDemoData()
                        if (summary != null) {
                            repository.clearLegacyStorage()
                            repository.loadFromRust()
                            Toast.makeText(context, "Sample data loaded (${summary.transactions} transactions)", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not load demo data", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Load Demo Data", color = accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoadDemoDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = cardBg
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Wipe Database & Reset?", style = CentwiseTypography.Headline, color = textPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete all transactions, budgets, and subscriptions? This action cannot be undone.",
                    style = CentwiseTypography.Body,
                    color = textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        repository.resetToEmptyDatabase()
                        Toast.makeText(context, "Database wiped. Starting completely clean.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Wipe Everything", color = CentwiseColors.ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = cardBg
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarBackButton(onBackClick = onBackClick, isDark = isDark)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Data & Storage",
                style = CentwiseTypography.Headline,
                color = textPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Section 1: Local Storage
            item {
                Column {
                    Text(
                        text = "Local Storage",
                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp),
                        color = textSecondary,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                    ) {
                        // Database Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Database",
                                style = CentwiseTypography.Body,
                                color = textPrimary
                            )
                            Text(
                                text = dbSizeString,
                                style = CentwiseTypography.Body.copy(fontWeight = FontWeight.Medium),
                                color = textSecondary
                            )
                        }

                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 16.dp))

                        // Encrypted notice row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Encrypted on this device",
                                style = CentwiseTypography.Body,
                                color = textSecondary
                            )
                        }
                    }
                }
            }

            // Section 2: Current Records
            item {
                Column {
                    Text(
                        text = "Current Records",
                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp),
                        color = textSecondary,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                    ) {
                        RecordRow(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            iconColor = accent,
                            title = "Transactions",
                            count = transactions.size,
                            showDivider = true,
                            dividerColor = dividerColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        RecordRow(
                            icon = Icons.Default.AccountBalance,
                            iconColor = accent,
                            title = "Accounts",
                            count = accounts.size,
                            showDivider = true,
                            dividerColor = dividerColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        RecordRow(
                            icon = Icons.Default.PieChart,
                            iconColor = accent,
                            title = "Budgets",
                            count = budgets.size,
                            showDivider = true,
                            dividerColor = dividerColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                        RecordRow(
                            icon = Icons.Default.Autorenew,
                            iconColor = accent,
                            title = "Subscriptions",
                            count = subscriptions.size,
                            showDivider = false,
                            dividerColor = dividerColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }

            // Section 3: Data & Backup
            item {
                Column {
                    Text(
                        text = "Data & Backup",
                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp),
                        color = textSecondary,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                    ) {
                        // Scan SMS Inbox (Manual SMS Trigger)
                        var isScanning by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosBounceClick {
                                    if (isScanning) return@iosBounceClick
                                    val hasReadSms = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.READ_SMS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                    if (!hasReadSms) {
                                        val activity = context as? androidx.fragment.app.FragmentActivity
                                        activity?.let {
                                            androidx.core.app.ActivityCompat.requestPermissions(
                                                it,
                                                arrayOf(android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS),
                                                1001
                                            )
                                        }
                                    } else {
                                        isScanning = true
                                        Toast.makeText(context, "Scanning SMS inbox for bank & MFS transactions...", Toast.LENGTH_SHORT).show()
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val result = com.centwise.core.scanner.HistoricalSmsScanner.scanInbox(context.applicationContext)
                                            withContext(Dispatchers.Main) {
                                                isScanning = false
                                                Toast.makeText(
                                                    context,
                                                    "Scan complete: Found ${result.transactionsImported} transactions (${result.totalScanned} scanned)",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isScanning) "Scanning SMS Inbox..." else "Scan SMS Inbox",
                                    style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                                    color = textPrimary
                                )
                                Text(
                                    text = "Auto-detect bank & MFS transactions from your messages",
                                    style = CentwiseTypography.Caption,
                                    color = textSecondary
                                )
                            }
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = accent
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFFC7C7CC),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 52.dp))

                        // Load Demo Sample Data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosBounceClick { showLoadDemoDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Load Demo Sample Data",
                                style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                                color = textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFFC7C7CC),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 52.dp))

                        // Export Data to CSV
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosBounceClick {
                                    com.centwise.features.transactions.CsvExporter.shareExport(context)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Export Data to CSV",
                                style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                                color = textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFFC7C7CC),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Demo data adds sample transactions, accounts, budgets, and subscriptions. Export creates a local CSV file.",
                        style = CentwiseTypography.Caption,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }

            // Section 4: Destructive Action (Reset Database)
            item {
                Column {
                    Surface(
                        shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge),
                        color = cardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .iosBounceClick { showResetDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Reset Database",
                                style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                                color = CentwiseColors.ExpenseRed,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Reset permanently deletes all transactions, budgets, and subscriptions from this device.",
                        style = CentwiseTypography.Caption,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    count: Int,
    showDivider: Boolean,
    dividerColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = CentwiseTypography.Body,
                    color = textPrimary
                )
            }

            Text(
                text = "$count",
                style = CentwiseTypography.Body.copy(fontWeight = FontWeight.Medium),
                color = textSecondary
            )
        }

        if (showDivider) {
            HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 52.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DataManagementScreenPreview() {
    DataManagementScreen()
}
