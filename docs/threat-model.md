# Threat model

> Status: v0.1 developer-preview baseline. Independent review and the remaining
> gates below are still required before real credentials are used.

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

## v0.1 implemented controls

- no Internet permission, Agent transport, credential export, or cloud sync;
- AES-256-GCM authenticated encryption with a non-exportable Android Keystore
  wrapping key and per-record random nonce;
- credential ID bound as AEAD additional authenticated data;
- app data excluded from Android backup and device transfer;
- `FLAG_SECURE` on distributable builds; a separate non-distributable screenshot
  build type exists only for UI evidence;
- copied OTP values are marked sensitive for Android clipboard handling;
- RFC 6238 SHA-1/SHA-256/SHA-512 test vectors; and
- strict TOTP-only URI parsing with duplicate-field and issuer checks.

## v0.1 known gaps

- no biometric/device-credential prompt on each app open;
- no recovery, encrypted export, device revocation, or key rotation flow;
- third-party QR scanning dependency and Android Keystore behavior require
  independent review across supported devices;
- debug-key-signed GitHub APK is for disposable test credentials only; and
- TOTP remains phishable and a copied code temporarily exists in the system
  clipboard when the user explicitly selects Copy.

## Review gates

1. accepted vault/key-storage decision;
2. accepted recovery decision and destructive loss exercise;
3. published RFC test vectors passing on Android;
4. inspection proving no seed/code leakage through platform surfaces;
5. independent cryptographic review before production credentials;
6. Phase 1 protocol and replay/origin-binding review before Agent integration.
