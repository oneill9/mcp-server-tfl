---
name: tfl-status
description: Check the current operational status of London transport services.
---

Use the `service_status` MCP tool to check the current operational status and any disruptions for London Underground services.

## When to use this skill

- The user asks about Tube or London Underground status
- The user asks if a specific line is running normally
- The user asks about delays or disruptions on London Underground services

## How to use

1. Call the `service_status` tool with `modes: "tube"`
2. If the user asks about a specific Underground line, use `tube` mode and look for that line in the results
3. Present the status clearly, highlighting any disruptions or delays
4. If disruption reasons are provided, include them in your response
5. If the user asks for non-tube modes, explain that this connector currently exposes tube service status only

## Example queries

- "Is the Central line running normally?"
- "Are there any delays on the tube?"
- "What's the status of the Elizabeth line?"
