---
name: tfl-arrivals
description: Get live arrival predictions at any London transport stop.
---

Use the `arrivals` MCP tool to get real-time arrival predictions at TfL stops.

## When to use this skill

- The user asks when the next train/bus/tube is arriving
- The user asks about departure times from a specific stop or station
- The user wants live arrival information

## How to use

1. Call the `arrivals` tool with the stop name (e.g. `oxford circus`, `bank`, `victoria`)
2. The tool automatically resolves common stop names to NaPTAN IDs
3. Results are sorted by arrival time, showing line name, destination, and minutes until arrival
4. Present the nearest arrivals first

## Example queries

- "When is the next tube from Oxford Circus?"
- "What buses are coming to Bank?"
- "Next arrivals at Victoria station"
