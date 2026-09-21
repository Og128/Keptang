# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

A single person tracking their own day-to-day spending, in a currency the app is configured for.
There is one audience and no second one: no team, no client, no shared account.

The defining situation is hands-busy and away from a desk: leaving a restaurant, a shop, a taxi.
The job is to record what was just spent in the two or three seconds before it is forgotten, and
— separately, later, sitting down — to understand where the month went.

Those two moments are different jobs with different tools. Capture happens from the home screen
and never opens the app. Understanding happens inside the app and is never urgent.

## Product Purpose

Record an expense by speaking it, so that tracking costs nothing at the moment of spending.

Everything runs on the device: recording, transcription, parsing, storage. No account, no
network call, no cloud. Success is that a month of spending is in the database without the user
ever having felt they were doing data entry.

## Positioning

The mechanism a neighboring expense app could not truthfully copy is the capture path: a
home-screen widget tap starts a foreground recording service directly, without ever launching the
app. Speech is transcribed on-device, then parsed by a pure-Kotlin parser that extracts amount,
date, category, description, account and payment method from natural speech — including several
expenses from one sentence.

Categorisation is learned from the user's own corrections rather than shipped as a fixed taxonomy.
A category is free text; when the user recategorises an expense, that description-to-category pair
is remembered and applied to future captures.

## Operating Context

- Two home-screen widgets: a microphone (voice capture) and Quick Add (a typed popup parsed by the
  same pipeline). Neither opens the app.
- Notifications are the feedback channel for capture, because the app is not open when it happens.
- The app itself is for reviewing ambiguous captures, browsing and editing history, setting
  budgets, tagging, CSV export, and settings.
- The app is configured for a single default currency, THB by default. Expenses in another
  currency are recorded but excluded from budget totals, and surfaced rather than silently
  dropped.
- Distribution is a signed APK installed directly; there is no Play Store listing.

## Capabilities and Constraints

Confirmed functionality that must survive any redesign; the feature set is fixed:

- Voice capture, Quick Add, manual entry, recurring expenses
- Inbox (captures) and Review (ambiguous or failed captures)
- Expenses list with search, category filter, tag filter, and a calendar view
- Dashboard with a configurable card order and visibility
- Budgets: overall and per-category, monthly or weekly, each on its own cycle
- Categories with colour and icon, editable
- Tags, CSV export, and a user-selectable colour theme

Constraints:

- Jetpack Compose, Material 3, Room, minSdk 26, targetSdk 34. No dependency-injection framework;
  a hand-rolled ServiceLocator.
- Local-first and offline by construction. No network permission is requested.
- Phone, portrait. Tablet and landscape are explicitly out of scope (user decision).
- The domain vocabulary in `CONTEXT.md` is authoritative for what things are called in code.
  Whether the UI surfaces those exact words is a design decision, not a product one.

## Brand Commitments

- Name: Keptang.
- The mascot illustrations are binding and must be kept: `m_expense`, `m_concerned`, `m_reading`,
  `m_relaxed`, `m_settings` (`app/src/main/res/drawable-nodpi/`), and the widget microphone
  character `widget_mic_blanc` with its two mouth-open/closed animation frames.
- Nothing else is pinned. Navigation structure and UI wording are open.

## Evidence on Hand

- The only real data is whatever expense history exists on the installed device, which is personal.
  Screenshots and demonstrations must not be published without checking what amounts they show.
- `CONTEXT.md` (domain glossary) and `docs/adr/` (four accepted decisions) are the product's own
  written record.
- No testimonials, no user research, no usage metrics, no competitor benchmarks exist. Future work
  must not invent them.

## Product Principles

1. **The fast path never opens the app.** Anything that makes capture require the UI is a
   regression, however good the UI is.
2. **Never silently lose or invent data.** A capture that cannot be parsed is kept with its audio
   for review; an amount that was not heard is never guessed.
3. **The user's own language wins.** Categories are free text and learned from corrections, not
   imposed by a taxonomy.
4. **One person's scale.** Aggregation in memory, no pagination pressure, no multi-user concerns.
   Design for a few thousand rows, not a million.
5. **Offline is the design, not a fallback.** There is no degraded online mode to account for.

## Accessibility & Inclusion

No standard is contractually required, but the current build was audited against Material 3 and
TalkBack. Established needs: touch targets of at least 48 dp, every interactive control labelled,
and type that follows the system font-size setting (the codebase already uses the Material type
scale exclusively, with no hard-coded text sizes — this must not regress).
