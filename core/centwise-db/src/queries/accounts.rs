use centwise_domain::{Account, AccountSummary};
use rusqlite::{params, OptionalExtension};

use crate::error::{DbError, DbResult};
use crate::queries::{collect, now_epoch_ms, Queries};

impl<'a> Queries<'a> {
    /// Inserts an account.
    pub fn insert_account(&self, account: &Account) -> DbResult<()> {
        self.connection.execute(
            "INSERT INTO accounts (id, name, provider, last_four, balance_minor, archived, created_at_epoch_ms)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
            params![
                account.id,
                account.name,
                account.provider,
                account.last_four,
                account.balance_minor,
                account.archived as i64,
                now_epoch_ms()
            ],
        )?;
        Ok(())
    }

    pub fn update_account(&self, account: &Account) -> DbResult<bool> {
        let changed = self.connection.execute(
            "UPDATE accounts SET name = ?1, provider = ?2, last_four = ?3,
                    balance_minor = ?4, archived = ?5
             WHERE id = ?6",
            params![
                account.name,
                account.provider,
                account.last_four,
                account.balance_minor,
                account.archived as i64,
                account.id
            ],
        )?;
        Ok(changed == 1)
    }

    pub fn delete_account(&self, id: &str) -> DbResult<bool> {
        let transaction_count: i64 = self.connection.query_row(
            "SELECT COUNT(*) FROM transactions WHERE account_id = ?1",
            params![id],
            |row| row.get(0),
        )?;
        if transaction_count > 0 {
            return Err(DbError::Invalid(
                "account is still used by transactions".into(),
            ));
        }
        Ok(self
            .connection
            .execute("DELETE FROM accounts WHERE id = ?1", params![id])?
            == 1)
    }

    /// Current balance of an account, or 0 when unknown.
    pub fn account_balance(&self, account_id: &str) -> DbResult<i64> {
        let balance = self
            .connection
            .query_row(
                "SELECT balance_minor FROM accounts WHERE id = ?1",
                params![account_id],
                |row| row.get::<_, i64>(0),
            )
            .optional()?;
        Ok(balance.unwrap_or(0))
    }

    /// All accounts, active first, ordered by name.
    pub fn list_accounts(&self) -> DbResult<Vec<AccountSummary>> {
        let mut statement = self.connection.prepare(
            "SELECT id, name, provider, last_four, balance_minor, archived
             FROM accounts
             ORDER BY archived ASC, name ASC",
        )?;

        let rows = statement.query_map([], |row| {
            Ok(AccountSummary {
                id: row.get(0)?,
                name: row.get(1)?,
                provider: row.get(2)?,
                last_four: row.get(3)?,
                balance_minor: row.get(4)?,
                archived: row.get::<_, i64>(5)? != 0,
            })
        })?;

        collect(rows)
    }

    /// Returns active accounts that can safely be associated with a parsed SMS.
    /// A caller must still require exactly one match before auto-importing.
    pub fn find_matching_accounts(
        &self,
        provider_id: &str,
        account_last4: Option<&str>,
    ) -> DbResult<Vec<AccountSummary>> {
        let mut statement = self.connection.prepare(
            "SELECT id, name, provider, last_four, balance_minor, archived
             FROM accounts
             WHERE archived = 0 AND provider = ?1
               AND (?2 IS NULL OR last_four = ?2)
             ORDER BY name ASC",
        )?;

        let rows = statement.query_map(params![provider_id, account_last4], |row| {
            Ok(AccountSummary {
                id: row.get(0)?,
                name: row.get(1)?,
                provider: row.get(2)?,
                last_four: row.get(3)?,
                balance_minor: row.get(4)?,
                archived: row.get::<_, i64>(5)? != 0,
            })
        })?;

        collect(rows)
    }
}
