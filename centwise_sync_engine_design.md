# Centwise — Offline Sync & Hub Election Engine

## Final Design (v1.0)

---

## 1. Goal

A mess/family expense-sharing app that works **fully offline**, with **no server, no account, no internet dependency**. Members' phones sync directly with each other over the **local home Wi-Fi**, exactly like AirDrop-style local discovery — but for expense data, continuously, in the background.

---

## 2. How Phones Find Each Other (Discovery & Trust)

- When a member's phone joins the home Wi-Fi, it broadcasts itself locally (mDNS/local network discovery) — the same tech AirDrop uses to "see" nearby devices.
- Only **paired/trusted phones** respond to each other. Trust is established once, at setup:
  - The group creator generates a **QR code** containing the group ID and an encryption key.
  - Each member scans it once, while on the same Wi-Fi.
  - From then on, phones only recognize and talk to devices paired this way — a random stranger on the same Wi-Fi is invisible to the app.
- Removing a member = revoke their key on all phones → instant lockout from future syncs.

---

## 3. How Data Moves (Sync Engine)

- Every phone keeps its **own complete local database**. Nothing is centralized.
- Every action (add/edit/delete an expense, settle up, etc.) is saved as a small **"operation"** with a unique, ever-increasing ID. Operations are **never overwritten or deleted** — this gives a full, tamper-proof history.
- When two trusted phones connect:
  1. Phone A says: *"I have operations up to #1041."*
  2. Phone B replies: *"I only have up to #1038 — send me 1039 to 1041."*
  3. A sends just those 3 operations. B applies them and updates its balances.
- Only the **gap** is exchanged — never a full database copy. Fast and lightweight.
- If two people edit the same expense while both offline, both edits arrive and are kept in history; the current value is resolved by timestamp (last-write-wins), but nothing is silently erased — the manager can never quietly alter old records.

---

## 4. The Core Problem This Design Solves

Without a server, two phones can only sync if they are **online on the same Wi-Fi at the same moment**. If everyone's app is closed and phones wake up at random times, they may **never** overlap — so data could get stuck.

**Solution: one phone always stays awake and reachable — the "Hub."**

---

## 5. Roles

| Role | Behavior |
|---|---|
| **Hub** | One phone at a time. Always listening on the local network (like a small always-open post office). Chosen because it's home, charging, and has good battery. |
| **Member** | Every other phone. App is mostly closed. Wakes up automatically roughly every **15 minutes** for about **60 seconds**, says hello, syncs, then goes back to sleep. |

Every phone can become the Hub — the role is not fixed to one person's device.

---

## 6. Hub Score (Who Is Fit to Be Hub)

Each phone calculates its own score locally:

| Factor | Points |
|---|---|
| Currently charging | +40 |
| Battery percentage | up to +30 (100% battery = +30) |
| Average hours/day this phone stays on home Wi-Fi (learned over time) | up to +20 |
| Background/battery-optimization disabled for the app | +10 |
| **Max possible** | **100** |

This naturally favors the phone that's home all day and plugged in (e.g., sitting on a charger) over someone who's out at office and rarely home.

---

## 7. When and How the Hub Changes

The Hub does **not** change randomly. It only changes in exactly these situations:

### A) No Hub exists
- If a phone joins the Wi-Fi and hears no Hub for **10 seconds**, it declares itself Hub. No permission or election needed — being alone is enough.

### B) Hub goes silent
- If no phone has heard from the Hub for **60 seconds** (crashed, battery saver killed it, phone died), the remaining phones elect a new Hub among whoever is currently awake.

### C) Normal term rotation (every 30 minutes)
- The Hub serves in **30-minute terms**.
- During its term, every Member's wakeup "hello" includes their score. The Hub records these in a **scoreboard**, along with when each phone last checked in.
- A phone is considered **"reachable"** only if it checked in within the **last 5 minutes**.
- At the end of the 30-minute term:
  - The Hub looks at the scoreboard and picks the **best-scoring phone that is currently reachable**.
  - If that candidate beats the current Hub's score by at least **+15 points**, the Hub hands off the role to them.
  - If no reachable candidate beats it by +15, the Hub **renews its own term silently** — no interruption, no message, nothing visible to users.
- **Important:** the Hub never waits around for the single best-scoring phone in existence to wake up. It only ever compares against phones that are reachable right now. If the true best phone (e.g., 95 score) checked in 8 minutes ago, it's outside the 5-minute window and is simply skipped for this term — it gets picked up automatically at the **next** term-end if it's reachable then.

### D) Emergency handoff (skips waiting entirely)
- If the Hub's battery drops below 20%, its charger is unplugged, or it's about to leave the Wi-Fi, it immediately hands off to the best reachable phone right away — no waiting for the 30-minute term, and the +15 margin rule is waived (anyone reachable is better than a dying Hub).
- If nobody is reachable at that moment, the Hub announces it's leaving; the next phone that wakes up becomes Hub.

### E) Two Hubs appear at once (split-brain)
- Harmless — the two Hubs simply sync with each other like normal peers.
- At the very next term check, the rule resolves it automatically (higher score wins; if scores tie, lower device ID wins). Self-heals within one cycle, no action needed.

---

## 8. What a Wakeup Actually Does

This is the most important stability rule:

> **A wakeup is just a check-in. It is never an election.**

Every ~15 minutes, a Member's phone wakes for 60 seconds and does exactly this:
1. Says "Hi, I'm here, my score is X" to the Hub.
2. Syncs any missing operations in both directions.
3. Goes back to sleep.

It does **not** compare its score to the Hub's score, and it does **not** attempt to take over. A living Hub is **never** challenged outside of the term-end check (section 7-C) or an emergency (7-D). This is what prevents the Hub from flipping back and forth every few minutes even with many phones waking up at different random times.

---

## 9. Delivering a Handoff to a Sleeping Phone

A sleeping phone cannot receive anything directly over the network — there's no "message waiting on the internet." Instead:

- When the current Hub decides phone X should be the next Hub but X is asleep, the Hub simply **stores that decision in its own local database** (a "pending offer").
- The next time X wakes up and checks in (within ~15 minutes), the Hub delivers the offer during that same 60-second window, and X becomes the new Hub immediately.
- Nothing is lost or delayed indefinitely — the maximum wait is one wakeup cycle.

---

## 10. The Absolute Safety Net

Even if every mechanism above somehow fails — no Hub, nobody wakes up in sync, everyone's Wi-Fi is off at different times — the moment **any user simply opens the app** while on the mess/home Wi-Fi, it immediately performs a full sync with whoever it can reach. **No data is ever permanently lost**; it just waits patiently until two phones happen to be reachable at the same time.

---

## 11. One-Line Mental Model

> **Hub = the always-open post office. Members = people dropping by on their rounds. Operations = numbered letters that are never lost or duplicated. Score = who has the shop with free electricity, so they host the post office. Every 30 minutes, the post office manager quietly checks if someone better should take over — but only hands over the keys if that person is clearly better *and* actually present. If the manager has to leave suddenly, they hand the keys to whoever is around immediately.**

---

*End of document.*
