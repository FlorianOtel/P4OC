---
name: repository-triage
description: Triage an unfamiliar local repository using a deterministic structural snapshot, targeted file reads, Git evidence, and LSP navigation before proposing work.
license: MIT
compatibility: opencode
metadata:
  audience: demo-users
  workflow: repository-analysis
---

## Workflow

1. Call `demo_triage_repository_snapshot` for the requested repository.
2. Read the `triage://repository/checklist` resource when a structured checklist is useful.
3. Identify manifests, entry points, tests, and the dominant file types from the snapshot.
4. Inspect only relevant non-secret files. Never read `.env`, credential, token, private-key, or secret files.
5. Use LSP navigation to trace at least one important symbol when source files are present.
6. Inspect Git status and diff without changing the working tree.
7. Separate observed facts from hypotheses.

## Result

Return a concise repository purpose, structure overview, current Git state, likely risks, and the smallest useful next action. Ask a focused question when the requested goal is ambiguous. Ask permission before any edit or state-changing command.
