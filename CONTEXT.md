# Keptang

Keptang is a voice-first expense tracker: spoken expenses are captured, transcribed, parsed into structured expenses, reviewed, and tracked against budgets.

## Language

**Capture**:
A single voice recording and its processing pipeline run (recording → transcription → parsing). One Capture can yield zero or more Expenses.
_Avoid_: Recording, voice note

**Expense**:
A single monetary transaction, parsed from a Capture or entered manually, carrying an amount, currency, category, date, and review status.
_Avoid_: Transaction, entry

**Approved Expense**:
An Expense the user has confirmed is correct (review status = Approved). Only Approved Expenses count toward Budgets or appear in the Expenses list.
_Avoid_: Confirmed expense

**Category**:
A free-text label classifying an Expense's purpose (e.g. "Dining", "Coffee"). Not a closed list — any string the parser or the user enters is valid, and Categories are matched by exact text, not normalized.
_Avoid_: Tag, type

**Preferred Categories**:
A user-curated list of Category names (Settings) offered as quick-select suggestions when entering a Category — for manual expense entry and the Budget target picker. Doesn't constrain what value a Category can actually hold; free text is still accepted everywhere (see [ADR-0001](docs/adr/0001-category-budgets-use-free-text-categories.md)).
_Avoid_: Category list, closed categories

**Default Currency**:
The single currency an installation is configured for. Budgets are computed in the Default Currency; an Expense recorded in a different currency doesn't count toward any Budget total, but is surfaced to the user rather than silently ignored (see [ADR-0002](docs/adr/0002-budgets-exclude-non-default-currency-expenses.md)).
_Avoid_: Base currency, home currency

## Budgets

**Budget**:
A spending limit the user sets over a recurring Budget Period, either as the Overall Budget or scoped to one Category. Its amount is set once and applies to every new Period until edited — it is not re-entered each Period.
_Avoid_: Budget line, limit

**Overall Budget**:
The Budget with no Category attached. Caps total spend across every Category combined, including Other. Set independently of Category Budgets — it is not required to equal their sum.
_Avoid_: Total budget, master budget

**Category Budget**:
A Budget scoped to one specific Category. Its Category is one of the free-text values that has actually appeared on an Expense — there is no separate, pre-declared category list to choose from (see [ADR-0001](docs/adr/0001-category-budgets-use-free-text-categories.md)).

**Budget Period**:
The recurring cadence a Budget resets on: Monthly (with a configurable start day) or Weekly (with a configurable start weekday). Chosen per Budget, not globally — the Overall Budget and each Category Budget can each run on their own cycle (see [ADR-0004](docs/adr/0004-budget-period-is-chosen-per-budget.md)).
_Avoid_: Cycle, billing period

**Current Period**:
The concrete, currently-active date range derived from a Budget's Budget Period (e.g. "Mar 1–31" for a monthly Budget starting on the 1st). Spend is only ever reported against the Current Period; Budgets carry no history from past Periods (see [ADR-0003](docs/adr/0003-no-rollover-between-budget-periods.md)).
_Avoid_: This month, active period

**Other**:
Spend in a Category with no Category Budget defined. Always counts toward the Overall Budget's total, shown grouped under a single "Other" line rather than hidden. Distinct from the Category value "Uncategorized": an Expense can be Uncategorized and still have a Category Budget defined for the literal category "Uncategorized", or be normally categorized but still land in Other if that Category has no Budget.
_Avoid_: Catch-all, miscellaneous, unbudgeted (as a noun)
