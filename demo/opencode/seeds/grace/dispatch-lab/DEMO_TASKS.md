# Demo Tasks

## 1. Make cancellation reliable
1. Reproduce cancellation behavior with a test that cancels while workers are active.
2. Determine why the configured `jobTimeout` is validated but never enforced.
3. Identify which sends or waits can outlive the caller.
4. Change the dispatcher so timeout and cancellation return promptly without leaking goroutines.
5. Preserve completed results and the relevant cancellation cause.
6. Run `go test -race ./...` and exercise the CLI.

## 2. Add retry policy
1. Extend the JSON config with bounded attempts and backoff duration.
2. Retry only transient `flaky` jobs, never malformed or unknown jobs.
3. Report attempts in each result while retaining useful wrapped errors.
4. Add deterministic tests using an injectable delay rather than slow sleeps.
5. Update the fixture config and compare CLI output before and after.

## 3. Validate workload limits
1. Reject duplicate IDs, non-positive durations, and payloads over a chosen limit.
2. Report the JSONL line number together with validation failures.
3. Decide whether blank and comment lines are accepted, then test that contract.
4. Ensure one bad line cannot silently discard earlier valid jobs.

## 4. Add per-kind metrics
1. Aggregate success, failure, and elapsed time by job kind without a data race.
2. Print a stable JSON summary from the CLI.
3. Test metrics independently from output completion order.
4. Confirm `go test -race ./...` remains clean under a large fixture generated in `/tmp`.
