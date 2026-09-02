import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import test from "node:test";

const cli = new URL("../bin/release-audit.js", import.meta.url);
const config = new URL("../fixtures/audit-config.json", import.meta.url);

test("CLI emits a machine-readable clean report", () => {
  const run = spawnSync(process.execPath, [cli.pathname, "--config", config.pathname, "--json"], { encoding: "utf8" });
  assert.equal(run.status, 0, run.stderr);
  const report = JSON.parse(run.stdout);
  assert.equal(report.package, "orbit-console");
  assert.deepEqual(report.findings, []);
});

test("CLI reserves exit code 2 for invocation errors", () => {
  const run = spawnSync(process.execPath, [cli.pathname, "--wat"], { encoding: "utf8" });
  assert.equal(run.status, 2);
  assert.match(run.stderr, /unknown argument --wat/);
});
