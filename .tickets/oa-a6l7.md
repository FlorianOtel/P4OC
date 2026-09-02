---
id: oa-a6l7
status: closed
deps: []
links: [oa-1n6h, oa-t4t2, oa-764s, oa-p7ei, oa-es45]
created: 2026-04-19T13:57:42Z
type: feature
priority: 2
assignee: Jasmin Le Roux
external-ref: pr-3-cherrypick
tags: [discovery, networking, mdns]
---
# Seeded mDNS discovery from recent + typed URLs (Tailscale/VPN UX)

mDNS can't reach servers over Tailscale/VPN (no multicast). The PR #3 sweep approach is too aggressive (512 targets × 32 concurrent probes on every network). Instead: seed discovery from URLs the user has already shown interest in.

## Design

Add MdnsDiscoveryManager.startDiscovery(seeds: List<String>) variant:
- Keep existing mDNS/NSD scanning for Wi-Fi _http._tcp _opencode-*_ services
- For each seed URL: resolve hostname, health-probe the resolved address, add to discovered list on 200 response
- Bounded concurrency (4 at a time), short timeout (2s per probe)

ServerViewModel.startDiscovery passes recentServers.map { it.url } + currentTypedUrl as seeds.

## Explicitly NOT doing

- Subnet sweep (512 targets, CIDR enumeration, reverse DNS) — too aggressive, battery/privacy concern on corporate networks
- Accepting 401/403/404 + header sniffing as positive hits — false-positive soup
- Multicast lock acquisition (not needed without sweep)
- ACCESS_WIFI_STATE / CHANGE_WIFI_MULTICAST_STATE permissions

## Verify existing lifecycle

Main currently uses DisposableEffect start/stop in ServerScreen — verify this is not regressed. PR #3 broke it (LaunchedEffect without stop on dispose).

## Test plan

- Tailscale server on 100.x.y.z visited once, added to recent
- Next discovery run: 100.x server should appear in discovered list
- Local mDNS server should still appear via NSD path
- Nothing else should appear (no sweep, no false positives)

## Acceptance Criteria

1. Recent servers show up in discovered list when reachable
2. mDNS still works on local Wi-Fi
3. No new permissions needed
4. Discovery stops when leaving server screen
5. No regression on PR #1 self-signed TLS flow

