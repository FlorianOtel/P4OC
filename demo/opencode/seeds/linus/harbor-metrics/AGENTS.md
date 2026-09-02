# Harbor Metrics contributor guide

## Scope

Harbor Metrics reads shipment CSV files and produces deterministic operational summaries. `harbor_metrics/records.py` owns parsing and validation; `harbor_metrics/report.py` owns calculations; `harbor_metrics/cli.py` owns user-facing I/O.

## Commands

- Summarize fixture: `python -m harbor_metrics summarize data/shipments.csv`
- Filter delayed rows: `python -m harbor_metrics delayed data/shipments.csv --min-hours 4`
- Test: `python -m unittest discover -s tests -v`

## Conventions

- Keep monetary values as `Decimal`, never binary floats.
- Keep report ordering deterministic so output is diff-friendly.
- Raise `DataError` with the source row number for malformed CSV input.
- CLI failures go to stderr and return a non-zero status; library code must not print.
