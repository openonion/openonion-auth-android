# OpenOnion Auth

Android authenticator today; OpenOnion's human-approval surface tomorrow.

> **Status: developer preview.** Version 0.1.0 is being built as a local,
> offline TOTP authenticator. Until the security review is complete, use only
> test credentials—not production accounts.

## Product direction

OpenOnion Auth will be delivered in deliberately small phases:

1. **Local authenticator** — scan standard TOTP/HOTP QR codes, encrypt them on
   the Android device, and generate codes offline.
2. **One Agent approval** — pair one ConnectOnion Agent and approve one
   origin-bound MFA request without exporting the long-lived TOTP seed.
3. **Approval platform** — later, become the common human-consent surface for
   OpenOnion protocol actions that require a person.

The first design goal is understanding and freezing the trust boundaries, not
inventing cryptography. Start with the Chinese [concept map](docs/concept-map.zh-CN.md).

## Version 0.1

- Scan a standard `otpauth://totp` QR code or paste the URI.
- Encrypt each credential locally with AES-256-GCM; the wrapping key is created
  by Android Keystore and is not exportable through the app.
- Generate 6- or 8-digit SHA-1, SHA-256, and SHA-512 TOTP codes offline.
- Protect codes from screenshots and exclude the vault from Android backup and
  device transfer.
- Keep Agent access, credential export, cloud sync, and HOTP disabled until
  their protocols are reviewed.

The communication and synchronization proposal is shown in the Chinese
[Agent architecture map](docs/agent-communication.zh-CN.md). The central rule is
that an Agent identity key identifies an Agent; it does not automatically unlock
the human's credential vault.

Planning issue: [openonion/connectonion#1358](https://github.com/openonion/connectonion/issues/1358)

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew test lint assembleDebug
```

The development APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
Published v0.1 artifacts are debug-key-signed developer previews and are not
Google Play releases.

## Test data

Do not scan a production MFA secret yet. For development, create a disposable
test account or use a published RFC 6238 fixture. The unit suite verifies all
three supported HMAC algorithms against RFC 6238 vectors.

## Planned identity

```text
Product       OpenOnion Auth
Android ID    ai.openonion.auth
Repository    openonion-auth-android
```

## Security

Never commit recovery phrases, TOTP seeds, account private keys, Android signing
keys, production QR codes, or real credential backups. Version 0.1 has not had an
independent cryptographic review, does not yet require a biometric prompt each
time the app opens, and has no recovery mechanism. Do not use it as the sole MFA
holder for a production account.

## License

Apache-2.0. See [LICENSE](LICENSE).
