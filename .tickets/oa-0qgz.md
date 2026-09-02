---
id: oa-0qgz
status: closed
deps: [oa-s5jj]
links: []
created: 2026-05-05T18:19:53Z
type: task
priority: 2
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, ofish, benchmark]
---
# Empirical OFISH chunk-size benchmark + runtime probe

JVM benchmark script that drives OpenCodeApi.executeShellCommand against a running opencode serve to find the optimal chunk size empirically. No hard upper cap.

Run target: PENCODE_SERVER_PASSWORD=hunter2 opencode serve --hostname 0.0.0.0 --port 4096 (user: opencode).

Bench script (app/src/test/java/dev/blazelight/p4oc/bench/ChunkSizeBenchmark.kt):
1. Create ephemeral session.
2. Probe sizes geometrically: 64 KiB, 256 KiB, 1 MiB, 4 MiB, 16 MiB, 64 MiB, ... DOUBLE until first failure (no upper cap in algorithm).
3. For each size: emit base64 payload via heredoc-on-stdin OFISH command, sha256 verify, measure latency.
4. Pick highest-throughput SUCCESSFUL size as the constant.
5. Delete session, clean temp file.

Output: const val OFISH_DEFAULT_CHUNK_BYTES: Int = <bench result>.

Runtime probe at workspace connect (in OfishSessionProvider equivalent):
- Try one probe write at OFISH_DEFAULT_CHUNK_BYTES.
- If success: use constant.
- If fail: halve until success or 64 KiB minimum.
- NEVER probe upward — only the manual benchmark grows the constant.

Re-run benchmark when server, proxy, OkHttp, or deployment config changes.

## Acceptance Criteria

Benchmark script runs against the dev server in less than 60s. Outputs a chunk size with measured throughput. Constant committed to source. Runtime probe halves correctly on simulated server-rejection. No hard upper cap anywhere in the chunking code path.

