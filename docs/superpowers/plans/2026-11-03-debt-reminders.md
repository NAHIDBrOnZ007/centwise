# Debt tracking implementation plan

1. Add a versioned SQLite migration for debts and their linked ledger entries. Keep regular reports separate. Verify atomic posting, linked SMS reuse, overpayment rejection, monthly totals, and database upgrade/restore.
2. Expose typed Rust operations for debt creation, repayments, due dates, detail and monthly totals. Regenerate Android bindings and use existing Settings navigation, modal buttons and account selectors.
3. Schedule offline Android reminders against the persisted due date. Recheck the debt before notifying. Verify Rust, Android build, and device behavior when an emulator is available.
