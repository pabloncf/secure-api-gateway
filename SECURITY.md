# Security Policy

## Supported Versions

This is a portfolio/demonstration project. Security fixes are applied to the `main` branch only.

| Version | Supported |
|---|---|
| latest (`main`) | Yes |

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please report it responsibly.

**Do not open a public GitHub issue for security vulnerabilities.**

Instead, send an email to **pabloncf@gmail.com** with:

- A description of the vulnerability and its potential impact
- Steps to reproduce or a proof-of-concept
- Any suggested mitigations, if you have them

You can expect an acknowledgement within **48 hours** and a status update within **7 days**.

## What qualifies as a vulnerability?

Examples of in-scope issues:

- Authentication bypass (JWT validation flaws, missing auth checks)
- Authorization flaws (privilege escalation between roles)
- Input validation bypasses that reach the business logic layer
- Security misconfiguration in Docker Compose or Spring Security
- Sensitive data exposure (tokens, passwords, PII in logs or responses)

Examples of out-of-scope issues:

- Vulnerabilities in upstream dependencies (report directly to the dependency maintainer)
- Issues that require physical access to the host
- Theoretical attacks with no practical exploit path

## Security Design Notes

This project implements several security controls as learning exercises. See the [README — Security Decisions](README.md#security-decisions) section for the rationale behind each design choice.

Known intentional limitations (not vulnerabilities):

- **No JWT revocation**: tokens are valid until expiry. This is a documented trade-off for stateless simplicity.
- **In-memory rate limit state**: restarting Redis resets all rate limit counters. This is expected behavior for a single-node demo.
- **Default credentials in `.env.example`**: the example file uses weak defaults. Production deployments must override all secrets via environment variables.

## Disclosure Timeline

| Step | Target |
|---|---|
| Reporter submits vulnerability | Day 0 |
| Acknowledgement sent | ≤ 48 hours |
| Triage and severity assessment | ≤ 7 days |
| Fix released (if confirmed) | ≤ 30 days |
| Public disclosure | After fix is released |
