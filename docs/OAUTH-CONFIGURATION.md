# Experimental OAuth configuration

`app/src/main/assets/public-google-client.json` contains the published installed-client configuration used by the community Antigravity connector. It is copied from desktop Sotto's documented configuration, originally verified against OmniRoute's public [client defaults](https://github.com/diegosouzapw/OmniRoute/blob/release/v3.8.52/open-sse/utils/publicCreds.ts).

These are shared application identifiers, not a user's API key, access token, refresh token or password. User tokens are obtained after browser consent and saved encrypted with Android Keystore. No desktop sessions are copied into this project or APK.

The connector uses an ephemeral listener on `127.0.0.1:20128`, random OAuth state, bounded sign-in time, direct HTTPS token exchange and desktop Sotto's account/model protocol. The listener closes on completion or cancellation. This compatibility flow currently lacks PKCE and has not been security-audited. Public client configuration does not establish Google's authorization or guaranteed access.

Use this unofficial integration at your own risk. API connections work independently. Mobile sign-in has not been verified against a real account on a physical Android phone.

OmniRoute attribution and its MIT license are included in [OmniRoute-MIT.txt](OmniRoute-MIT.txt).
