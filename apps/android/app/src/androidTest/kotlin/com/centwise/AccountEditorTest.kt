package com.centwise

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.centwise.data.models.AccountItem
import com.centwise.features.accounts.AddEditAccountSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountEditorTest {
    @get:Rule val compose = createComposeRule()

    private val account = AccountItem(
        id = "synthetic-account", name = "Demo", type = "Bank Account",
        balance = -125.5, providerName = "City Bank", accountNumber = "1234",
        archived = true
    )

    @Test
    fun savingAnOverdrawnAccountPreservesBalanceAndArchivedStatus() {
        var saved: AccountItem? = null
        compose.setContent {
            MaterialTheme {
                AddEditAccountSheet(account, onDismiss = {}, onSave = { saved = it })
            }
        }
        compose.onNodeWithText("-125.5").assertExists()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { saved != null }
        assertEquals(-125.5, saved!!.balance, 0.0)
        assertTrue(saved!!.archived)
    }

    @Test
    fun invalidBalanceCannotBeSavedAsZeroOrNonFiniteMoney() {
        compose.setContent {
            MaterialTheme {
                AddEditAccountSheet(account.copy(balance = 125.5), onDismiss = {}, onSave = {})
            }
        }
        compose.onNodeWithText("125.5").performTextReplacement("NaN")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("NaN").performTextReplacement("invalid")
        compose.onNodeWithText("Save").assertIsNotEnabled()
    }
}
