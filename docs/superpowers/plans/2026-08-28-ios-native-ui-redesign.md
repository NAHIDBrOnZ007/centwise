# iOS Native SwiftUI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Centwise's custom app-shell interactions with an iOS 16-compatible native SwiftUI baseline while preserving Rust-backed behavior.

**Architecture:** Native `TabView`, per-tab `NavigationStack`, `List`/`Form`, semantic typography/colors, and standard toolbar actions own ordinary interaction. Financial summaries, transaction semantics, and restrained Centwise branding remain focused custom components.

**Tech Stack:** Swift, SwiftUI, SF Symbols, UniFFI-backed repositories

**Spec:** `docs/superpowers/specs/2026-08-28-ios-native-ui-redesign.md`

## Global Constraints

- iOS 16 remains the deployment target.
- Use native SwiftUI components and semantic styles before custom controls.
- Do not copy PennyWise source, assets, text, or branding.
- Preserve Rust, database, parser, and repository behavior except the separately approved account-resolution contract.
- Windows validation is source-only; Xcode/device acceptance remains required.

---

### Task 1: Native tab and navigation shell

**Files:**
- Modify: `apps/ios/Centwise/App/MainTabView.swift`
- Modify: tab root screens only where navigation ownership must move.

**Interfaces:**
- Four native tabs: Home, Transactions, Analytics, Settings.
- Each tab owns a `NavigationStack`; Home can select Transactions through a binding.

- [ ] **Step 1: Run a source check** that fails while the custom tab bar remains.
- [ ] **Step 2: Replace** the manual `ZStack`/floating pill switcher with `TabView(selection:)` and native `.tabItem` labels.
- [ ] **Step 3: Keep** add-transaction presentation as a native sheet with toolbar actions.
- [ ] **Step 4: Re-run** the shell source check.

### Task 2: Native Settings structure

**Files:**
- Modify: `apps/ios/Centwise/Features/Settings/Screens/SettingsScreen.swift`

**Interfaces:**
- Grouped `List` sections with `NavigationLink`, `Toggle`, semantic labels, and destructive confirmation roles.

- [ ] **Step 1: Record** current routes and actions so none are lost.
- [ ] **Step 2: Replace** custom card rows with grouped native sections while preserving every destination and toggle.
- [ ] **Step 3: Use** semantic fonts/colors and accessible labels.
- [ ] **Step 4: Run** a route/action source audit against the recorded list.

### Task 3: Native transaction and home surfaces

**Files:**
- Modify: `apps/ios/Centwise/Features/Transactions/Screens/TransactionListView.swift`
- Modify: `apps/ios/Centwise/Features/Home/Screens/HomeScreen.swift`
- Inspect: `apps/ios/Centwise/Core/Design/Components/TransactionRow.swift`; keep it unchanged unless its root interaction conflicts with native `List` row selection.

**Interfaces:**
- Transactions use native search, toolbar menus, list rows, swipe delete, and standard sheets.
- Home keeps financial summaries but uses native navigation title/toolbar and list-like recent activity.

- [ ] **Step 1: Run source checks** for native searchable/list/swipe/toolbar usage.
- [ ] **Step 2: Refactor** Transactions to native list interaction without moving filtering/business logic into the view.
- [ ] **Step 3: Refactor** Home hierarchy using semantic sections and toolbar actions.
- [ ] **Step 4: Re-run** source checks and verify all repository calls remain unchanged.

### Task 4: Native forms and secondary screens

**Files:**
- Modify: add/edit screens under Accounts, Budgets, Subscriptions, Settings, and Transactions.
- Modify: `apps/ios/Centwise/Features/Transactions/Screens/ReviewQueueView.swift`
- Modify: `apps/ios/Centwise/Features/Settings/Screens/DataManagementScreen.swift`

**Interfaces:**
- `Form`/`Section`, native Cancel/Save toolbar items, destructive roles, confirmation dialogs, and failure-aware disabled states.

- [ ] **Step 1: Audit** each form's fields, validation, save method, and destructive action.
- [ ] **Step 2: Replace** custom action buttons with standard bordered prominent/plain/destructive role controls.
- [ ] **Step 3: Preserve** all fields and Rust-backed mutations.
- [ ] **Step 4: Run** a source audit proving each original save/delete destination remains.

### Task 5: Analytics, accessibility, and styling cleanup

**Files:**
- Modify: analytics Swift files under `apps/ios/Centwise/Features/Analytics/`.
- Modify: shared UI components only where fixed typography or custom press behavior prevents native accessibility.

**Interfaces:**
- Charts retain financial content and gain accessible summaries.
- Standard buttons provide native press feedback; custom styling remains only for financial/brand meaning.

- [ ] **Step 1: Add** accessibility labels/summaries for charts and amount semantics.
- [ ] **Step 2: Replace** avoidable fixed-size typography with semantic text styles in touched screens.
- [ ] **Step 3: Respect** Reduce Motion for any remaining explicit animations.
- [ ] **Step 4: Run** searches for custom tab bar use, hidden plain button styles, and inaccessible chart-only color meaning.

### Task 6: Verification and Mac handoff

**Files:** No production changes.

- [ ] **Step 1: Run** Rust workspace verification after the combined work.
- [ ] **Step 2: Run** Swift source boundary checks and `git diff --check` on Windows.
- [ ] **Step 3: Confirm** generated Xcode project references every changed/created Swift file.
- [ ] **Step 4: Document** required Xcode checks: compile, light/dark, small/large iPhone, Dynamic Type, VoiceOver, Reduce Motion, and all CRUD flows.
