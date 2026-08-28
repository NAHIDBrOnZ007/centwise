# Centwise iOS Native SwiftUI UI Redesign

**Status:** Approved for implementation
**Date:** 2026-08-28
**Scope:** iOS UI only
**Target:** Native SwiftUI on iOS 16+

## 1. Purpose

Redesign the Centwise iOS interface so it feels like a native Apple app. Use
SwiftUI and Apple's standard controls, navigation, typography, materials,
gestures, sheets, and interaction behavior as the default foundation.

Centwise should keep its own identity through its logo, product language,
financial meaning, category colors, and restrained accent color. The UI should
not imitate PennyWise source code, assets, branding, or implementation.

## 2. Current UI assessment

Centwise already uses SwiftUI primitives such as `Button`, `NavigationStack`,
`NavigationLink`, `Toggle`, sheets, SF Symbols, and system materials. However,
many important surfaces are currently custom:

- `CentwiseButton` replaces standard button styles with custom padding, colors,
  corner radius, and plain-button behavior.
- `CentwiseCard` and `glassCard` use custom rounded rectangles, borders, and
  shadows rather than standard grouped/list/form surfaces.
- `MainTabView` uses a custom floating pill tab bar instead of native `TabView`.
- Several screens use manually selected font sizes, shadows, spacing, and
  colored controls.

The redesign should reduce custom styling where Apple already provides a
consistent component and reserve custom components for product-specific needs.

## 3. Design principles

1. Native SwiftUI first.
2. Use the system component before creating a custom replacement.
3. Use system typography and semantic colors by default.
4. Use SF Symbols instead of custom icon artwork for interface actions.
5. Use standard navigation, sheets, alerts, dialogs, forms, lists, and controls.
6. Keep financial meaning clear through amount formatting, semantic colors, and
   labels—not decoration alone.
7. Use animation to explain state changes, not to make every control move.
8. Support Dynamic Type, VoiceOver, Reduce Motion, light mode, and dark mode.
9. Keep the UI compatible with the current iOS 16 deployment target unless a
   later minimum version is explicitly approved.
10. Do not change Rust, FFI, database, parser, or repository behavior as part
    of this UI redesign.

## 4. Native component policy

### Use standard SwiftUI components for

- Primary actions: `Button` with `.buttonStyle(.borderedProminent)` where
  appropriate.
- Secondary actions: bordered or plain system buttons according to context.
- Destructive actions: system destructive role buttons and confirmation dialogs.
- Main navigation: `TabView` with `.tabItem` and `NavigationStack`.
- Settings and editable data: `List`, `Form`, `Section`, `NavigationLink`, and
  `ToolbarItem`.
- Selection: `Picker`, `Toggle`, `DatePicker`, and `Menu`.
- Progress and loading: `ProgressView` and standard disabled/loading states.
- Temporary actions: `.sheet`, `.popover` where supported, `.alert`, and
  `.confirmationDialog`.
- Empty content: native empty-state composition, with
  `ContentUnavailableView` only behind the required availability check.
- Icons: SF Symbols with clear labels and accessibility descriptions.

### Keep limited custom components for

- Financial amount display and income/expense/refund semantics.
- Transaction rows when they add useful financial information beyond a native
  list row.
- Category and account visual identity.
- A small Centwise accent treatment applied to native controls.
- Brand-specific onboarding and dashboard summary content.

Custom components must expose normal SwiftUI behavior and must not hide the
system accessibility or interaction model.

## 5. Visual language

### Surfaces

- Prefer system backgrounds, grouped backgrounds, `List` sections, and native
  material surfaces.
- Keep cards only where they improve hierarchy or summarize financial data.
- Remove unnecessary borders and heavy shadows.
- Use continuous rounded shapes sparingly and consistently.
- Keep the existing AMOLED option only if it does not make native controls or
  text illegible.

### Color

- Preserve Centwise emerald as the main brand accent.
- Use semantic system colors for primary text, secondary text, separators, and
  backgrounds wherever possible.
- Keep income green, expense red, transfer blue, and refund amber for financial
  meaning.
- Avoid coloring every settings row icon unless the color communicates a real
  category or action distinction.

### Typography

- Prefer `.font(.body)`, `.headline`, `.title`, `.caption`, and related semantic
  text styles.
- Use `CentwiseTypography` only where a product-specific hierarchy is needed.
- Do not rely on fixed font sizes that break with Dynamic Type.

### Materials and glass

- The current iOS 16 target should use compatible system materials such as
  `.thinMaterial` or `.ultraThinMaterial` where they improve hierarchy.
- Newer Liquid Glass APIs may be adopted only with availability checks and only
  after deciding whether the minimum iOS version should change.
- Glass must not be applied to every surface. Content readability and contrast
  take priority.

## 6. Screen redesign direction

### Main tab structure

- Replace the custom floating pill tab bar with native `TabView` unless a
  later visual review proves a small brand treatment is necessary.
- Use four tabs: Home, Transactions, Analytics, and Settings.
- Use SF Symbols and native tab selection behavior.
- Keep navigation state inside each tab's `NavigationStack`.

### Home

- Use a native navigation title and toolbar actions.
- Keep the financial summary as a focused Centwise component.
- Present recent transactions in a native list-like structure with clear amount
  alignment and familiar disclosure/detail behavior.
- Use a standard toolbar or native floating action treatment only if the add
  action remains clearly discoverable and accessible.

### Transactions

- Use `List` with native row behavior, swipe actions, and search where useful.
- Use native filter controls through a toolbar menu or sheet.
- Use standard edit and delete confirmation behavior.
- Keep transaction detail and add/edit flows in native sheets or navigation
  destinations.

### Analytics

- Preserve charts and financial summaries.
- Use native navigation, scrolling, section hierarchy, and semantic labels.
- Ensure charts have accessible summaries and do not depend only on color.

### Settings

- Move toward a native grouped `List`/`Form` settings structure.
- Use standard rows, `NavigationLink`, `Toggle`, `Picker`, and toolbar buttons.
- Keep only the profile header and product-specific summary pieces custom.
- Reduce repeated rounded cards, heavy shadows, and decorative icon colors.

### Add/edit flows

- Prefer `Form`, `Section`, `TextField`, `Picker`, `DatePicker`, and native
  validation messaging.
- Use `.toolbar` for Cancel and Save actions.
- Use native keyboard dismissal, focus management, and disabled Save states.
- Preserve all existing Rust-backed repository operations and validation.

### Review queue and data management

- Use native list rows, badges, alerts, confirmation dialogs, and sheets.
- Keep raw SMS readable with appropriate monospaced text, but do not expose
  sensitive content unnecessarily in previews or accessibility labels.

## 7. Interaction and animation

- Use SwiftUI's standard button press feedback and navigation transitions first.
- Use short, purposeful `withAnimation` transitions for tab changes, row
  insertion/removal, filters, and loading state changes.
- Avoid manual spring effects on every button.
- Respect `accessibilityReduceMotion` and provide a non-animated equivalent.
- Keep haptic feedback limited to meaningful confirmations, selection changes,
  success, warning, and error events.
- Do not add animation to database or parser operations that suggests work has
  completed before Rust returns success.

## 8. Data and architecture boundary

The redesign must not move business logic into views or ViewModels.

```text
SwiftUI screen
    → native ViewModel/repository adapter
    → Centwise UniFFI backend
    → Rust domain/database/query
```

The redesign must preserve:

- Rust-owned SQLite and migrations.
- Rust-owned writes and account balance updates.
- Rust-owned SMS parsing, categorization, rules, and review queue decisions.
- Existing generated Swift FFI bindings.
- Existing localization and data models.

## 9. Accessibility and localization

- All controls must have meaningful labels and hints.
- Dynamic Type must not clip transaction titles, amounts, or settings subtitles.
- Do not communicate income/expense only through red or green.
- Support VoiceOver ordering for cards, summaries, charts, and transaction rows.
- Support Bengali strings and mixed Bangla/Latin content.
- Test long localized strings and right-sized controls.
- Respect Reduce Motion, Increase Contrast, and larger accessibility text.

## 10. Verification requirements

Before calling the redesign complete:

- Run Rust workspace tests to confirm the shared core remains healthy.
- Confirm no FFI API or generated binding changes were introduced accidentally.
- Build the iOS app through the macOS/GitHub Actions path.
- Check light mode and dark mode.
- Check iOS 16-compatible behavior.
- Check small iPhone screen and a larger iPhone screen.
- Check Dynamic Type and VoiceOver basics.
- Check Reduce Motion.
- Check loading, empty, error, and success states.
- Confirm transaction, budget, account, subscription, rule, and review-queue
  operations still use the Rust backend.

## 11. Implementation order

1. Establish native navigation and `TabView` structure.
2. Replace custom button styling with native button styles where appropriate.
3. Simplify Settings into native grouped sections.
4. Simplify add/edit flows with native forms and toolbars.
5. Refine Home and Transactions while preserving financial summary components.
6. Refine Analytics, Review Queue, and Data Management.
7. Apply limited Centwise brand treatments after the native baseline works.
8. Build and perform visual/accessibility verification on macOS/iPhone.

## 12. Acceptance criteria

The redesign is successful when:

- The app feels recognizably native to iOS.
- Standard Apple controls are used for standard Apple interactions.
- Custom styling is limited to Centwise identity and financial-specific content.
- Navigation, sheets, forms, lists, alerts, and buttons behave predictably.
- The interface works in light mode, dark mode, Dynamic Type, VoiceOver, and
  Reduce Motion.
- No Rust core, FFI contract, database behavior, or SMS ingestion behavior is
  changed by the UI work.
- The final UI is independently implemented in Centwise and does not copy
  PennyWise source code, assets, strings, or branding.
