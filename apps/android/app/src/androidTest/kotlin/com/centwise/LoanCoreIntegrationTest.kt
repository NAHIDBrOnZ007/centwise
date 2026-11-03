package com.centwise

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.centwise.core.uniffi.AccountInput
import com.centwise.core.uniffi.CentwiseCore
import com.centwise.core.uniffi.LoanPaymentInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoanCoreIntegrationTest {
    @Test
    fun loanAndPartialRepaymentUpdateBalancesThroughRust() {
        CentwiseCore.open(":memory:").use { core ->
            core.insertAccount(AccountInput("wallet", "Wallet", "cash", null, 600_000L, false))
            core.insertAccount(AccountInput("bank", "Bank", "city-bank", null, 0L, false))
            val loan = core.createLoan(
                "loan", "Test person", true, null, "Example",
                LoanPaymentInput("principal", "wallet", "5000", 1_700_000_000_000L, null)
            )
            assertEquals(500_000L, loan.remainingMinor)
            val updated = core.repayLoan("loan", LoanPaymentInput("repayment", "bank", "1000", 1_703_000_000_000L, null))
            assertEquals(400_000L, updated.remainingMinor)
            assertTrue(core.listAccounts().all { it.balanceMinor == 100_000L })
            assertEquals(2, core.loanMovements("loan").size)
            assertEquals(2, core.loanMonths().size)
            assertEquals(setOf("principal", "repayment"), core.loanTransactionIds().toSet())
        }
    }
}
