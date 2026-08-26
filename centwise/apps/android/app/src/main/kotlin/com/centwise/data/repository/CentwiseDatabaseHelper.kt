package com.centwise.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.centwise.data.models.AccountItem
import com.centwise.data.models.BudgetItem
import com.centwise.data.models.SubscriptionItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType

/**
 * Native Android SQLite database helper for Centwise (centwise.db).
 */
class CentwiseDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transactions (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                category TEXT NOT NULL,
                payment_method TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                note TEXT,
                raw_sms TEXT
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS accounts (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                balance REAL NOT NULL,
                provider_name TEXT NOT NULL,
                account_number TEXT NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS budgets (
                id TEXT PRIMARY KEY,
                category_name TEXT NOT NULL,
                allocated_amount REAL NOT NULL,
                spent_amount REAL NOT NULL,
                period TEXT NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subscriptions (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                amount REAL NOT NULL,
                billing_cycle TEXT NOT NULL,
                next_billing_date TEXT NOT NULL,
                icon TEXT NOT NULL
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS transactions")
        db.execSQL("DROP TABLE IF EXISTS accounts")
        db.execSQL("DROP TABLE IF EXISTS budgets")
        db.execSQL("DROP TABLE IF EXISTS subscriptions")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
        db.execSQL("PRAGMA cache_size = -500;") // 500 KB limit
        db.execSQL("PRAGMA temp_store = MEMORY;")
    }

    // Transactions
    fun insertTransaction(tx: TransactionItem) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("id", tx.id)
                put("title", tx.title)
                put("amount", tx.amount)
                put("type", tx.type.name)
                put("category", tx.category)
                put("payment_method", tx.paymentMethod)
                put("timestamp", tx.timestamp)
                put("note", tx.note)
                put("raw_sms", tx.rawSms)
            }
            db.insertWithOnConflict("transactions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun deleteTransaction(id: String) {
        writableDatabase.use { db ->
            db.delete("transactions", "id = ?", arrayOf(id))
        }
    }

    fun getAllTransactions(): List<TransactionItem> {
        val list = mutableListOf<TransactionItem>()
        readableDatabase.rawQuery(
            "SELECT id, title, amount, type, category, payment_method, timestamp, note, raw_sms FROM transactions ORDER BY timestamp DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val title = cursor.getString(1)
                val amount = cursor.getDouble(2)
                val typeStr = cursor.getString(3)
                val category = cursor.getString(4)
                val paymentMethod = cursor.getString(5)
                val timestamp = cursor.getLong(6)
                val note = if (cursor.isNull(7)) null else cursor.getString(7)
                val rawSms = if (cursor.isNull(8)) null else cursor.getString(8)
                val type = runCatching { TransactionType.valueOf(typeStr) }.getOrDefault(TransactionType.EXPENSE)

                list.add(
                    TransactionItem(
                        id = id,
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        paymentMethod = paymentMethod,
                        timestamp = timestamp,
                        note = note,
                        rawSms = rawSms
                    )
                )
            }
        }
        return list
    }

    // Accounts
    fun insertOrUpdateAccount(acc: AccountItem) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("type", acc.type)
                put("balance", acc.balance)
                put("provider_name", acc.providerName)
                put("account_number", acc.accountNumber)
            }
            db.insertWithOnConflict("accounts", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getAllAccounts(): List<AccountItem> {
        val list = mutableListOf<AccountItem>()
        readableDatabase.rawQuery("SELECT id, name, type, balance, provider_name, account_number FROM accounts", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    AccountItem(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        type = cursor.getString(2),
                        balance = cursor.getDouble(3),
                        providerName = cursor.getString(4),
                        accountNumber = cursor.getString(5)
                    )
                )
            }
        }
        return list
    }

    // Budgets
    fun insertOrUpdateBudget(budget: BudgetItem) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("id", budget.id)
                put("category_name", budget.categoryName)
                put("allocated_amount", budget.allocatedAmount)
                put("spent_amount", budget.spentAmount)
                put("period", budget.period)
            }
            db.insertWithOnConflict("budgets", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun deleteBudget(id: String) {
        writableDatabase.use { db ->
            db.delete("budgets", "id = ?", arrayOf(id))
        }
    }

    fun getAllBudgets(): List<BudgetItem> {
        val list = mutableListOf<BudgetItem>()
        readableDatabase.rawQuery("SELECT id, category_name, allocated_amount, spent_amount, period FROM budgets", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    BudgetItem(
                        id = cursor.getString(0),
                        categoryName = cursor.getString(1),
                        allocatedAmount = cursor.getDouble(2),
                        spentAmount = cursor.getDouble(3),
                        period = cursor.getString(4)
                    )
                )
            }
        }
        return list
    }

    // Subscriptions
    fun insertOrUpdateSubscription(sub: SubscriptionItem) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("id", sub.id)
                put("name", sub.name)
                put("amount", sub.amount)
                put("billing_cycle", sub.billingCycle)
                put("next_billing_date", sub.nextBillingDate)
                put("icon", sub.icon)
            }
            db.insertWithOnConflict("subscriptions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun deleteSubscription(id: String) {
        writableDatabase.use { db ->
            db.delete("subscriptions", "id = ?", arrayOf(id))
        }
    }

    fun getAllSubscriptions(): List<SubscriptionItem> {
        val list = mutableListOf<SubscriptionItem>()
        readableDatabase.rawQuery("SELECT id, name, amount, billing_cycle, next_billing_date, icon FROM subscriptions", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    SubscriptionItem(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        amount = cursor.getDouble(2),
                        billingCycle = cursor.getString(3),
                        nextBillingDate = cursor.getString(4),
                        icon = cursor.getString(5)
                    )
                )
            }
        }
        return list
    }

    fun clearAllTables() {
        writableDatabase.use { db ->
            db.delete("transactions", null, null)
            db.delete("accounts", null, null)
            db.delete("budgets", null, null)
            db.delete("subscriptions", null, null)
        }
    }

    companion object {
        private const val DATABASE_NAME = "centwise.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: CentwiseDatabaseHelper? = null

        fun getInstance(context: Context? = null): CentwiseDatabaseHelper? {
            if (INSTANCE == null && context != null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = CentwiseDatabaseHelper(context.applicationContext)
                    }
                }
            }
            return INSTANCE
        }

        fun init(context: Context) {
            getInstance(context)
        }
    }
}
