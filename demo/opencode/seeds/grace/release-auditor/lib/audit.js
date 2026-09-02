import { readFile } from "node:fs/promises";
import path from "node:path";
import { parseVersion } from "./semver.js";

export async function loadAudit(configPath) {
  const absoluteConfig = path.resolve(configPath);
  const config = await readJson(absoluteConfig, "config");
  const base = path.dirname(absoluteConfig);
  const manifestPath = path.resolve(base, requiredString(config.manifest, "manifest"));
  const changelogPath = path.resolve(base, requiredString(config.changelog, "changelog"));
  const [manifest, changelog] = await Promise.all([
    readJson(manifestPath, "manifest"),
    readFile(changelogPath, "utf8").catch((error) => {
      throw new Error(`read changelog ${changelogPath}: ${error.message}`, { cause: error });
    }),
  ]);
  return { config, manifest, changelog, files: { config: absoluteConfig, manifest: manifestPath, changelog: changelogPath } };
}

export function auditRelease({ config, manifest, changelog }) {
  const findings = [];
  let version;
  try {
    version = parseVersion(requiredString(manifest.version, "manifest.version"));
  } catch (error) {
    findings.push({ code: "INVALID_VERSION", message: error.message });
  }

  if (!manifest.name) findings.push({ code: "MISSING_NAME", message: "manifest.name is required" });
  if (config.forbidPrivateRelease && manifest.private) {
    findings.push({ code: "PRIVATE_RELEASE", message: "release manifest must not be private" });
  }
  for (const script of config.requiredScripts ?? []) {
    if (typeof manifest.scripts?.[script] !== "string" || manifest.scripts[script].trim() === "") {
      findings.push({ code: "MISSING_SCRIPT", message: `required script ${script} is missing` });
    }
  }
  const nodeMajor = parseMinimumNodeMajor(manifest.engines?.node);
  if (nodeMajor === null || nodeMajor < config.minimumNodeMajor) {
    findings.push({ code: "NODE_ENGINE", message: `Node engine must require at least ${config.minimumNodeMajor}` });
  }

  const releases = parseChangelog(changelog);
  if (version && !releases.some((release) => release.version === version.raw)) {
    findings.push({ code: "CHANGELOG_VERSION", message: `changelog has no release heading for ${version.raw}` });
  }
  return { package: manifest.name ?? null, version: manifest.version ?? null, findings, releases };
}

export function parseChangelog(markdown) {
  const releases = [];
  const heading = /^## \[([^\]]+)\](?: - (\d{4}-\d{2}-\d{2}))?$/;
  for (const [index, line] of markdown.split("\n").entries()) {
    const match = heading.exec(line);
    if (match) releases.push({ version: match[1], date: match[2] ?? null, line: index + 1 });
  }
  return releases;
}

function parseMinimumNodeMajor(range) {
  if (typeof range !== "string") return null;
  const match = /^(?:>=|\^|~)?\s*(\d+)/.exec(range);
  return match ? Number(match[1]) : null;
}

function requiredString(value, field) {
  if (typeof value !== "string" || value.trim() === "") throw new Error(`${field} must be a non-empty string`);
  return value;
}

async function readJson(file, label) {
  let source;
  try {
    source = await readFile(file, "utf8");
  } catch (error) {
    throw new Error(`read ${label} ${file}: ${error.message}`, { cause: error });
  }
  try {
    return JSON.parse(source);
  } catch (error) {
    throw new Error(`parse ${label} ${file}: ${error.message}`, { cause: error });
  }
}
