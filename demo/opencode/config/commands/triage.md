---
description: Triage a demo repository with the local MCP snapshot and reusable workflow
agent: repo-triage
---

Load the `repository-triage` skill and triage repository `$1` using `demo_triage_repository_snapshot`. Treat the remaining arguments as the focus: `$ARGUMENTS`.

Do not edit files. Return observed purpose, structure, manifests, dominant file types, current Git state, risks, and one concrete next action.
