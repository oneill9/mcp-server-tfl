# Third-party notices

## Model Context Protocol TypeScript SDK conformance fixture

`node/test/support/conformance-server.ts` is adapted and modified from
`test/conformance/src/everythingServer.ts` in the Model Context Protocol
TypeScript SDK.

- Upstream project: https://github.com/modelcontextprotocol/typescript-sdk
- Exact source revision: `3c7ddafa05d8f17fb52168bf4638f09251c3d0ff`
- Source file: https://github.com/modelcontextprotocol/typescript-sdk/blob/3c7ddafa05d8f17fb52168bf4638f09251c3d0ff/test/conformance/src/everythingServer.ts
- License at that revision: https://github.com/modelcontextprotocol/typescript-sdk/blob/3c7ddafa05d8f17fb52168bf4638f09251c3d0ff/LICENSE

The local copy removes the upstream standalone serving layer, adapts imports
and types to this repository, composes the production TfL registrations, and
retains diagnostic registrations needed by the official conformance referee.

At that revision the upstream project states that it is transitioning from
MIT to Apache-2.0: contributions with relicensing consent and new code are
Apache-2.0, while older contributions without consent remain MIT. The upstream
copyright notice for MIT-licensed portions is:

> Copyright (c) 2024-2025 Model Context Protocol a Series of LF Projects, LLC.

The complete upstream license and transition terms are available at the exact
revision link above. Copies of the applicable license texts are included in
this repository's `LICENSES` directory.
