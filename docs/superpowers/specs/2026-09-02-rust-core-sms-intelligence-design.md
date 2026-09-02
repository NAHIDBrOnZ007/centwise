# Rust Core SMS Intelligence Design

## Scope

Improve Centwise SMS categorization, correction learning, parsing, financial correctness, deduplication, and ingestion performance entirely inside the Rust workspace. Kotlin, Swift, generated UniFFI bindings, and exported UniFFI signatures remain unchanged.

## Behavioral contract

- A credit is categorized as `salary` only when the SMS contains a strong salary or payroll signal.
- Other recognized credits fall back to `income`; recognized expenses fall back to `other`.
- Refunds, cashback, interest or profit, dividends, fees, cash withdrawals, housing, and travel use dedicated system categories.
- Category resolution order is learned exact merchant mapping, persisted Smart Rule, strong transaction signal, built-in Bangladesh merchant rule, then the type-safe fallback.
- A category correction on an auto-tracked transaction with a stable parsed merchant or party creates or updates a future exact mapping without changing historical transactions.
- Messages with uncertain amount, direction, or account remain reviewable. Category uncertainty alone does not require review.

## Architecture

`centwise-parser` extracts normalized facts and transaction direction. `centwise-categorization` owns strong-signal and Bangladesh merchant classification. `centwise-db` owns system categories, exact merchant mappings, migrations, atomic balance changes, and persisted rules. `centwise-ffi` orchestrates these layers through the existing public API.

Exact merchant mappings use a dedicated SQLite table keyed by normalized merchant plus transaction type. Category provenance is stored internally on transactions as a string column; it is not added to exported FFI records in this phase.

## Financial correctness

Refunds increase account balance. A trusted reported balance is used only when ingesting a transaction that is at least as recent as the account's latest auto-tracked transaction with a reported balance; otherwise normal deltas apply. Transaction edits preserve stored SMS metadata when existing native callers omit it.

Transfers keep their current single-account zero-delta behavior in this phase because modeling two transfer legs would require exported/native contract changes. Detection and categorization can improve without changing that contract.

## Performance

Parser regexes are initialized once. Batch ingestion parses messages once, preloads rules and mappings once, and executes writes under one database transaction while retaining per-message result semantics. Review queue filtering and limits stay in SQL where possible.

## Testing

Every behavior change follows red-green TDD. Rust fixtures cover English and Bangla digits and terms, salary false positives, Income/Other fallbacks, refund balance direction, merchant learning, metadata preservation, deduplication, reported-balance ordering, and batch equivalence. Completion requires formatting, workspace tests, clippy with warnings denied, and a clean generated-binding diff.
