# OpenOnion Auth

Android authenticator today; OpenOnion's human-approval surface tomorrow.

> **Status: design bootstrap.** There is no APK and no production-ready
> cryptography in this repository yet. Do not place real authentication secrets
> in it.

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

## Current repository scope

- product and security concepts;
- architecture and threat-model decisions;
- protocol schemas and test vectors once reviewed; and
- the Android project after the local-vault design is accepted.

Planning issue: [openonion/connectonion#1358](https://github.com/openonion/connectonion/issues/1358)

## Planned identity

```text
Product       OpenOnion Auth
Android ID    ai.openonion.auth
Repository    openonion-auth-android
```

## Security

Never commit recovery phrases, TOTP seeds, account private keys, Android signing
keys, production QR codes, or real credential backups. Security-sensitive design
must receive independent review before production credentials are used.

## License

TBD before the repository is made public.
