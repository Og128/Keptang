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

THESIS: Two animals keep this ledger, and the app is their room. The chihuahua's world is ink
and paper-white; the cat's is apricot and cream. Both are drawn in one hand — the same black
outline, the same flat fills, the same pink — so the theme switch changes the light in the room,
never the room. It refuses the arrangement this category always ships: pastel pills and a donut
floating on a grey card deck, mascot demoted to an empty-state illustration. Here the mascot is
the system the colours came from, not a sticker placed on top of it.

OWN-WORLD: Every colour is sampled from the two mascots, not invented beside them. Shared across
both themes: ink #131313 as the only line colour, and the mascots' own pink #F5A89A as the single
accent — it is the ears on both animals, so it belongs to neither theme alone. CAT (light): a
committed apricot field #E89B4B carrying 30–60% of the surface, cream #FBF3E6 as the paper it
sits on, ink for every rule and figure. DOG (dark): near-black #0D0D0D ground, bone #F4F2ED as
ink, the same pink accent, the chihuahua's saddle brown #6B4A22 for secondary marks. Component
language is the mascots' own drawing: a 2dp ink outline on every container, flat fills with no
gradient, no elevation shadow anywhere, corners at a single 14dp radius matching the sticker
silhouettes. Type is the Material scale in the system face; the character comes from the line
weight and the colour fields, never from a display font.

STORY: The user opens the app after the spending already happened. They understand within one
screen how much of this period is gone and what is left, they believe it because the number is
right there in full and not abstracted into a chart, and they either close the app or tap one
row to fix a category the parser guessed wrong.

FIRST VIEWPORT: Dashboard, no cards anywhere. The period total sits top-left at display scale in
tabular figures, ink on the apricot field, the ฿ set smaller and raised. The relevant mascot sits
at the top-right of that field, cropped by the screen edge so it reads as present in the room
rather than pasted into a box. One 2dp ink rule runs edge to edge beneath. Then the budget
measures: each a full-width track, ink outline, filling in the accent pink, category name in
small caps left, remaining right-aligned in tabular figures. A second rule, then the latest
entries in a fixed three-field grid — description, category mark, amount — packed, ruled, no
gutters, no gaps between rows. The add action is a pink disc with an ink outline, at the thumb.

FORM: Brief-pinned by the user, which beats the roll under this skill's own rule. The direction
round for seed 598c429d is closed by that pin; its assigned direction (Lacquer & Gold) is
discarded, not deferred. Four disciplines from that round's declined challengers are kept, because
they are rigour rather than clothes: a budget renders as a continuous filled measure and never a
pass/fail pill; every expense row carries the same three fields at the same three positions; all
amounts are tabular figures aligned on the decimal and no chart ever stands in for a number; and
nothing floats — rules and packing replace cards, with no grey ground behind anything.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the
verdict, DESIGN.md, and every shipping raster carrying its provenance.

## Unresolved

- ColorTheme drops from four values to two. Persisted settings hold DEFAULT/DARK/LIGHT/AMOLED and
  are read by name, so a migration mapping is required or existing installs crash on read.
- Cat assets are 1232px, ~1MB each, 7.9MB total against the chihuahua set's 1.4MB at 512px.
- Which mascot appears on which screen when both are shown together.
