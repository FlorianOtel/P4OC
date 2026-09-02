import assert from "node:assert/strict";
import test from "node:test";
import { auditRelease, loadAudit, parseChangelog } from "../lib/audit.js";

test("sample fixture passes the release policy", async () => {
  const input = await loadAudit(new URL("../fixtures/audit-config.json", import.meta.url).pathname);
  const report = auditRelease(input);
  assert.deepEqual(report.findings, []);
  assert.equal(report.package, "orbit-console");
  assert.equal(report.releases[0].line, 3);
});

test("audit accumulates independent policy findings", () => {
  const report = auditRelease({
    config: { minimumNodeMajor: 22, requiredScripts: ["test"], forbidPrivateRelease: true },
    manifest: { name: "old-tool", version: "1.4.0", private: true, engines: { node: ">=18" }, scripts: {} },
    changelog: "# Changelog\n",
  });
  assert.deepEqual(report.findings.map(({ code }) => code), [
    "PRIVATE_RELEASE", "MISSING_SCRIPT", "NODE_ENGINE", "CHANGELOG_VERSION",
  ]);
});

test("changelog parser records release dates and lines", () => {
  assert.deepEqual(parseChangelog("# Log\n\n## [1.2.0] - 2026-08-01\ntext\n## [1.1.0]"), [
    { version: "1.2.0", date: "2026-08-01", line: 3 },
    { version: "1.1.0", date: null, line: 5 },
  ]);
});
