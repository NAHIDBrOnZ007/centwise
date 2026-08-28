use centwise_db::notify::DataObserver;

/// Errors surfaced to Kotlin/Swift.
#[derive(Debug, uniffi::Error)]
pub enum CentwiseError {
    Db { reason: String },
    Invalid { reason: String },
}

impl std::fmt::Display for CentwiseError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            CentwiseError::Db { reason } => write!(f, "database error: {reason}"),
            CentwiseError::Invalid { reason } => write!(f, "invalid input: {reason}"),
        }
    }
}

impl From<centwise_db::DbError> for CentwiseError {
    fn from(error: centwise_db::DbError) -> Self {
        match error {
            centwise_db::DbError::Invalid(reason) => CentwiseError::Invalid { reason },
            other => CentwiseError::Db {
                reason: other.to_string(),
            },
        }
    }
}

/// Implemented in Kotlin/Swift. Fires after every data write.
#[uniffi::export(callback_interface)]
pub trait ChangeListener: Send + Sync {
    fn on_data_changed(&self);
}

/// Adapter that lets a foreign listener plug into the database registry.
pub(crate) struct ForeignObserver(pub(crate) Box<dyn ChangeListener>);

impl DataObserver for ForeignObserver {
    fn data_changed(&self) {
        self.0.on_data_changed();
    }
}
