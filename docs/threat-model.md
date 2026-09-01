# Threat model

> Status: scaffold only. This must be completed before real credentials are used.

## Assets

- third-party TOTP/HOTP secrets;
- encrypted credential vault and recovery material;
- Android device approval key;
- Agent and device bindings;
- approval request integrity and audit history.

## Initial adversaries

- a reader of a server database or encrypted backup;
- a malicious or compromised Agent;
- an unpaired device or network attacker;
- phishing content asking for a code on the wrong origin;
- accidental leakage through logs, screenshots, clipboard, notifications,
  telemetry, crash reports, or backups;
- device loss and offline filesystem extraction.

## Explicit early boundaries

- Phase 0 does not protect against a fully compromised unlocked Android device.
- TOTP is phishable and cannot prove human presence by itself.
- A copied TOTP seed cannot be remotely revoked; the third-party credential must
  be rotated.
- Rooted-device policy, encrypted cloud backup, relay availability, and advanced
  Agent policies remain design decisions.

## Review gates

1. accepted vault/key-storage decision;
2. accepted recovery decision and destructive loss exercise;
3. published RFC test vectors passing on Android;
4. inspection proving no seed/code leakage through platform surfaces;
5. independent cryptographic review before production credentials;
6. Phase 1 protocol and replay/origin-binding review before Agent integration.
