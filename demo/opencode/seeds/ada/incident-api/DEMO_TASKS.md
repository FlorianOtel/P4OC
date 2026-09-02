# Demo tasks

1. **Fix acknowledgement semantics.** `POST /v1/incidents/:id/acknowledge` currently allows a resolved incident to be acknowledged. Reproduce with `curl`, enforce the state transition in the store, and add store plus HTTP regression tests.
2. **Make service filtering case-insensitive.** `GET /v1/incidents?service=Billing-API` returns no rows because the filter compares raw strings. Normalize safely without changing stored IDs and test mixed-case input.
3. **Add incident timeline notes.** Accept a nonblank operator note on `POST /v1/incidents/:id/notes`, stamp it with the injected clock, return `201`, and expose notes in incident detail while keeping list responses compact.
4. **Repair pagination metadata.** The incident list reports `total` after applying `limit`, making clients think there is no next page. Write a failing test, fix the response, and manually compare two `curl` requests.
5. **Filter incidents by creation time.** Add a validated ISO-8601 `createdAfter` query parameter through the router and store, reject malformed timestamps with the standard error envelope, and add inclusive-boundary tests proving an incident that starts exactly at the supplied instant is retained.
6. **Add a severity query.** Support repeatable `severity` values such as `?severity=critical&severity=major`, reject unknown values with the standard error envelope, and document examples in the route's tests.
