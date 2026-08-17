# Securing an API Key for the TfL API

An API key is **strongly recommended** for effective use. Without one, TfL strictly rate-limits anonymous requests to a handful per minute per IP address, which will impact most real-world usage.

Providing the `TFL_APP_KEY` environment variable significantly raises the rate limits, allowing continuous AI usage. Optionally, you can also provide `TFL_APP_ID` alongside `TFL_APP_KEY` if your TfL registration issued you an App ID (older registrations).

### Steps to Register an API Key

1. Go to the [TfL API Portal Registration Page](https://api-portal.tfl.gov.uk/signup).
2. Create an account and verify your email.
3. Once logged in, navigate strictly to the **Products** section and register for the "500 requests per min" default tier product.
4. Retrieve the **Primary Key** generated for your application.

### Passing the Key to the MCP Server

For a source-based stdio configuration, pass it via the `env` dictionary in your `claude_desktop_config.json`. The MCPB installer exposes the same value as a sensitive configuration field.

```json
"env": {
  "TFL_APP_KEY": "your_primary_key"
}
```

For **Docker**, expose the client process environment and ask Docker to forward it into the container:

```json
"args": [
  "run",
  "-i",
  "--rm",
  "-e",
  "TFL_APP_KEY",
  "ghcr.io/oneill9/tfl-mcp-server:latest"
],
"env": {
  "TFL_APP_KEY": "your_primary_key"
}
```
