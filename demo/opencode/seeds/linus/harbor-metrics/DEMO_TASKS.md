# Demo tasks

1. **Fix promised-date semantics**: same-day arrivals are incorrectly counted as late when they have a positive transit delay. Define lateness from `promised_date`, update calculations, fixtures, and tests.
2. **Carrier comparison**: add a `carriers` subcommand reporting shipment count, late rate, and revenue by carrier in descending late-rate order.
3. **CSV diagnostics**: collect all malformed rows instead of stopping at the first one, while keeping row numbers and a useful CLI error format.
4. **Date-window support**: add inclusive `--from` and `--through` filters to both commands and test invalid and empty ranges.
5. **JSON export**: add `--format json` to `delayed`, preserving exact decimal values as strings and deterministic field order.
