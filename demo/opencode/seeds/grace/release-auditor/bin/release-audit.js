#!/usr/bin/env node
import { auditRelease, loadAudit } from "../lib/audit.js";

async function main(argv) {
  const options = parseArgs(argv);
  const input = await loadAudit(options.config);
  const report = auditRelease(input);
  if (options.json) {
    console.log(JSON.stringify(report, null, 2));
  } else {
    console.log(`${report.package ?? "<unnamed>"} ${report.version ?? "<unversioned>"}`);
    if (report.findings.length === 0) console.log("release audit passed");
    for (const finding of report.findings) console.log(`- ${finding.code}: ${finding.message}`);
  }
  return report.findings.length === 0 ? 0 : 1;
}

function parseArgs(argv) {
  let config;
  let json = false;
  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index];
    if (argument === "--json") json = true;
    else if (argument === "--config") config = argv[++index];
    else throw new Error(`unknown argument ${argument}`);
  }
  if (!config) throw new Error("usage: release-audit --config <file> [--json]");
  return { config, json };
}

main(process.argv.slice(2))
  .then((code) => { process.exitCode = code; })
  .catch((error) => {
    console.error(`release-audit: ${error.message}`);
    process.exitCode = 2;
  });
