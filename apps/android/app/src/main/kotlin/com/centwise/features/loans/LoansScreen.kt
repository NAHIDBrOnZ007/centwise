package com.centwise.features.loans

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.centwise.R
import com.centwise.core.design.components.ButtonVariant
import com.centwise.core.design.components.CentwiseButton
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.core.uniffi.LoanRecord
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val loanZone: ZoneId = ZoneId.of("Asia/Dhaka")
internal fun loanDate(epoch: Long): String = DateTimeFormatter.ofPattern("d MMM yyyy")
    .format(Instant.ofEpochMilli(epoch).atZone(loanZone))
internal fun loanAmount(minor: Long): String = "৳" + BigDecimal.valueOf(minor, 2).toPlainString()

@Composable
fun LoansScreen(initialLoanId: String?, onBackClick: () -> Unit, isDark: Boolean) {
    val context = LocalContext.current
    val repo = remember(context) { LoanRepository(context) }
    val scope = rememberCoroutineScope()
    var selectedId by rememberSaveable(initialLoanId) { mutableStateOf(initialLoanId) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var changingReminder by rememberSaveable { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    val data by produceState<Result<LoanScreenData>?>(null, revision, selectedId) {
        value = try {
            Result.success(LoanScreenData(repo.loans(), repo.months(), selectedId, selectedId?.let { repo.movements(it) }.orEmpty()))
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
    val selected = data?.getOrNull()?.loans?.firstOrNull { it.id == selectedId }
    val back = { if (selectedId == null) onBackClick() else selectedId = null }
    BackHandler { back() }

    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = granted
        if (granted) scope.launch {
            data?.getOrNull()?.loans.orEmpty().forEach { LoanReminderWorker.schedule(context, it) }
        }
    }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (notificationsEnabled) {
            data?.getOrNull()?.loans.orEmpty().forEach { LoanReminderWorker.schedule(context, it) }
        }
    }

    Surface(color = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TopBarBackButton(onBackClick = back, isDark = isDark)
                    Text(selected?.person ?: stringResource(R.string.loans_title), style = CentwiseTypography.Headline)
                }
            }
            if (!notificationsEnabled) item {
                CentwiseButton(stringResource(R.string.loan_notifications_disabled), onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        settings.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }
                }, variant = ButtonVariant.SECONDARY, isFullWidth = true, isDark = isDark)
            }
            val loaded = data?.getOrNull()
            when {
                data == null -> item { CircularProgressIndicator() }
                loaded == null -> item {
                    Text(stringResource(R.string.loan_load_failed))
                    CentwiseButton(stringResource(R.string.loan_retry), { revision++ }, isDark = isDark)
                }
                selected != null -> {
                    item {
                        LoanSummary(selected, isDark)
                        if (selected.note.isNotBlank()) Text(selected.note, modifier = Modifier.padding(top = 12.dp))
                    }
                    if (selected.remainingMinor > 0) item {
                        CentwiseButton(
                            stringResource(if (selected.lent) R.string.loan_record_received else R.string.loan_record_paid),
                            { editing = true }, isFullWidth = true, isDark = isDark
                        )
                        Spacer(Modifier.height(8.dp))
                        CentwiseButton(stringResource(R.string.loan_update_due), { changingReminder = true },
                            variant = ButtonVariant.SECONDARY, isFullWidth = true, isDark = isDark)
                    }
                    item { Text(stringResource(R.string.loan_history), style = CentwiseTypography.Headline) }
                    items(loaded.movements.takeIf { loaded.selectedId == selected.id }.orEmpty(), key = { it.transactionId }) { movement ->
                        ListItem(
                            headlineContent = {
                                Text(stringResource(
                                    if (movement.repayment) {
                                        if (selected.lent) R.string.loan_received else R.string.loan_paid
                                    } else if (selected.lent) R.string.loan_lent else R.string.loan_borrowed
                                ))
                            },
                            supportingContent = { Text(stringResource(R.string.loan_summary_line,
                                movement.accountName, loanDate(movement.occurredAtEpochMs))) },
                            trailingContent = { Text(loanAmount(movement.amountMinor)) }
                        )
                    }
                }
                else -> {
                    item {
                        CentwiseButton(stringResource(R.string.loan_add), { editing = true }, isFullWidth = true, isDark = isDark)
                        if (loaded.loans.isEmpty()) Text(stringResource(R.string.loan_empty), modifier = Modifier.padding(top = 16.dp))
                    }
                    items(loaded.loans, key = { it.id }) { loan ->
                        Surface(onClick = { selectedId = loan.id }, shape = RoundedCornerShape(16.dp),
                            color = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(loan.person, style = CentwiseTypography.Headline)
                                LoanAmounts(loan)
                            }
                        }
                    }
                    if (loaded.months.isNotEmpty()) item {
                        Text(stringResource(R.string.loan_months), style = CentwiseTypography.Headline)
                    }
                    items(loaded.months, key = { it.month }) { month ->
                        ListItem(headlineContent = { Text(month.month) }, supportingContent = {
                            Text(stringResource(R.string.loan_month_amounts,
                                loanAmount(month.incomingMinor), loanAmount(month.outgoingMinor)))
                        })
                    }
                }
            }
        }
    }

    if (editing) {
        LoanPaymentSheet(repo, selected, isDark, onDismiss = { editing = false }, onSaved = {
            editing = false
            selectedId = it.id
            revision++
        })
    }
    if (changingReminder && selected != null) {
        LoanReminderDialog(repo, selected, onDismiss = { changingReminder = false }, onSaved = {
            changingReminder = false
            revision++
        })
    }
}

private data class LoanScreenData(
    val loans: List<LoanRecord>,
    val months: List<com.centwise.core.uniffi.LoanMonthRecord>,
    val selectedId: String?,
    val movements: List<com.centwise.core.uniffi.LoanMovementRecord>
)

@Composable
private fun LoanSummary(loan: LoanRecord, isDark: Boolean) {
    Surface(shape = RoundedCornerShape(16.dp), color = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LoanAmounts(loan)
            loan.dueAtEpochMs?.takeIf { loan.remainingMinor > 0 }?.let {
                Text(stringResource(R.string.loan_summary_line, stringResource(R.string.loan_due_date), loanDate(it)))
            }
        }
    }
}

@Composable
private fun LoanAmounts(loan: LoanRecord) {
    Text(stringResource(R.string.loan_summary_line,
        stringResource(if (loan.lent) R.string.loan_lent else R.string.loan_borrowed),
        loanAmount(loan.principalMinor)))
    Text(stringResource(R.string.loan_summary_line,
        stringResource(if (loan.lent) R.string.loan_received else R.string.loan_paid),
        loanAmount(loan.repaidMinor)))
    Text(if (loan.remainingMinor == 0L) stringResource(R.string.loan_complete) else
        stringResource(R.string.loan_summary_line,
            stringResource(if (loan.lent) R.string.loan_remaining_receivable else R.string.loan_remaining_payable),
            loanAmount(loan.remainingMinor)), style = CentwiseTypography.Headline)
}
