# Architecture decisions

Record one reviewed decision per file before implementation depends on it.

Planned decisions:

1. local vault and Android Keystore boundary;
2. recovery and encrypted backup model;
3. TOTP/HOTP compatibility profile and QR parsing;
4. Agent/device pairing identities;
5. canonical approval request and grant schemas;
6. one-time code handoff and browser origin binding.

Each decision should state the threat being addressed, alternatives considered,
chosen boundary, migration/rotation behavior, failure modes, test evidence, and
what remains out of scope.
