# Demo Tasks

## 1. Correct semantic-version precedence
1. Add table-driven tests for prerelease identifiers, leading zeroes, and build metadata.
2. Reproduce the current incorrect ordering of `2.1.0-rc.10` and `2.1.0-rc.2`.
3. Implement SemVer 2.0 precedence without adding a package dependency.
4. Improve malformed-version diagnostics with the manifest field name.
5. Run the sample audit and all tests.

## 2. Make changelog parsing robust
1. Add fixtures with CRLF, setext titles, duplicate releases, and bracketed links.
2. Return line numbers for duplicate or malformed release headings.
3. Distinguish missing releases from headings hidden inside fenced code blocks.
4. Keep output deterministic and update tests for human and JSON modes.

## 3. Audit multiple packages
1. Extend config to accept a list of manifests with optional package labels.
2. Resolve each manifest relative to its config file.
3. Accumulate findings instead of stopping on the first invalid package.
4. Define stable JSON output and exit behavior for mixed parse and policy failures.
5. Add a realistic monorepo fixture and end-to-end CLI tests.

## 4. Add release-note policy rules
1. Require configurable sections such as Added, Fixed, and Security.
2. Permit explicitly configured exceptions for patch releases.
3. Report the heading and source line for every finding.
4. Add focused parser tests and a CLI test that snapshots normalized JSON values, not whitespace.
