---
id: oa-de13
status: closed
deps: []
links: []
created: 2026-04-19T13:01:10Z
type: feature
priority: 2
assignee: Jasmin Le Roux
external-ref: gh-1
---
# Support self-signed certificates via allowInsecure toggle

Users behind reverse proxies with self-signed certs hit 'Trust anchor for certification path not found'. Added per-server 'Allow self-signed certificate' checkbox that installs a permissive TrustManager + HostnameVerifier on the OkHttp client, persisted in ServerConfig and RecentServer.

