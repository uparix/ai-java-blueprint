# SwarmForge Defect Ledger

Single source of truth for known defects and queued work. Update this file in
the same commit that opens or closes a defect — do not leave defect state living
only in commit messages.

**ID scheme:** `D<n>` for a defect, `D<n><letter>` when one defect is split into
independently-shippable parts (e.g. `D1a`, `D1b`).
**Status values:** `open` · `in-progress` · `closed` · `wontfix`

---

## Open

_None._

---

## Closed

_None._

---

## Template for a new entry

```
### D<n> — <short title>
- **Status:** open
- **Opened:** <YYYY-MM-DD>
- **Spec:** features/<name>.feature   (Rule 2: write this before any code)
- **Summary:** <root cause / observed behavior / desired behavior>
- **Closed by:** <commit sha>   (fill in when closed)
```
