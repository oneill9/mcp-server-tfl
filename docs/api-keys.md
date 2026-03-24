# Securing an API Key for the TfL API

The server can run **without an API key**, but queries will be strictly rate-limited to just a handful of anonymous requests per minute per IP address.

Providing the `TFL_APP_KEY` environment variable significantly raises the rate limits, allowing continuous AI usage.

### Steps to Register an API Key

1. Go to the [TfL API Portal Registration Page](https://api-portal.tfl.gov.uk/signup).
2. Create an account and verify your email.
3. Once logged in, navigate strictly to the **Products** section and register for the "500 requests per min" default tier product.
4. Retrieve the **Primary Key** generated for your application.

### Passing the Key to the MCP Server
If you're using Docker within the Claude Desktop `claude_desktop_config.json`, simply pass it as an environment variable via `args`:

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

Or pass it as part of the `env` dictionary if you are directly executing the java process:

```json
"env": {
  "TFL_APP_KEY": "your_primary_key"
}
```
