# No rollover between Budget Periods

Budgets reset flat at the start of every Current Period — unused amounts don't carry forward, and overspend doesn't carry as a debt against the next Period. This keeps "spend so far this Period" a pure function of the Period's own Expenses, with no chained state across Periods. Envelope-style rollover (YNAB-style carry-over) was rejected for this pass: it requires storing and chaining per-Period history, and there's no historical reporting yet for a user to make sense of a carried balance against.

**Revisit**: if historical/trend reporting across past Periods is added later, that's the natural point to reconsider rollover too, since the two features share the same underlying need (Periods retaining state from their predecessor).
