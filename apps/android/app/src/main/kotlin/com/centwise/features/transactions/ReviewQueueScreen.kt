package com.centwise.features.transactions

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import com.centwise.core.scanner.HistoricalSmsScanner
import com.centwise.data.models.ReviewQueueItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.data.repository.ReviewQueueRepository
import com.centwise.data.repository.TransactionRepository
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

    var isScanning by remember { mutableStateOf(false) }
    var scannedCount by remember { mutableIntStateOf(0) }
    var importedCount by remember { mutableIntStateOf(0) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    LaunchedEffect(Unit) {
        repository.refresh()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startManualScan(
                context = context,
                coroutineScope = coroutineScope,
                repository = repository,
                onScanningChange = { isScanning = it },
                onScannedChange = { scannedCount = it },
                onImportedChange = { importedCount = it }
            )
        } else {
            Toast.makeText(context, "SMS permission is required to scan inbox", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerScan() {
        if (isScanning) return
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS
                )
            )
        } else {
            startManualScan(
                context = context,
                coroutineScope = coroutineScope,
                repository = repository,
                onScanningChange = { isScanning = it },
                onScannedChange = { scannedCount = it },
                onImportedChange = { importedCount = it }
            )
        }
    }

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
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { coroutineScope.launch { CsvExporter.shareReviewQueueExport(context) } }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export Review Queue to CSV",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(
                onClick = { triggerScan() },
                enabled = !isScanning
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = accent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan SMS Inbox",
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Live scanning status banner
        if (isScanning) {
            Surface(
                color = accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accent
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Scanning SMS Inbox...",
                            style = CentwiseTypography.Headline.copy(fontSize = 14.sp),
                            color = textPrimary
                        )
                        Text(
                            text = if (scannedCount > 0) "$scannedCount messages checked ($importedCount imported)" else "Reading messages...",
                            style = CentwiseTypography.Caption,
                            color = textSecondary
                        )
                    }
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
                        color = accent.copy(alpha = 0.12f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                tint = accent,
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
                        onClick = { triggerScan() },
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            disabledContainerColor = accent.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (scannedCount > 0) "Scanning ($scannedCount)..." else "Scanning SMS...",
                                style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        } else {
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
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${items.size} messages awaiting review",
                            style = CentwiseTypography.Subheadline,
                            color = textSecondary
                        )
                        OutlinedButton(
                            onClick = { coroutineScope.launch { CsvExporter.shareReviewQueueExport(context) } },
                            shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export CSV",
                                style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                                color = accent
                            )
                        }
                    }
                }
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

private fun startManualScan(
    context: android.content.Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    repository: ReviewQueueRepository,
    onScanningChange: (Boolean) -> Unit,
    onScannedChange: (Int) -> Unit,
    onImportedChange: (Int) -> Unit
) {
    onScanningChange(true)
    onScannedChange(0)
    onImportedChange(0)

    coroutineScope.launch(Dispatchers.IO) {
        try {
            val result = HistoricalSmsScanner.scanInbox(
                context = context.applicationContext,
                forceFullScan = true,
                onProgress = { scanned, imported ->
                    onScannedChange(scanned)
                    onImportedChange(imported)
                }
            )
            withContext(Dispatchers.Main) {
                onScanningChange(false)
                repository.refresh()
                TransactionRepository.shared.loadFromRust()
                val message = if (result.transactionsImported > 0 || result.reviewQueued > 0) {
                    "Scan complete: ${result.transactionsImported} transactions imported, ${result.reviewQueued} queued for review (${result.totalScanned} messages checked)"
                } else {
                    "Scan complete: ${result.totalScanned} messages checked. All transactions up to date."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onScanningChange(false)
                Toast.makeText(context, "Scan error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
