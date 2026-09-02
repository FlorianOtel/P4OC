import assert from "node:assert/strict";
import test from "node:test";
import { compareVersions, parseVersion } from "../lib/semver.js";

test("parseVersion exposes stable release components", () => {
  assert.deepEqual(parseVersion("2.1.0+build.7"), {
    raw: "2.1.0+build.7", major: 2, minor: 1, patch: 0, prerelease: null,
  });
});

test("stable releases sort after prereleases", () => {
  assert.equal(compareVersions("2.1.0", "2.1.0-rc.1"), 1);
});

test("numeric core fields compare numerically", () => {
  assert.equal(compareVersions("2.10.0", "2.9.9"), 1);
});
