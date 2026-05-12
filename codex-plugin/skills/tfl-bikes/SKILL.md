---
name: tfl-bikes
description: Find Santander Cycles bike docking stations with live availability.
---

Use the `bike_points` MCP tool to find Santander Cycles docking stations and check bike/dock availability.

## When to use this skill

- The user asks about bike availability near a location
- The user wants to find a Santander Cycles docking station
- The user needs to know if there are empty docks to return a bike

## How to use

1. Call the `bike_points` tool with an optional `query` parameter to filter by name/location
2. Without a query, all bike points are returned (there are hundreds — use a query when possible)
3. Results show the station name, number of available bikes, and number of empty docks
4. Help the user identify the nearest or most convenient station

## Example queries

- "Are there any bikes available near Clerkenwell?"
- "Find Santander Cycles docking stations near Waterloo"
- "Where can I pick up a Boris bike near King's Cross?"
