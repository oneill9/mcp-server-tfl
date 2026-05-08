import { cpSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const nodeDir = resolve(scriptDir, "..");
const repoRoot = resolve(nodeDir, "..");
const sourceDir = resolve(repoRoot, "shared", "resources");
const outputDir = resolve(nodeDir, "dist", "resources");

mkdirSync(outputDir, { recursive: true });
cpSync(sourceDir, outputDir, { recursive: true });
