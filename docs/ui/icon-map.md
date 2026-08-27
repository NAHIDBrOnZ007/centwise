# Icon Map: iOS (SF Symbols) ↔ Android (Material Symbols)

Rule: **same screen = same icon concept on both platforms.** iOS uses SF
Symbols, Android uses Material Symbols (`androidx.compose.material.icons`).
When adding a new icon, add a row here first.

## Tabs

| Purpose | iOS (SF Symbols) | Android (Material Icons) |
|---|---|---|
| Home | `house.fill` | `Icons.Default.Home` |
| Transactions | `list.bullet` | `Icons.Default.ReceiptLong` |
| Analytics | `chart.bar.fill` | `Icons.Default.BarChart` |
| Settings | `gearshape.fill` | `Icons.Default.Settings` |

## Settings rows

| Purpose | iOS | Android |
|---|---|---|
| Appearance | `paintbrush.fill` | `Icons.Default.Palette` |
| Currency | `coloncurrencysign.circle.fill` | `Icons.Default.Paid` |
| Categories | `square.grid.2x2.fill` | `Icons.Default.GridView` |
| Budgets | `chart.pie.fill` | `Icons.Default.PieChart` |
| Accounts | `building.columns.fill` | `Icons.Default.AccountBalance` |
| Subscriptions | `arrow.triangle.2.circlepath` | `Icons.Default.Autorenew` |
| Smart Rules | `sparkles` | `Icons.Default.AutoAwesome` |
| App Lock | `lock.fill` / `lock.open.fill` | `Icons.Default.Lock` / `Icons.Default.LockOpen` |
| Export CSV | `square.and.arrow.up` | `Icons.Default.Share` |
| FAQ | `questionmark.circle.fill` | `Icons.Default.HelpOutline` |
| About | `info.circle.fill` | `Icons.Default.Info` |

## Common actions

| Purpose | iOS | Android |
|---|---|---|
| Add | `plus` | `Icons.Default.Add` |
| Edit | `pencil` / `slider.horizontal.3` | `Icons.Default.Edit` |
| Delete | `trash` | `Icons.Default.Delete` |
| Back | `chevron.left` | `Icons.AutoMirrored.Filled.ArrowBack` |
| Forward / disclose | `chevron.right` | `Icons.AutoMirrored.Filled.KeyboardArrowRight` |
| Search | `magnifyingglass` | `Icons.Default.Search` |
| Done / selected | `checkmark.circle.fill` | `Icons.Default.CheckCircle` |
| Share | `square.and.arrow.up` | `Icons.Default.Share` |
| Empty state | `tray` | `Icons.Default.Inbox` |
| Expand / collapse | `chevron.down` / `chevron.up` | `Icons.Default.ExpandMore` / `ExpandLess` |

## Transaction types

| Type | iOS | Android |
|---|---|---|
| Expense | `arrow.up.right` | `Icons.Default.NorthEast` |
| Income | `arrow.down.left` | `Icons.Default.SouthWest` |
| Transfer | `arrow.left.arrow.right` | `Icons.Default.SwapHoriz` |
| Refund | `arrow.uturn.left` | `Icons.Default.Undo` |

## Categories (domain `icon` field — iOS names, Android equivalent)

The domain stores SF Symbol names (`TransactionModels`). Android maps the
top set; extend `providerIcon`/category mapping when rendering.

| Category | iOS | Android |
|---|---|---|
| Food & Dining | `fork.knife` | `Icons.Default.Restaurant` |
| Transport | `car.fill` | `Icons.Default.DirectionsCar` |
| Shopping | `bag.fill` | `Icons.Default.ShoppingBag` |
| Bills & Utilities | `bolt.fill` | `Icons.Default.Bolt` |
| Mobile Recharge | `antenna.radiowaves.left.and.right` | `Icons.Default.SignalCellularAlt` |
| Salary | `banknote.fill` | `Icons.Default.Payments` |
| Transfers | `arrow.left.arrow.right` | `Icons.Default.SwapHoriz` |
| Healthcare | `cross.case.fill` | `Icons.Default.MedicalServices` |
| Entertainment | `play.tv.fill` | `Icons.Default.LiveTv` |
| Education | `book.closed.fill` | `Icons.Default.School` |
| Cash Withdrawal (ATM) | `banknote.fill` | `Icons.Default.LocalAtm` |
| Other | `square.grid.2x2.fill` | `Icons.Default.Category` |

## Providers

| Provider type | iOS | Android |
|---|---|---|
| MFS (bKash/Nagad/Rocket/Upay) | `phone.bubble.left.fill` | `Icons.Default.PhoneAndroid` |
| Bank | `building.columns.fill` | `Icons.Default.AccountBalance` |
| Cash | `banknote.fill` | `Icons.Default.Savings` |

## Notes

- iOS keeps icon **names as strings** in models (SF Symbols available at
  runtime); Android renders equivalent Material icons in composables.
- Don't mix filled/outlined styles within one screen.
- New icon? Add the row here, then use it on both platforms in the same PR.
