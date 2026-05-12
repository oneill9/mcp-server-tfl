---
name: tfl-journey
description: Plan a journey between two points in London using the TfL Journey Planner.
---

Use the `journey` MCP tool to plan routes between two points in London.

## When to use this skill

- The user asks how to get from one place to another in London
- The user needs directions involving public transport
- The user asks about travel time between locations

## How to use

1. Call the `journey` tool with `from` and `to` parameters
2. Accepted formats: postcodes (e.g. `SW1A 1AA`), lat/lon pairs (e.g. `51.5074,-0.1278`), or NaPTAN IDs
3. The tool returns multiple journey options with step-by-step legs and durations
4. Present the quickest or most relevant option first

## Example queries

- "How do I get from King's Cross to Canary Wharf?"
- "Plan a journey from Waterloo to Heathrow"
- "What's the fastest route from SE1 to EC2?"
