package com.centwise.features.loans

import android.content.Context
import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.core.uniffi.LoanPaymentInput
import com.centwise.core.uniffi.LoanRecord
import com.centwise.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class LoanRepository(context: Context) {
    private val context = context.applicationContext

    private fun core() = CentwiseRustBackend.requireCore()

    suspend fun loans() = withContext(Dispatchers.IO) { core().listLoans() }
    suspend fun accounts() = withContext(Dispatchers.IO) { core().listAccounts() }
    suspend fun movements(id: String) = withContext(Dispatchers.IO) { core().loanMovements(id) }
    suspend fun months() = withContext(Dispatchers.IO) { core().loanMonths() }
    suspend fun candidates(incoming: Boolean) = withContext(Dispatchers.IO) {
        core().loanTransactionCandidates(incoming)
    }

    suspend fun create(
        id: String, person: String, lent: Boolean, due: Long?, note: String, payment: LoanPaymentInput
    ) = save { core().createLoan(id, person, lent, due, note, payment) }.also { LoanReminderWorker.schedule(context, it) }

    suspend fun repay(id: String, payment: LoanPaymentInput) = save { core().repayLoan(id, payment) }.also {
        if (it.remainingMinor == 0L) LoanReminderWorker.schedule(context, it)
    }

    suspend fun changeDueDate(id: String, due: Long?) = save { core().setLoanDueDate(id, due) }.also {
        LoanReminderWorker.schedule(context, it)
    }

    private suspend fun save(write: () -> LoanRecord): LoanRecord {
        val loan = withContext(Dispatchers.IO) { write() }
        TransactionRepository.shared.loadFromRust()
        return loan
    }
}
