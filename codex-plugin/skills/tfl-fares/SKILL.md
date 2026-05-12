---
name: tfl-fares
description: Get fare information between two London transport stops.
---

Use the `fares` MCP tool to look up fare information between two TfL stops.

## When to use this skill

- The user asks how much a tube/train journey costs
- The user wants to compare peak vs off-peak fares
- The user asks about pay-as-you-go vs cash prices

## How to use

1. Call the `fares` tool with `fromName` and `toName` parameters (stop names, not IDs)
2. The tool automatically resolves stop names to NaPTAN IDs
3. Results include pay-as-you-go and cash single prices for peak and off-peak travel
4. Present fares clearly, distinguishing between peak and off-peak pricing

## Example queries

- "How much is the tube from Oxford Circus to Bond Street?"
- "What's the fare from Bank to Canary Wharf?"
- "How much does it cost to go from Paddington to Heathrow on the Elizabeth line?"
