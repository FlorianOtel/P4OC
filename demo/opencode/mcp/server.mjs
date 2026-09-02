import { execFile } from "node:child_process";
import { readdir, realpath, stat } from "node:fs/promises";
import { homedir } from "node:os";
import path from "node:path";
import { promisify } from "node:util";

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const execFileAsync = promisify(execFile);
const workspaceRoot = await realpath(
  path.join(process.env.HOME ?? homedir(), "workspaces"),
);
const skippedDirectories = new Set([
  ".git",
  ".gradle",
  ".idea",
  ".next",
  ".opencode",
  "build",
  "coverage",
  "dist",
  "node_modules",
  "target",
  "vendor",
]);
const manifestNames = new Set([
  "Cargo.toml",
  "build.gradle",
  "build.gradle.kts",
  "go.mod",
  "package.json",
  "pom.xml",
  "pyproject.toml",
  "requirements.txt",
  "settings.gradle",
  "settings.gradle.kts",
]);
const sensitiveNamePattern = /(^|[._-])(\.env|credentials?|secrets?|tokens?|private[-_]?key)([._-]|$)/i;
const maxFiles = 2_000;

async function resolveRepository(repository) {
  const requested = path.resolve(workspaceRoot, repository || ".");
  let candidate;
  try {
    candidate = await realpath(requested);
  } catch (error) {
    if (error && typeof error === "object" && "code" in error && error.code === "ENOENT") {
      throw new Error(`repository does not exist: ${repository || "."}`);
    }
    throw error;
  }
  const relative = path.relative(workspaceRoot, candidate);
  if (relative.startsWith("..") || path.isAbsolute(relative)) {
    throw new Error("repository must stay inside the OpenCode workspace root");
  }
  return { candidate, relative: relative || "." };
}

function extensionLabel(fileName) {
  const extension = path.extname(fileName).toLowerCase();
  return extension || "[no extension]";
}

async function collectSnapshot(root, maxDepth) {
  const topLevel = [];
  const manifests = [];
  const extensionCounts = new Map();
  let directoryCount = 0;
  let fileCount = 0;
  let skippedSensitiveFiles = 0;
  let truncated = false;

  async function visit(directory, depth) {
    const entries = await readdir(directory, { withFileTypes: true });
    entries.sort((left, right) => left.name.localeCompare(right.name, "en"));

    for (const entry of entries) {
      if (fileCount >= maxFiles) {
        truncated = true;
        return;
      }

      const absolute = path.join(directory, entry.name);
      const relative = path.relative(root, absolute);
      if (depth === 0) {
        topLevel.push(entry.isDirectory() ? `${entry.name}/` : entry.name);
      }

      if (entry.isSymbolicLink()) {
        continue;
      }
      if (entry.isDirectory()) {
        if (skippedDirectories.has(entry.name)) {
          continue;
        }
        directoryCount += 1;
        if (depth < maxDepth) {
          await visit(absolute, depth + 1);
        }
        continue;
      }
      if (!entry.isFile()) {
        continue;
      }
      if (sensitiveNamePattern.test(entry.name)) {
        skippedSensitiveFiles += 1;
        continue;
      }

      fileCount += 1;
      const extension = extensionLabel(entry.name);
      extensionCounts.set(extension, (extensionCounts.get(extension) || 0) + 1);
      if (manifestNames.has(entry.name)) {
        manifests.push(relative);
      }
    }
  }

  await visit(root, 0);
  return {
    topLevel,
    manifests: manifests.sort((left, right) => left.localeCompare(right, "en")),
    fileCount,
    directoryCount,
    skippedSensitiveFiles,
    truncated,
    filesByExtension: Object.fromEntries(
      [...extensionCounts.entries()].sort(([left], [right]) => left.localeCompare(right, "en")),
    ),
  };
}

async function gitStatus(root) {
  try {
    const { stdout } = await execFileAsync(
      "git",
      ["-C", root, "status", "--short", "--branch", "--untracked-files=all"],
      { encoding: "utf8", timeout: 2_000, maxBuffer: 256 * 1024 },
    );
    return stdout.trimEnd().split("\n").filter(Boolean);
  } catch (error) {
    if (error && typeof error === "object" && "stderr" in error) {
      const stderr = String(error.stderr).trim();
      if (stderr.includes("not a git repository")) {
        return ["[not a git repository]"];
      }
    }
    throw new Error(`unable to inspect git status: ${error instanceof Error ? error.message : String(error)}`);
  }
}

const server = new McpServer({
  name: "opencode-demo-triage",
  version: "1.0.0",
});

server.registerResource(
  "repository-triage-checklist",
  "triage://repository/checklist",
  {
    title: "Repository triage checklist",
    description: "A short, repeatable checklist for understanding an unfamiliar demo repository.",
    mimeType: "text/markdown",
  },
  async (uri) => ({
    contents: [
      {
        uri: uri.href,
        mimeType: "text/markdown",
        text: [
          "# Repository triage checklist",
          "",
          "1. Capture the repository snapshot and current Git status.",
          "2. Identify manifests, entry points, tests, and the dominant file types.",
          "3. Use LSP and file reads to trace one important symbol before proposing changes.",
          "4. Separate observed facts from hypotheses and list the smallest next action.",
          "5. Never inspect `.env`, credentials, tokens, private keys, or other secret material.",
        ].join("\n"),
      },
    ],
  }),
);

server.registerPrompt(
  "triage-repository",
  {
    title: "Triage a demo repository",
    description: "Build an evidence-based overview of one repository before editing it.",
    argsSchema: {
      repository: z.string().trim().min(1).max(128).describe("Repository directory under the workspace root"),
      focus: z.string().trim().max(240).optional().describe("Optional subsystem or concern to prioritize"),
    },
  },
  ({ repository, focus }) => ({
    messages: [
      {
        role: "user",
        content: {
          type: "text",
          text: [
            `Triage the repository ${repository}.`,
            focus ? `Prioritize this focus: ${focus}.` : "Cover its purpose, structure, and current working-tree state.",
            "First call the demo_triage_repository_snapshot tool, then inspect only relevant non-secret files and use LSP where helpful.",
            "Report observed facts, risks, and one concrete next action. Do not modify files.",
          ].join(" "),
        },
      },
    ],
  }),
);

server.registerTool(
  "repository_snapshot",
  {
    title: "Repository snapshot",
    description: "Summarize a local demo repository's structure, manifests, file types, and Git status without reading file contents.",
    inputSchema: {
      repository: z.string().trim().max(128).optional().describe("Directory under the workspace root; defaults to the root"),
      maxDepth: z.number().int().min(1).max(6).default(3).describe("Maximum directory depth to inspect"),
    },
  },
  async ({ repository, maxDepth }) => {
    const { candidate, relative } = await resolveRepository(repository);
    const candidateStat = await stat(candidate);
    if (!candidateStat.isDirectory()) {
      throw new Error("repository must name a directory");
    }

    const snapshot = await collectSnapshot(candidate, maxDepth);
    const output = {
      repository: relative,
      maxDepth,
      ...snapshot,
      gitStatus: await gitStatus(candidate),
    };
    return {
      content: [{ type: "text", text: JSON.stringify(output, null, 2) }],
      structuredContent: output,
    };
  },
);

const transport = new StdioServerTransport();
await server.connect(transport);
