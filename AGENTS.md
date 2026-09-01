# Repository Guidelines

## Current phase

This repository is design-first. Do not implement production cryptography or
accept real credentials until the applicable architecture decision, threat
model, schemas, and test vectors have been reviewed.

## Product boundaries

- Phase 0 is a local, offline TOTP/HOTP authenticator.
- Phase 1 adds one explicit human-approved Agent use.
- Generalized Agent approvals, policy automation, passkeys, iOS, and public
  distribution are later work unless an issue explicitly brings them into scope.
- This app is not the OpenOnion Messages SMS client.

## Cryptographic rules

- Use established standards and reviewed libraries; do not design new primitives.
- Keep TOTP secrets, identity signing keys, device approval keys, and vault
  encryption keys separate.
- Never put the BIP-39 account seed or SLIP-0010 master node in the Android app.
- Never expose a TOTP seed or OTP value to model context, logs, telemetry, crash
  reports, notifications, screenshots, or unprotected system backups.
- Add published RFC vectors and cross-language protocol fixtures before relying
  on an implementation.
- Any change to key derivation, recovery, signing, encryption, canonicalization,
  or replay behavior requires a recorded design decision and security review.

## Android conventions

Use Kotlin, Jetpack Compose, four-space indentation, `PascalCase` types, and
`camelCase` members. Keep security-sensitive logic independent of UI code and
covered by deterministic unit tests. Prefer Android Keystore/StrongBox for
non-exportable device keys and gate sensitive use with system authentication.

## Testing

Never use real accounts or secrets in tests. Keep fixed public test vectors in a
dedicated fixture directory. When Android code exists, run focused unit tests,
then the complete Gradle test/lint/build checks and record a clean-device journey.
