# Dispatch Lab Agent Guide

## Scope
This repository is a small concurrent job dispatcher. Keep changes within this repository.

## Commands
- Run all tests: `go test ./...`
- Exercise the CLI: `go run ./cmd/dispatch -config fixtures/config.json -jobs fixtures/jobs.jsonl`
- Race-check concurrency changes: `go test -race ./...`

## Conventions
- Preserve input-order-independent processing; output is intentionally completion ordered.
- Return errors with job IDs and preserve wrapped causes.
- Do not add third-party dependencies for JSON, logging, or synchronization.
- Add deterministic tests for cancellation and concurrent behavior; never use arbitrary sleeps as the only assertion.
