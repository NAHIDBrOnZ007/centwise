package com.centwise.features.loans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.centwise.R
import com.centwise.core.design.components.ButtonVariant
import com.centwise.core.design.components.CentwiseButton
import com.centwise.core.design.components.ModalSheetTopBar
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.uniffi.AccountRecord
import com.centwise.core.uniffi.LoanPaymentInput
import com.centwise.core.uniffi.LoanRecord
import com.centwise.core.uniffi.TransactionRecord
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoanPaymentSheet(
    repo: LoanRepository,
    loan: LoanRecord?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSaved: (LoanRecord) -> Unit
) {
    val scope = rememberCoroutineScope()
    val operationId = rememberSaveable { UUID.randomUUID().toString() }
    var person by rememberSaveable { mutableStateOf("") }
    var lent by rememberSaveable { mutableStateOf(loan?.lent ?: true) }
    var note by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var due by rememberSaveable { mutableStateOf<Long?>(null) }
    var accountId by rememberSaveable { mutableStateOf(loan?.accountId) }
    var link by rememberSaveable { mutableStateOf(false) }
    var linkedId by rememberSaveable { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val accounts by produceState<List<AccountRecord>>(emptyList(), repo) {
        try {
            value = repo.accounts().filterNot { it.archived }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            error = true
        }
    }
    val incoming = if (loan == null) !lent else lent
    val candidates by produceState<List<TransactionRecord>?>(null, incoming, link) {
        value = null
        if (link) {
            try {
                value = repo.candidates(incoming)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                error = true
            }
        }
    }
    val linked = candidates?.firstOrNull { it.id == linkedId }
    val account = accounts.firstOrNull { it.id == accountId }
    val canSave = !busy && (loan != null || person.isNotBlank()) &&
        (if (link) linked != null else account != null && amount.isNotBlank())
    val title = if (loan == null) R.string.loan_add else if (loan.lent) R.string.loan_record_received else R.string.loan_record_paid
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, sheetState = sheetState,
        containerColor = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModalSheetTopBar(
                title = stringResource(title),
                onCancel = { if (!busy) onDismiss() },
                onSave = {
                    if (canSave) scope.launch {
                        busy = true
                        error = false
                        val payment = LoanPaymentInput(
                            id = operationId,
                            accountId = linked?.accountId ?: accountId.orEmpty(),
                            amount = linked?.let { BigDecimal.valueOf(it.amountMinor, 2).toPlainString() } ?: amount,
                            occurredAtEpochMs = linked?.occurredAtEpochMs ?: date,
                            existingTransactionId = linked?.id
                        )
                        try {
                            val saved = if (loan == null) repo.create(operationId, person, lent, due, note, payment)
                                else repo.repay(loan.id, payment)
                            onSaved(saved)
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            error = true
                        } finally {
                            busy = false
                        }
                    }
                },
                saveEnabled = canSave,
                cancelLabel = stringResource(R.string.loan_cancel),
                saveLabel = stringResource(R.string.loan_save),
                accent = AccentOptions.byName(AppearancePrefs.accentName).color,
                textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary,
                textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary,
                isDark = isDark
            )
            if (loan == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(lent, { lent = true; linkedId = null }, { Text(stringResource(R.string.loan_lent)) }, enabled = !busy)
                    FilterChip(!lent, { lent = false; linkedId = null }, { Text(stringResource(R.string.loan_borrowed)) }, enabled = !busy)
                }
                OutlinedTextField(person, { person = it }, Modifier.fillMaxWidth(), singleLine = true,
                    enabled = !busy, label = { Text(stringResource(R.string.loan_person)) })
            }
            CentwiseButton(stringResource(if (link) R.string.loan_new_payment else R.string.loan_link),
                { link = !link; linkedId = null }, variant = ButtonVariant.SECONDARY,
                isFullWidth = true, enabled = !busy, isDark = isDark)
            if (link) {
                val options = candidates
                if (options == null) CircularProgressIndicator()
                else if (options.isEmpty()) Text(stringResource(R.string.loan_no_candidates))
                else LoanDropdown(stringResource(R.string.loan_link), options, linked, !busy,
                    label = { "${it.title} · ${loanAmount(it.amountMinor)} · ${loanDate(it.occurredAtEpochMs)}" },
                    onSelect = { linkedId = it.id })
            } else {
                OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), singleLine = true,
                    enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(stringResource(R.string.loan_amount)) })
                if (loan != null) {
                    TextButton(onClick = { amount = BigDecimal.valueOf(loan.remainingMinor, 2).toPlainString() }, enabled = !busy) {
                        Text(stringResource(R.string.loan_full_amount))
                    }
                }
                LoanDropdown(stringResource(R.string.loan_account), accounts, account, !busy,
                    label = { it.name }, onSelect = { accountId = it.id })
                CentwiseButton(stringResource(R.string.loan_summary_line,
                    stringResource(R.string.loan_date), loanDate(date)), { picker = "payment" },
                    variant = ButtonVariant.SECONDARY, isFullWidth = true, enabled = !busy, isDark = isDark)
            }
            if (loan == null) {
                CentwiseButton(due?.let { stringResource(R.string.loan_summary_line,
                    stringResource(R.string.loan_due_date), loanDate(it)) } ?: stringResource(R.string.loan_due_date),
                    { picker = "due" }, variant = ButtonVariant.SECONDARY, isFullWidth = true, enabled = !busy, isDark = isDark)
                if (due != null) TextButton({ due = null }, enabled = !busy) {
                    Text(stringResource(R.string.loan_remove_reminder))
                }
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), enabled = !busy,
                    label = { Text(stringResource(R.string.loan_note)) })
            }
            if (error) Text(stringResource(R.string.loan_save_failed), color = MaterialTheme.colorScheme.error)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
    picker?.let { field ->
        LoanDateDialog(if (field == "due") due else date, reminder = field == "due", onDismiss = { picker = null }) {
            if (field == "due") due = it else date = it
            picker = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> LoanDropdown(
    title: String, options: List<T>, selected: T?, enabled: Boolean, label: (T) -> String, onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(selected?.let(label).orEmpty(), {}, Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth(),
            readOnly = true, enabled = enabled, label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
        ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(label(option)) }, onClick = {
                    onSelect(option)
                    expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanDateDialog(initial: Long?, reminder: Boolean, onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    val localDate = Instant.ofEpochMilli(initial ?: System.currentTimeMillis()).atZone(loanZone).toLocalDate()
    val state = rememberDatePickerState(initialSelectedDateMillis = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(onClick = {
            state.selectedDateMillis?.let { picked ->
                val selectedDate = Instant.ofEpochMilli(picked).atZone(ZoneOffset.UTC).toLocalDate()
                val time = if (reminder) LocalTime.of(9, 0) else LocalTime.MIDNIGHT
                onSelect(selectedDate.atTime(time).atZone(loanZone).toInstant().toEpochMilli())
            }
        }, enabled = state.selectedDateMillis != null) { Text(stringResource(R.string.loan_select)) }
    }, dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.loan_cancel)) } }) {
        DatePicker(state)
    }
}

@Composable
internal fun LoanReminderDialog(repo: LoanRepository, loan: LoanRecord, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var choosing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val save: (Long?) -> Unit = { due ->
        scope.launch {
            busy = true
            try {
                repo.changeDueDate(loan.id, due)
                onSaved()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
            } finally {
                busy = false
            }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.loan_update_due)) },
        text = {
            Text(if (failed) stringResource(R.string.loan_save_failed) else
                loan.dueAtEpochMs?.let(::loanDate) ?: stringResource(R.string.loan_no_due))
        },
        confirmButton = {
            TextButton({ choosing = true }, enabled = !busy) { Text(stringResource(R.string.loan_select)) }
        },
        dismissButton = {
            TextButton({ save(null) }, enabled = !busy) { Text(stringResource(R.string.loan_remove_reminder)) }
        }
    )
    if (choosing) LoanDateDialog(loan.dueAtEpochMs, true, { choosing = false }) {
        choosing = false
        save(it)
    }
}
