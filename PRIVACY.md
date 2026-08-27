# Centwise Privacy Policy

Last updated: 2026-08-22

Centwise is a local-first expense tracker. This document explains exactly
what the app accesses, where data lives, and what never happens.

## The short version

**Your financial data never leaves your device.** Centwise has no servers,
no accounts, and no analytics. Parsing happens on your phone.

## What Centwise accesses

### Android

- **SMS messages** (`RECEIVE_SMS`, `READ_SMS`): used only to detect
  transaction messages from supported Bangladeshi banks and MFS providers
  (bKash, Nagad, Rocket, and partner banks). Non-transaction messages are
  discarded immediately.
- **Notifications permission**: used only to show local notifications for
  newly tracked transactions and budget warnings.
- No contacts, no location, no camera, no microphone, no storage browsing.

### iOS

- Apple does not allow apps to read SMS. Centwise receives messages only
  through the **Shortcuts automation you set up yourself** (see
  `ios_shortcut.md`), or when you manually share a message via the Share
  Extension. Nothing is received without that explicit setup.

## What happens to a transaction SMS

1. The message is checked against known provider formats **on your device**.
2. Amount, balance, reference, and merchant are extracted locally.
3. The extracted transaction is stored in the app's local database.
4. The raw message text may be kept locally (optional in settings) to help
   you verify parsing — it is never transmitted.

## What Centwise never does

- ❌ No upload of SMS, transactions, or balances to any server
- ❌ No user accounts or sign-ups
- ❌ No advertising or tracking SDKs, no analytics
- ❌ No selling or sharing of data — there is nothing to share to
- ❌ No background network access for financial data

## Where data is stored

- In a local SQLite database inside the app's private storage.
- On iOS, in a shared app-group container so the app, Shortcuts intents, and
  Share Extension can use one database — still only on your device.

## Backups and exports (you initiate them)

- **CSV export** and future backups are created only when you tap Export,
  and are handed to you through the system share sheet. Where you save or
  send them is your choice and your responsibility.
- Backups may contain transaction history; handle them like bank statements.

## Deleting your data

- Deleting individual transactions works everywhere in the app.
- A full data-deletion option (Settings) permanently removes all
  transactions, accounts, and preferences from the device.
- Uninstalling the app removes the database.

## Permissions you can revoke

- Android: SMS and notification permissions can be revoked in system
  settings at any time; the app keeps working for manual entry and already
  tracked data.

## Contact

Questions about this policy or the app's data handling: open an issue in the
project repository or contact the maintainer directly.

---

**Summary for app-store review:** Centwise uses SMS/Shortcuts input solely
to create local expense records; no data is collected, transmitted, or
shared with anyone.
