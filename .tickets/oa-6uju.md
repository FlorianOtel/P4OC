---
id: oa-6uju
status: closed
deps: []
links: []
created: 2026-07-09T00:00:00Z
type: task
priority: 1
assignee: Jasmin Le Roux
---
# Fix release visual QA guide evidence honesty

## Problem

The release visual QA guide at `local-adb-screenshots/release-v013-to-current/index.html` contradicts itself: several scenarios show red `Missing ...` placeholders while their Result text says `captured before/after`. Reviewers cannot trust the guide if the visible evidence status conflicts with the summary.

Concrete known contradictions:

- `Draft persistence across tab switch`: missing `after/055_current_chat_draft_persistence.png` but says captured before/after.
- `Chat scroll restoration`: missing `after/056_current_chat_scroll_position.png` but says captured before/after.
- `Rotation and ViewModel recreation`: missing `after/057_current_chat_rotation_landscape.png` but says captured before/after.
- `Portrait state after rotation`: missing `after/058_current_chat_rotation_portrait.png` but says captured before/after.
- `Upgrade tab survival`: missing `before/023_old_pre_upgrade_visible_tab_bar.png`; likely stale filename because `before/23_old_pre_upgrade_visible_tab_bar.png` exists.
- `Provider/model settings`: missing `before/030_provider_model_settings_old.png` but says captured before/after.

## Known consistency note

`oa-5sro` and `oa-xju6` were closed before this guide audit, and their closeout record may overstate the reliability of the release visual QA guide. Do not silently rewrite that history or reopen those tickets unless explicitly requested. Instead, this ticket is the follow-up record that reconciles the overstated evidence claims and links the correction back to the closed QA/architecture work.

## UX Constraint

The report must be an honest QA artifact, not commentary that explains away missing screenshots. If a screenshot does not exist or does not prove the scenario, say so plainly.

## Expected Behavior

Each scenario reports its actual evidence state:

- before image present/missing
- after image present/missing
- behavior visually proven / partially proven / not visually proven
- test/API evidence if visual proof is intentionally unavailable
- blocked reason when evidence is missing

## Acceptance Criteria

- No scenario says `captured before/after` when either displayed image is missing.
- Missing image placeholders are either eliminated by recapturing/renaming screenshots or paired with `partial`, `before-only`, `after-only`, or `missing current evidence` result text.
- Stale filename references are fixed, including `023_old_pre_upgrade_visible_tab_bar.png` if the actual file is `23_old_pre_upgrade_visible_tab_bar.png`.
- The guide distinguishes visual proof from reachability/menu screenshots and test/API evidence.
- Screenshot-count-gate language is removed or moved out of the user-facing report.

## Verification

- Search the generated guide for `Missing` and verify nearby Result text does not overclaim.
- Verify every referenced image exists or is intentionally labeled missing/partial.
- Open the guide in Firefox and visually inspect the corrected scenarios.

## Verification Notes

- 2026-07-09: Corrected `local-adb-screenshots/release-v013-to-current/index.html` so `Draft persistence across tab switch`, `Chat scroll restoration`, `Rotation and ViewModel recreation`, and `Portrait state after rotation` now report partial/missing current evidence instead of `captured before/after`.
- 2026-07-09: Fixed stale `Upgrade tab survival` before-image reference from `before/023_old_pre_upgrade_visible_tab_bar.png` to existing `before/23_old_pre_upgrade_visible_tab_bar.png`.
- 2026-07-09: Corrected `Provider/model settings` to partial because the before screenshot is missing.
- 2026-07-09: Removed screenshot-count-gate wording from the guide gallery copy.
- 2026-07-09: Verified with regex search that no `Missing after/055` through `Missing after/058` scenario still says `captured before/after`.
