# Securing an API Key for the TfL API

The server can run **without an API key**, but queries will be strictly rate-limited to just a handful of anonymous requests per minute per IP address.

Providing the `TFL_APP_KEY` environment variable significantly raises the rate limits, allowing continuous AI usage. Optionally, you can also provide `TFL_APP_ID` alongside `TFL_APP_KEY` if your TfL registration issued you an App ID (older registrations).

### Steps to Register an API Key

1. Go to the [TfL API Portal Registration Page](https://api-portal.tfl.gov.uk/signup).
2. Create an account and verify your email.
3. Once logged in, navigate strictly to the **Products** section and register for the "500 requests per min" default tier product.
4. Retrieve the **Primary Key** generated for your application.

### Passing the Key to the MCP Server

For **Node.js MCPB** or **Java direct**, pass it via the `env` dictionary in your `claude_desktop_config.json`:

```json
"env": {
  "TFL_APP_KEY": "your_primary_key"
}
```

For **Docker**, pass it as a `-e` flag in `args`:

```json
"args": [
  "run",
  "-i",
  "--rm",
  "-e",
  "TFL_APP_KEY=your_primary_key",
  "ghcr.io/oneill9/mcp-server-tfl:latest"
]
```
