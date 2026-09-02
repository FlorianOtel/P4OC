# Synthesis: Server filter mini-cards

## Decision
Use a fixed-order horizontally scrolling rail of independently toggleable, flat two-line server mini-cards.

```text
SERVERS · 3/5 ON                              ›
┌──────────────────────┐ ┌──────────────────────┐ ┌────
│ ● archive         ON │ │ ○ studio         OFF│ │ ●
│ archive.lan:4096  12 │ │ studio.lan:4096    7│ │
└──────────────────────┘ └──────────────────────┘ └────
```

The connection glyph communicates health only. `ON/OFF` communicates inclusion in blank-search Home browsing. Session count occupies a stable bottom-right column. The whole card is one semantic toggle target.

## Key considerations that shaped this

The selected direction preserves the user's simple two-line terminal reference while separating filter selection from connection health. Horizontal overflow is communicated by a partial next-card peek, a trailing or leading chevron based on scroll position, and `N/M ON` summary text. Cards retain stable ordering and rail position.

Blank search combines and newest-first interleaves sessions/workspaces from all enabled servers. All-on is implicit, so there is no synthetic All card. All-off yields zero results with a clear Select all recovery action. Nonblank global search preserves but temporarily ignores enabled selections and clearly discloses that override.

## What we ruled out and why

- Three-line checkbox cards: clearer but unnecessarily tall for the primary Home rail.
- Filled Material chips/cards: too webby and visually heavy.
- Selection represented only by dot, color, border, or rail: ambiguous with connection health or inaccessible without color.
- Persistent expanded ledger: consumes too much Home workspace.
- A synthetic All card: can drift out of sync with independent toggles.

## What we parked for later

A three-line checkbox card remains a fallback if phone testing shows users confuse the leading connection glyph with selection despite explicit ON/OFF text.

## Open questions

- Exact card width after testing long display names, IPv6 endpoints, and maximum supported font scaling.
- Whether edge chevrons are informational only or 48dp scroll-by-one actions.
- Whether counts use a bare numeral visually or a compact localized noun; accessibility semantics always announce “N sessions.”
