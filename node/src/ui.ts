import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export interface LineStatusData {
  id: string;
  name: string;
  statuses: Array<{
    severity: string;
    reason?: string;
  }>;
}

export function renderServiceStatusHtml(_lines?: LineStatusData[]): string {
  const here = dirname(fileURLToPath(import.meta.url));
  return readFileSync(resolve(here, "resources", "service-status.html"), "utf8");
}
