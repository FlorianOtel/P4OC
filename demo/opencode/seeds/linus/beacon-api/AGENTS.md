# Beacon API contributor guide

## Scope

This repository is a dependency-free Python HTTP service backed by an in-memory incident store. Keep HTTP parsing and response formatting in `beacon_api/app.py`; keep filtering and aggregation in `beacon_api/store.py`.

## Commands

- Run: `python -m beacon_api --port 8080`
- Test: `python -m unittest discover -s tests -v`
- Try: `curl http://127.0.0.1:8080/api/v1/incidents`

## Conventions

- Use only the Python standard library unless a dependency is justified in `pyproject.toml`.
- Preserve the JSON error shape: `{"error": {"code": ..., "message": ...}}`.
- Add behavioral tests for routes and unit tests for store rules.
- Never mutate fixture dictionaries returned to callers.
