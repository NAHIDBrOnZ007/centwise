CREATE TABLE loans (
    id TEXT PRIMARY KEY NOT NULL,
    person TEXT NOT NULL,
    lent INTEGER NOT NULL CHECK (lent IN (0, 1)),
    due_at_epoch_ms INTEGER,
    note TEXT NOT NULL
);

CREATE TABLE loan_movements (
    loan_id TEXT NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    transaction_id TEXT PRIMARY KEY NOT NULL REFERENCES transactions(id),
    repayment INTEGER NOT NULL CHECK (repayment IN (0, 1))
);

CREATE INDEX idx_loan_movements_loan ON loan_movements(loan_id);
CREATE UNIQUE INDEX idx_loan_principal ON loan_movements(loan_id) WHERE repayment = 0;

CREATE VIEW ordinary_transactions AS
SELECT t.* FROM transactions t
WHERE NOT EXISTS (SELECT 1 FROM loan_movements m WHERE m.transaction_id = t.id);

CREATE TRIGGER protect_loan_movement BEFORE UPDATE ON transactions
WHEN EXISTS (SELECT 1 FROM loan_movements WHERE transaction_id = OLD.id)
AND (NEW.amount_minor IS NOT OLD.amount_minor
    OR NEW.transaction_type IS NOT OLD.transaction_type
    OR NEW.occurred_at_epoch_ms IS NOT OLD.occurred_at_epoch_ms
    OR NEW.account_id IS NOT OLD.account_id
    OR NEW.currency IS NOT OLD.currency
    OR NEW.fee_minor IS NOT OLD.fee_minor
    OR NEW.balance_after_minor IS NOT OLD.balance_after_minor)
BEGIN
    SELECT RAISE(ABORT, 'This transaction belongs to a loan');
END;
