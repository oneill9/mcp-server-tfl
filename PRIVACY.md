# Privacy Policy

**Last updated: 2026-08-09**

> **Disclaimer:** This is a **community-built** project. It is **not affiliated with, endorsed by, or connected to Transport for London (TfL)** in any way.

The TfL MCP Server (`tfl-mcp-server`) is an open-source local proxy that forwards transport queries from your AI assistant to the [TfL (Transport for London) Unified API](https://api.tfl.gov.uk/). This policy describes how the server handles data.

## Data Collection

This server does **not** collect or transmit personal data to the maintainer. It has no analytics or telemetry. It writes startup, transport-error, and limited service-status diagnostic messages to standard error; those diagnostics do not include TfL API credentials and are not persisted by the server. A host or container platform may retain standard-error output according to its own logging configuration.

## Usage and Storage

Queries entered by the user (e.g. stop IDs, line names, journey origins/destinations) are forwarded directly to the TfL API in real time. Responses are returned to the user immediately. No query history, results, or identifiers are persisted to disk or any external service.

## Third-Party Services

Requests are forwarded to the **TfL Unified API** at `https://api.tfl.gov.uk`. TfL may log API requests in accordance with their own policies. See [TfL's Privacy and Cookies policy](https://tfl.gov.uk/corporate/privacy-and-cookies/) for details.

If you provide a `TFL_APP_KEY`, it is sent as a query parameter to TfL's servers on each request. It is never stored or forwarded to any other party.

## Data Retention

The server holds no state between requests and does not retain data itself. Host-managed standard-error retention, if enabled, is controlled by the host rather than this server.

## Children's Privacy

This server does not knowingly collect any information from or about children.

## Changes to This Policy

Any changes to this privacy policy will be reflected in this file. The "Last updated" date at the top of this document will be revised accordingly.

## Contact

For privacy concerns or questions, please open an issue at:
<https://github.com/oneill9/tfl-mcp-server/issues>
