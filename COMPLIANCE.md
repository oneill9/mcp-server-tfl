# Compliance Statement

**Last updated: 2026-03-26**

> **Disclaimer:** This is an **community-built** project. It is **not affiliated with, endorsed by, or connected to Transport for London (TfL)** in any way.

This document records the compliance review for the **TfL MCP Server** against the Anthropic Software Directory requirements.

---

## Anthropic Software Directory Terms

We have reviewed the [Anthropic Software Directory Terms](https://support.claude.com/en/articles/13145338-anthropic-software-directory-terms) and confirm that this server complies with them. Specifically:

- The server is described accurately — it is a read-only proxy to the TfL Unified API with no write operations.
- No misleading claims are made about capabilities, data handling, or third-party affiliations.
- The server does not impersonate Anthropic or any other entity.
- All open-source dependencies are used in accordance with their respective licences.

## Anthropic Software Directory Policy

We have reviewed the [Anthropic Software Directory Policy](https://support.claude.com/en/articles/13145358-anthropic-software-directory-policy) and confirm that this server complies with it. Specifically:

- The server is a read-only integration with a public third-party API (TfL) — no destructive or sensitive operations are exposed.
- No user personal data is collected, stored, or transmitted beyond forwarding queries to TfL in real time (see [PRIVACY.md](PRIVACY.md)).
- The server does not facilitate or encourage any activity that violates Anthropic's usage policies.
- The server logo and branding are original and do not infringe TfL trademarks. The project is clearly marked as community-built, with no affiliation to Transport for London.

## Commitment to Security, Responsiveness, and Accuracy

The maintainer commits to the following ongoing obligations:

| Obligation | How it is met |
|------------|---------------|
| **Security** | Dependencies are kept up to date via Dependabot. Security issues reported via GitHub Issues are triaged and patched promptly. Secrets (API keys) are never hardcoded — only supplied via environment variables. |
| **Responsiveness** | Bug reports, questions, and pull requests are reviewed via [GitHub Issues](https://github.com/oneill9/mcp-server-tfl/issues). |
| **Accurate descriptions** | The README, tool descriptions, and submission form content are kept in sync with the actual server behaviour. Any change to tool names, capabilities, or data handling is reflected in the documentation before or alongside the code change. |

## Third-Party API Notice

This server acts as a proxy to the **TfL (Transport for London) Unified API**. It is not affiliated with or endorsed by Transport for London. TfL data is subject to [TfL's terms of service](https://tfl.gov.uk/corporate/terms-and-conditions/transport-data-service) and [privacy policy](https://tfl.gov.uk/corporate/privacy-and-cookies/).

---

For questions about compliance, open an issue at <https://github.com/oneill9/mcp-server-tfl/issues>.
