package com.centwise.features.settings

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.core.scanner.HistoricalSmsScanner
import com.centwise.data.repository.ReviewQueueRepository
import com.centwise.data.repository.TransactionRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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
    var refreshCounter by remember { mutableIntStateOf(0) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val dividerColor = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)

    var dbSizeString by remember { mutableStateOf("Loading…") }
    LaunchedEffect(refreshCounter, transactions.size, accounts.size, budgets.size, subscriptions.size) {
        dbSizeString = withContext(Dispatchers.IO) {
            val dir = context.noBackupFilesDir
            val totalBytes = listOf("centwise.db", "centwise.db-wal", "centwise.db-shm")
                .sumOf { name -> dir.resolve(name).takeIf { it.exists() }?.length() ?: 0L }

            if (totalBytes == 0L) {
                "0 KB"
            } else {
                val kb = totalBytes / 1024.0
                if (kb < 1024) {
                    String.format(Locale.US, "%.1f KB", kb)
                } else {
                    String.format(Locale.US, "%.2f MB", kb / 1024.0)
                }
            }
        }
    }

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
                        coroutineScope.launch(Dispatchers.IO) {
                            val summary = CentwiseRustBackend.loadDemoData()
                            if (summary != null) {
                                repository.refreshNow()
                            }
                            withContext(Dispatchers.Main) {
                                refreshCounter++
                                Toast.makeText(
                                    context,
                                    if (summary != null) "Sample data loaded (${summary.transactions} transactions)"
                                    else "Could not load demo data",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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
                        coroutineScope.launch(Dispatchers.IO) {
                            val succeeded = CentwiseRustBackend.resetToEmptyDatabase()
                            if (succeeded) {
                                repository.refreshNow()
                            }
                            withContext(Dispatchers.Main) {
                                refreshCounter++
                                Toast.makeText(
                                    context,
                                    if (succeeded) "Database wiped. Starting completely clean."
                                    else "Could not reset database",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
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
                        text = if (LanguagePrefs.isBengali) "লোকাল স্টোরেজ" else "Local Storage",
                        style = CentwiseTypography.Headline.copy(fontSize = 14.sp),
                        color = textSecondary,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                            .padding(16.dp)
                    ) {
                        // Header: Icon + Title + Private Subtitle on left; Size + Space Used on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = if (LanguagePrefs.isBengali) "ডিভাইস স্টোরেজ" else "Device Storage",
                                        style = CentwiseTypography.Headline.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                        color = textPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (LanguagePrefs.isBengali) "লোকাল ও এনক্রিপ্ট করা" else "Local & Encrypted",
                                        style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                                        color = textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = dbSizeString,
                                    style = CentwiseTypography.Headline.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                    color = textPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (LanguagePrefs.isBengali) "ব্যবহৃত স্থান" else "Space Used",
                                    style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                                    color = textSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Segmented Progress Bar (Mobile Settings style)
                        val txCount = transactions.size
                        val acctBudgetCount = accounts.size + budgets.size + subscriptions.size
                        val hasUserData = txCount > 0 || acctBudgetCount > 0

                        val txColor = Color(0xFF3B82F6) // Royal Blue
                        val acctColor = Color(0xFF10B981) // Emerald Green
                        val rulesColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1) // Slate Gray

                        val activeSegments = remember(txCount, acctBudgetCount, hasUserData, isDark) {
                            if (!hasUserData) {
                                listOf(
                                    25f to acctColor,
                                    75f to rulesColor
                                )
                            } else {
                                buildList {
                                    if (txCount > 0) add(maxOf(txCount * 1.5f, 12f) to txColor)
                                    if (acctBudgetCount > 0) add(maxOf(acctBudgetCount * 1.5f, 10f) to acctColor)
                                    add(25f to rulesColor)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                        ) {
                            activeSegments.forEachIndexed { index, (weight, color) ->
                                Box(
                                    modifier = Modifier
                                        .weight(weight)
                                        .fillMaxHeight()
                                        .background(color)
                                )
                                if (index < activeSegments.lastIndex) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Breakdown Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StorageLegendItem(
                                color = txColor,
                                label = if (LanguagePrefs.isBengali) "লেনদেন" else "Transactions",
                                value = if (txCount == 0) (if (LanguagePrefs.isBengali) "০" else "0") else "$txCount",
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            StorageLegendItem(
                                color = acctColor,
                                label = if (LanguagePrefs.isBengali) "অ্যাকাউন্ট" else "Accounts",
                                value = if (acctBudgetCount == 0) (if (LanguagePrefs.isBengali) "০" else "0") else "$acctBudgetCount",
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            StorageLegendItem(
                                color = rulesColor,
                                label = if (LanguagePrefs.isBengali) "ক্যাটাগরি" else "Categories",
                                value = if (LanguagePrefs.isBengali) "বিল্ট-ইন" else "Built-in",
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = dividerColor)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Private & Offline notice row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (LanguagePrefs.isBengali) "ডিভাইসে সুরক্ষিত" else "On-device encrypted",
                                    style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                                    color = textSecondary,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (LanguagePrefs.isBengali) "১০০% অফলাইন" else "100% Offline",
                                    style = CentwiseTypography.Caption.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFF10B981)
                                )
                            }
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
                        var scannedCount by remember { mutableIntStateOf(0) }
                        var importedCount by remember { mutableIntStateOf(0) }

                        val smsPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestMultiplePermissions()
                        ) { permissions ->
                            val granted = permissions[Manifest.permission.READ_SMS] == true ||
                                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                isScanning = true
                                scannedCount = 0
                                importedCount = 0
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val result = HistoricalSmsScanner.scanInbox(
                                            context = context.applicationContext,
                                            forceFullScan = true,
                                            onProgress = { s, i ->
                                                scannedCount = s
                                                importedCount = i
                                            }
                                        )
                                        withContext(Dispatchers.Main) {
                                            isScanning = false
                                            repository.loadFromRust()
                                            ReviewQueueRepository.shared.refresh()
                                            refreshCounter++
                                            val msg = if (result.transactionsImported > 0 || result.reviewQueued > 0) {
                                                "Scan complete: ${result.transactionsImported} transactions imported, ${result.reviewQueued} queued for review (${result.totalScanned} messages checked)"
                                            } else {
                                                "Scan complete: ${result.totalScanned} messages checked. All transactions up to date."
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isScanning = false
                                            Toast.makeText(context, "Scan error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "SMS permission is required to scan inbox", Toast.LENGTH_SHORT).show()
                            }
                        }

                        fun triggerSmsScan() {
                            if (isScanning) return
                            val hasReadSms = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_SMS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasReadSms) {
                                smsPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_SMS,
                                        Manifest.permission.RECEIVE_SMS
                                    )
                                )
                            } else {
                                isScanning = true
                                scannedCount = 0
                                importedCount = 0
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val result = HistoricalSmsScanner.scanInbox(
                                            context = context.applicationContext,
                                            forceFullScan = true,
                                            onProgress = { s, i ->
                                                scannedCount = s
                                                importedCount = i
                                            }
                                        )
                                        withContext(Dispatchers.Main) {
                                            isScanning = false
                                            repository.loadFromRust()
                                            ReviewQueueRepository.shared.refresh()
                                            refreshCounter++
                                            val msg = if (result.transactionsImported > 0 || result.reviewQueued > 0) {
                                                "Scan complete: ${result.transactionsImported} transactions imported, ${result.reviewQueued} queued for review (${result.totalScanned} messages checked)"
                                            } else {
                                                "Scan complete: ${result.totalScanned} messages checked. All transactions up to date."
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isScanning = false
                                            Toast.makeText(context, "Scan error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosBounceClick { triggerSmsScan() }
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
                                    text = if (isScanning) {
                                        if (scannedCount > 0) "Scanning SMS... ($scannedCount checked)" else "Scanning SMS Inbox..."
                                    } else "Scan SMS Inbox",
                                    style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                                    color = textPrimary
                                )
                                Text(
                                    text = if (isScanning) {
                                        if (importedCount > 0) "$importedCount transactions imported so far" else "Checking messages for transactions..."
                                    } else {
                                        "Auto-detect bank & MFS transactions from your messages"
                                    },
                                    style = CentwiseTypography.Caption,
                                    color = if (isScanning && importedCount > 0) accent else textSecondary
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
                                    coroutineScope.launch {
                                        com.centwise.features.transactions.CsvExporter.shareExport(context)
                                    }
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

                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(start = 52.dp))

                        // Export Review Queue to CSV
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosBounceClick {
                                    coroutineScope.launch {
                                        com.centwise.features.transactions.CsvExporter.shareReviewQueueExport(context)
                                    }
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
                                text = "Export Review Queue to CSV",
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

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
            color = textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = CentwiseTypography.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            color = textPrimary,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DataManagementScreenPreview() {
    DataManagementScreen()
}
