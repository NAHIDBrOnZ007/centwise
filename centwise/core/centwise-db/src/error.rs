use std::fmt;

/// Errors surfaced by the database layer.
#[derive(Debug)]
pub enum DbError {
    /// SQLite operation failed.
    Sqlite(String),
    /// A stored value did not match the expected domain format.
    Corrupt(String),
    /// The caller supplied invalid input.
    Invalid(String),
    /// An SMS reference has already been stored or queued.
    DuplicateReference(String),
    /// A transaction id has already been stored.
    DuplicateTransaction(String),
}

impl fmt::Display for DbError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            DbError::Sqlite(message) => write!(f, "database error: {message}"),
            DbError::Corrupt(message) => write!(f, "corrupt data: {message}"),
            DbError::Invalid(message) => write!(f, "invalid input: {message}"),
            DbError::DuplicateReference(reference) => {
                write!(f, "duplicate transaction reference: {reference}")
            }
            DbError::DuplicateTransaction(id) => write!(f, "duplicate transaction id: {id}"),
        }
    }
}

impl std::error::Error for DbError {}

impl From<rusqlite::Error> for DbError {
    fn from(error: rusqlite::Error) -> Self {
        DbError::Sqlite(error.to_string())
    }
}

pub type DbResult<T> = Result<T, DbError>;
