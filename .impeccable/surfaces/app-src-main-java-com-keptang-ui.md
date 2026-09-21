---
version: 1
slug: "app-src-main-java-com-keptang-ui"
primary_target: "app/src/main/java/com/keptang/ui"
related_targets: []
---

Scope: the whole app UI (Dashboard, Expenses, Budgets, Settings, Inbox, Review, forms).
Visitor mode: Operate — the user is completing a task, never being sold to.
Audience: one person, their own money, phone in one hand, portrait only.

## Direction contract

THESIS: A contemporary banking app that two animals live in. The craft level and structural
conventions come from the current Lloyds app — bold brand colour used sparingly against stark
black-and-white type, generous rounded cards, an illustration suite, a tone with humour in it —
and every fact, colour and character comes from the mascots. It refuses the version of this
category that arrives by default: a sober grey utility where the illustration is demoted to an
empty state. Here the animal is on every screen, and the palette was sampled off its fur.

OWN-WORLD: Colours sampled from the artwork, never picked beside it. Shared by both themes: ink
#131313, and the mascots' own ear-pink #F5A89A. CAT (light): cream #FBF3E6 surfaces, apricot
#E89B4B used at full strength on the things that matter — the period total, the budget measures,
the add action — and nowhere else, so it keeps its force. DOG (dark): near-black #0D0D0D, bone
#F4F2ED ink, same accents. Generous rounded cards at one 14dp radius, a 2dp ink outline instead of
elevation, flat fills, no gradients or shadows anywhere. Type is Bricolage Grotesque, embedded:
chosen on a measured constraint — of six candidates only it and Schibsted Grotesk carry ฿, which
this app prints on every amount — and kept for its optical-size axis, display forms at the hero
number and calm forms in a list, which is the one structural thing GT Ultra does that a single
static face cannot.

STORY: The user opens the app after the spending already happened. They see how much of this
period is gone before reading a word, they trust it because the number is there in full rather
than abstracted into a chart, and they either close the app or tap one row to fix a category the
parser guessed wrong.

FIRST VIEWPORT: Dashboard, keeping its existing configurable card order (the user asked for the
feature to survive). A greeting row with the mascot at the top right, cropped by the screen edge.
Then the period card, the one apricot field on the screen: the total at display scale in tabular
figures, ฿ smaller and raised, counting up to its value on open. Below it the budget cards, each a
continuous filled measure with the category in small caps left and the remaining amount
right-aligned — never a pass/fail pill. Then recent expenses in a fixed three-field grid:
description, category mark, amount. The add action is a pink disc with an ink outline, docked at
the centre of the bottom bar.

FORM: Brief-pinned by the user across two rounds, which beats the roll under this skill's own
rule; the direction round for seed 598c429d is closed and its assigned direction discarded. Of the
four disciplines kept from that round's declined challengers, three hold: a budget renders as a
continuous filled measure; every expense row carries the same three fields at the same three
positions; amounts are tabular figures aligned on the decimal and no chart stands in for a number.
The fourth — "nothing floats, rules and packing instead of cards" — is overridden by the user's
explicit choice of generous cards, recorded here so it reads as a decision rather than drift.
Motion is exactly two moves, both chosen by the user: numbers count up to their value, and the
mascot's pose answers the screen's state. No screen transitions.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the
verdict, DESIGN.md, and every shipping raster carrying its provenance.

## Delivery

Foundations (type scale, component set, restyled bottom bar) then the Dashboard alone as the
witness screen. If the user accepts it, the same language is applied to the remaining screens
without asking again.

## Unresolved

- The four two-mascot illustrations the user is producing, and the theme-selection and About
  screens two of them imply.
- The bottom bar stays hand-rolled by the user's choice, so its missing selected-state semantics
  must be fixed by hand rather than inherited from NavigationBar.
