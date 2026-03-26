# Transport for London (TfL) MCP Server

> **Disclaimer:** This is a **community-built** project. It is **not affiliated with, endorsed by, or connected to Transport for London (TfL)** in any way.

Welcome to the **TfL MCP Server** documentation.

This is a community-built MCP (Model Context Protocol) server that exposes the [TfL (Transport for London) Unified API](https://api.tfl.gov.uk/) as tools, allowing AI assistants like Claude to query live London transport data.

## What is MCP?
The Model Context Protocol (MCP) allows AI models to securely interact with local or remote APIs, databases, and services to ground their responses in real, up-to-date data.

## Features

### Line & Service Status

- Check real-time status of the Tube and Overground network
- View disruptions and delays across all transport modes — Tube, bus, DLR, Elizabeth line, tram, river bus, and more
- List all available TfL transport modes

### Live Arrivals

- Get live arrival predictions at any bus stop, Tube station, or DLR platform
- Search for stops and stations by name to find their IDs

### Journey Planning

- Plan multi-leg journeys across London combining Tube, bus, rail, walking, and cycling
- Accepts postcodes, station IDs, or latitude/longitude coordinates

### Cycling

- Check live Santander Cycles docking station availability across London
- See bikes available and empty docks at each station

### Roads & Environment

- View current road disruptions, closures, and roadworks on London streets and A-roads
- Check the latest London air quality forecast and pollution levels

For the full list of tools and parameters, see the [Tools Reference](tools.md).

To learn how to install and connect this server to your AI assistant, check out the [Installation Guide](installation.md).
