/**
 * Self-contained HTML UI for TfL service status.
 *
 * Design: London transport status-board inspired, using standard line colors
 * and data conventions. Does NOT use TfL roundel, official logos, Johnston font,
 * or official-looking page chrome.
 *
 * The generated HTML is fully self-contained: no external scripts, styles, or images.
 */

/** Well-known London transport line colours (hex). */
const LINE_COLOURS: Record<string, string> = {
  bakerloo: "#B36305",
  central: "#E32017",
  circle: "#FFD300",
  district: "#00782A",
  "hammersmith-city": "#F3A9BB",
  jubilee: "#A0A5A9",
  metropolitan: "#9B0056",
  northern: "#000000",
  piccadilly: "#003688",
  victoria: "#0098D4",
  "waterloo-city": "#95CDBA",
  "elizabeth-line": "#6950A1",
  elizabeth: "#6950A1",
  dlr: "#00A4A7",
  overground: "#EE7C0E",
  "london-overground": "#EE7C0E",
  tram: "#84B817",
  "liberty": "#6E6E6E",
  "lioness": "#E4A42C",
  "mildmay": "#007EC0",
  "suffragette": "#50AF47",
  "weaver": "#CE1489",
  "windrush": "#EF3E36",
};

/** Returns a hex colour for a given line id, falling back to a neutral grey. */
function lineColour(lineId: string): string {
  return LINE_COLOURS[lineId.toLowerCase()] ?? "#6E6E6E";
}

/** Determines a suitable text colour (white or dark) for a given background hex. */
function textColour(bgHex: string): string {
  const hex = bgHex.replace("#", "");
  const r = parseInt(hex.slice(0, 2), 16);
  const g = parseInt(hex.slice(2, 4), 16);
  const b = parseInt(hex.slice(4, 6), 16);
  // Relative luminance approximation
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.55 ? "#1a1a2e" : "#ffffff";
}

export interface LineStatusData {
  id: string;
  name: string;
  statuses: Array<{
    severity: string;
    reason?: string;
  }>;
}

/**
 * Generate self-contained HTML for the service status UI.
 *
 * @param lines - Optional structured data to embed. If omitted, the UI
 *   renders a placeholder awaiting data from the host.
 */
export function renderServiceStatusHtml(lines?: LineStatusData[]): string {
  const lineRows = (lines ?? [])
    .map((line) => {
      const bg = lineColour(line.id);
      const fg = textColour(bg);
      const isGood = line.statuses.every(
        (s) => s.severity.toLowerCase() === "good service"
      );
      const statusBadge = isGood
        ? `<span class="badge good">Good Service</span>`
        : line.statuses
            .map((s) => {
              const reason = s.reason
                ? `<span class="reason">${escapeHtml(s.reason)}</span>`
                : "";
              return `<span class="badge disrupted">${escapeHtml(s.severity)}</span>${reason}`;
            })
            .join("");

      return `
      <div class="line-row" style="--line-bg:${bg};--line-fg:${fg}">
        <div class="line-name">${escapeHtml(line.name)}</div>
        <div class="line-status">${statusBadge}</div>
      </div>`;
    })
    .join("\n");

  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>TfL Service Status</title>
<style>
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
:root{
  --bg:#0f0f1a;
  --surface:#1a1a2e;
  --text:#e0e0e0;
  --text-muted:#8888aa;
  --good:#00c853;
  --disrupted:#ff6d00;
  --radius:10px;
  --font:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;
}
body{
  font-family:var(--font);
  background:var(--bg);
  color:var(--text);
  padding:16px;
  min-height:100vh;
}
.board{
  max-width:540px;
  margin:0 auto;
}
h1{
  font-size:1.15rem;
  font-weight:600;
  letter-spacing:.02em;
  color:var(--text-muted);
  text-transform:uppercase;
  margin-bottom:12px;
}
.line-row{
  display:flex;
  align-items:stretch;
  background:var(--surface);
  border-radius:var(--radius);
  margin-bottom:8px;
  overflow:hidden;
  transition:transform .15s ease;
}
.line-row:hover{transform:scale(1.01)}
.line-name{
  min-width:120px;
  padding:12px 14px;
  font-weight:700;
  font-size:.88rem;
  background:var(--line-bg);
  color:var(--line-fg);
  display:flex;
  align-items:center;
}
.line-status{
  flex:1;
  padding:10px 14px;
  display:flex;
  flex-wrap:wrap;
  align-items:center;
  gap:6px;
}
.badge{
  display:inline-block;
  font-size:.78rem;
  font-weight:600;
  padding:3px 10px;
  border-radius:20px;
}
.badge.good{
  background:rgba(0,200,83,.15);
  color:var(--good);
}
.badge.disrupted{
  background:rgba(255,109,0,.15);
  color:var(--disrupted);
}
.reason{
  display:block;
  font-size:.72rem;
  color:var(--text-muted);
  margin-top:2px;
  line-height:1.35;
}
.empty{
  text-align:center;
  color:var(--text-muted);
  padding:32px 16px;
  font-size:.9rem;
}
</style>
</head>
<body data-app="service-status">
<div class="board">
<h1>Service Status</h1>
${lineRows || '<div class="empty">Awaiting service status data…</div>'}
</div>
</body>
</html>`;
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
