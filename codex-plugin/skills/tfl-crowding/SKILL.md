---
name: tfl-crowding
description: Get live crowding data for a London transport station.
---

Use the `crowding` MCP tool to check how busy a TfL station is right now.

## When to use this skill

- The user asks how busy or crowded a station is
- The user wants to know if a station is quieter than usual
- The user is deciding whether to avoid a particular station

## How to use

1. Call the `crowding` tool with the `stopName` parameter (station name)
2. The tool returns the current crowding level as a percentage of the typical baseline
3. Values above 100% mean busier than usual; below 100% means quieter
4. If crowding data is unavailable for a station, inform the user

## Example queries

- "How busy is Oxford Circus right now?"
- "Is King's Cross crowded?"
- "What's the crowding level at Victoria?"
