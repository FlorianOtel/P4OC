# Server filter mini-cards
Date: 2026-07-15
Stage: UI exploration
Goal: Explore horizontally scrollable, independently toggleable server filter cards for Home
Domain: Android compact TUI UX
Technique: Perspective multiplication

## Raw Idea
"so what I think i like is a scrollable horizontal row (with ui affordance) and instead of selecting a single server, they're either on or off and can be toggled. gimme some designs for the mini cards. I think I like smth like just [reference image: hollow status circle + archive, second line archive.lan:4096] and maybe then a small indicator for session count or smth?"

## Context
Home search is global when nonblank. With blank search, enabled server cards form a multi-select browse filter. The row must visibly afford horizontal scrolling, remain compact, preserve endpoint-derived server identity and textual semantics, avoid shadows/rounded Material cards, and scale to roughly five or more servers. The reference favors a two-line terminal-style item with a status mark, display name, and raw endpoint. Session count should remain secondary. A clear empty-selection behavior is required.