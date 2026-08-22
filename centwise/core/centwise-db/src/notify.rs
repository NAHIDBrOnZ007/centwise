use std::sync::{Arc, Mutex};

/// Observer notified after any data table changes.
/// Listeners must be cheap: re-query, do not do heavy work here.
pub trait DataObserver: Send + Sync {
    fn data_changed(&self);
}

/// Thread-safe observer registry shared with the SQLite update hook.
pub struct ObserverRegistry {
    observers: Mutex<Vec<Arc<dyn DataObserver>>>,
}

impl ObserverRegistry {
    pub fn new() -> Self {
        ObserverRegistry {
            observers: Mutex::new(Vec::new()),
        }
    }

    pub fn add(&self, observer: Arc<dyn DataObserver>) {
        self.lock().push(observer);
    }

    /// Called from the update hook on the writing thread. Listener panics are
    /// contained so one bad listener cannot poison the write path.
    pub fn notify_all(&self) {
        for observer in self.lock().iter() {
            let _ = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
                observer.data_changed();
            }));
        }
    }

    fn lock(&self) -> std::sync::MutexGuard<'_, Vec<Arc<dyn DataObserver>>> {
        self.observers
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner())
    }
}

impl Default for ObserverRegistry {
    fn default() -> Self {
        Self::new()
    }
}
