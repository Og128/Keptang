# Category Budgets use free-text categories, no closed vocabulary

Categories are already free text with no closed list — the parser keyword-matches into a handful of values, but manual entry and the review screen accept any string, matched by exact text. Rather than introduce a closed/curated category list for Budgets to anchor to, a Category Budget simply targets whatever category strings have actually appeared in the user's Expenses. This was chosen over closing the vocabulary because doing so would mean constraining the parser and every category-editing screen, real scope beyond a first budgeting pass that's meant to work with existing data as-is.

**Consequences**: near-duplicate category strings (e.g. "Coffee" vs "coffee" vs "Café") will fragment Category Budget tracking rather than being merged — accepted for v1.
