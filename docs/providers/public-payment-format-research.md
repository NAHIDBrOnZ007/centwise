# Bangladesh payment format research — 2026-10-22

Scope: common shopping, service and household expenses. This is a primary-source
format review, not a market-share ranking. No user SMS or identifiers were sent
to websites. Fixture values are synthetic; third-party parser code was not used.

## Public SMS evidence and implemented coverage

| Evidence | Supported behavior | Regression coverage |
|---|---|---|
| [Gazipur Palli Bidyut guide](https://file-dhaka.portal.gov.bd/files/pbs1.gazipur.gov.bd/files/69eb60f6_1127_4b9d_b579_13a77e3f6f20/6d4c0f35d3baf6bf10afeabc28cb339a.pdf) publishes a labeled bKash BREBPrepaid bill confirmation and an intermediate request notice. | Confirmed payment is expense/bills; request awaiting confirmation is excluded. BREBPrepaid maps to Palli Bidyut. Biller account/contact fields do not identify the payer's wallet. | `official-breb-prepaid-bkash-confirmation`, request and failure fixtures. |
| [MetLife Bangladesh payment guide](https://www.metlife.com.bd/content/dam/metlifecom/bd/PDFs/Pay-Premium/bkashup.pdf) shows a bKash premium-payment receipt with Biller, MMYY, policy account, amount, fee and reference fields. | Expense/bills; merchant MetLife. Stop the merchant before the MMYY field. Policy number is not the payer account. | `official-metlife-bkash-premium-confirmation` and account-isolation tests. |
| [Daud Public School & College Rocket guide](https://www.dpsc.edu.bd/wp-content/uploads/2018/11/DAUD-PUBLIC-SCHOOL-AND-COLLEGE.pdf) publishes a completed payment receipt from 16216. | Recognize the paid-successfully wording, recipient before Bill No, dotted Txn.Id reference, amount and wallet balance; school/college payments use education. | `official-rocket-school-fee-confirmation`. |

These public guides demonstrate valid format families, not a promise that the
providers still use exactly these strings on every account. Some PDF fetches
timed out; their searchable indexed text exposed the templates. The MetLife PDF
was retrieved. `public-bd-payments.json` adapts the field order and wording, with
all amounts, balances, dates and identifiers replaced; it does not contain real
customer messages. No new sender aliases were inferred from branding alone.

## Shopping, delivery and cash payments

Existing merchant rules already cover Foodpanda, Chaldal, Shwapno, Agora, Meena
Bazar, Daraz, Aarong, Bata, Apex, Star Tech, Ryans, Pathao, Uber, DESCO, DPDC,
Titas Gas, Dhaka WASA, BTCL and Link3. `public_payments.rs` checks all 19 through
the existing bKash payment format, including expense direction, amount, category,
merchant and preserved wallet provider. These are synthetic counterparty
variations, not purported direct SMS templates from those shops.

Public business rules explain why separate order/status messages need care:

- [Aarong billing](https://www.aarong.com/default/billing-and-payments) supports
  advance payment and cash on delivery. An order invoice can precede payment.
- [Foodpanda Bangladesh terms](https://www.foodpanda.com.bd/contents/terms-and-conditions.htm)
  distinguish order confirmation from payment, including cash on delivery.
- [Daraz terms](https://www.daraz.com.bd/terms-conditions/) distinguish order
  communications, dispatch and the purchase contract. An order-status SMS alone
  does not prove that money moved.
- [Pathao payment-on-delivery](https://pathao.com/blog/pathao-parcel-payment-on-delivery/?lang=en)
  sends a payment link before the receiver pays. A payment-link SMS is not a debit.
- [DESCO prepaid guide](https://prepaid.desco.org.bd/doc/prepaid-leaflet.pdf)
  describes a request stage followed by a confirmation. Indexed text did not
  establish an additional full amount-bearing direct SMS template to implement.

Negative fixtures cover unpaid/COD orders and a pending Pathao payment link.
The previously agreed Ryans purchase-confirmation policy remains supported.
No direct shop/ride SMS format was invented from the presence of a brand or price.
Cash spending can be parsed when an actual supported purchase receipt arrives;
no SMS parser can infer cash purchases for which no message exists.

## Shared behavior and limits

The shared Rust parser, merchant rules, fixtures, provider catalog and wallet
ingestion tests own this change. Android/iOS use the same resulting contract;
no FFI shape, schema, UI or localization fields change. A new merchant dictionary
rule supplies defaults; existing user-created category rules are preserved.
No existing transactions are rewritten. Merchant-plus-bank confirmations of one
payment still require reconciliation when they lack a shared identity. Native
libraries/device behavior require separate platform verification.

Verified on Windows: `cargo test --workspace --locked` (306 tests),
`cargo fmt --all --check`, and
`cargo clippy --workspace --all-targets --locked -- -D warnings` passed. The
4,714-message per-row snapshot remains unchanged by this follow-up: 4,339 parsed,
375 ignored, zero review. Cargo emitted filesystem hard-link fallback warnings.
