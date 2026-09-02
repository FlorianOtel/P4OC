# Release Auditor Agent Guide

## Commands
- Run tests: `npm test` or `node --test`
- Run the sample audit: `npm run audit`
- Audit alternate inputs: `node bin/release-audit.js --config path/to/config.json --json`

## Constraints
- The utility deliberately has no runtime dependencies; prefer Node built-ins.
- Keep stdout machine-readable with `--json`; diagnostics belong on stderr.
- Resolve paths relative to the config file, not the current working directory.
- Preserve exit codes: 0 clean, 1 policy findings, 2 invocation or input failure.
- Add fixtures for parser edge cases and test public behavior rather than implementation text.
