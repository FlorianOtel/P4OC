# Demo tasks

1. **Fix search normalization.** Searching for `BILLING` currently hides Billing API. Make matching locale-safe and case-insensitive across service names, descriptions, and regions; add model tests for mixed-case input.
2. **Persist the region filter.** Store the selected region in the URL query string, restore it on reload, and ensure the Back button works. Exercise the behavior in the browser and keep fixture mode intact.
3. **Improve incident disclosure accessibility.** Turn incident cards into collapsible disclosures with accurate `aria-expanded`, keyboard operation, and a live announcement when details open. Add a DOM-level regression test.
4. **Add degraded-service sorting.** Operational services should follow degraded and outage services while names remain alphabetic within a group. Implement the ordering in the model rather than the renderer and test all statuses.
5. **Repair relative times.** `relativeTime` says “in 1 minute” for timestamps only a few seconds ahead and handles singular hours awkwardly. Define boundary cases, edit the formatter, inspect the diff, and run tests.
6. **Add an auto-refresh pause control.** Provide a visible toggle, abort in-flight fetches, and announce refresh failures without discarding the last good snapshot. Use shell search to find timer ownership before changing it.
7. **Theme follow-up.** Search for duplicated colors, introduce CSS custom properties for incident severity, and verify high-contrast focus states without changing the page's restrained visual language.
