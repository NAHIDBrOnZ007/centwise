# 0002. Record loan cash flow in the Rust ledger

Android exposes Loans & Reminders under Settings. iOS will use the same
Rust API, validation, BDT amounts, date rules, and ledger semantics when its UI
and local notifications are connected.

Schema v10 adds a loan and links its principal and repayments to existing
transaction rows. A new movement writes the transaction, balance update, and
link in one SQLite write. Linking an existing transaction verifies its account,
direction, amount, and date and leaves its balance effect in place. A transaction
can belong to one loan only. Existing reported balances remain anchors for
historical transactions, consistent with the ordinary ledger.

An outstanding amount is derived from principal minus repayments. A fully
repaid loan closes automatically. Loan cash flow is grouped by the transaction's
Bangladesh calendar month. The existing transaction history and CSV still contain
ledger movements. Ordinary Rust dashboard, analytics and budget totals exclude
linked movements. CSV describes the ledger, while a SQLite backup preserves the
loan metadata and movement links as well.

A due date only schedules a local Android notification around 9 AM Dhaka time.
WorkManager may deliver it later under system battery restrictions. The worker
rechecks the stored due date and outstanding amount. The notification contains
a generic message and opens the loan after the existing app lock. Recording
settlement cancels pending work. Notification permission is optional.

The first release supports positive BDT movements, partial or full repayment,
and reminder changes. Users can link a recent matching transaction already in
the ledger. Later SMS imports are not automatically matched to a manual loan
entry. Linked financial fields are protected from edits and deletion in the
ordinary transaction editor.
