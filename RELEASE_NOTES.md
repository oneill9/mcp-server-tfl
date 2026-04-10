## What's Changed in v1.3.0

### New Features
- Add Node.js MCPB server with all 12 tools (634be5e)
- Add bike_points tool to Java and Node.js implementations (1c40a96)
- Add air_quality and road_disruptions tools to Node.js implementation (ba0cb84)
- Add MCPB build script and skill for Claude Desktop testing (461d6ff)
- Add PNG icon for MCPB and clean build dir on rebuild (44f6f98)

### Improvements
- Add tool titles, destructiveHint annotations, and submission use cases (72e7eab)
- Add Node.js build and test to pre-commit hook (4676da1)
- Add Node.js CI workflows and update docs for dual implementations (8d08e9d)
- Update node icon (936b74b)

### Fixes
- Fix: align Node.js contract test with intentionally removed tools (945241b)
- Fix: replace deprecated gradle-home-cache-cleanup with cache-cleanup (79e1e73)
- Fix: add node types to tsconfig to resolve process is not defined errors (3b801f3)
- Fix: set artifact versions from git tag in release workflow (290d8de)
- Fix: add missing build step to release workflow test-node job (662c423)
- Fix: add missing build step to Node.js CI workflow (a0529b7)
- Fix: add zod as direct dependency to fix CI test failures (d07c38b)

### CI
- Run contract tests on pull requests (61b7519)
- Restrict release workflow to tag pushes only (bc00c25)
- Group Dependabot PRs to reduce review burden (2756972)

### Dependencies
- Bump the npm-dependencies group in /node with 4 updates (7281bf7)
- Bump the gradle-dependencies group with 2 updates (242d94f)
- Bump actions/upload-artifact from 4 to 7 (0cf19cc)
- Bump actions/download-artifact from 4 to 8 (eecf06a)
- Bump actions/checkout from 4 to 6 (023020d)
- Bump actions/setup-python from 5 to 6 (b3174b5)
- Bump tools.jackson.core:jackson-databind (4f80744)
- Bump io.modelcontextprotocol.sdk:mcp from 1.1.0 to 1.1.1 (664eaa1)
- Bump log4j from 2.25.3 to 2.25.4 (837e7e7)
