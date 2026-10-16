# Experimental OAuth configuration

`app/src/main/assets/public-google-client.json` contains the published installed-client configuration used by the community Antigravity connector. It is copied from desktop Sotto's documented configuration, originally verified against OmniRoute's public [client defaults](https://github.com/diegosouzapw/OmniRoute/blob/release/v3.8.52/open-sse/utils/publicCreds.ts).

These are shared application identifiers, not a user's API key, access token, refresh token or password. User tokens are obtained after browser consent and saved encrypted with Android Keystore. No desktop sessions are copied into this project or APK.

The connector uses an ephemeral listener on `127.0.0.1:20128`, random OAuth state, bounded sign-in time, direct HTTPS token exchange and desktop Sotto's account/model protocol. The listener closes on completion or cancellation. This compatibility flow currently lacks PKCE and has not been security-audited. Public client configuration does not establish Google's authorization or guaranteed access.

Use this unofficial integration at your own risk. API connections work independently. Mobile sign-in has not been verified against a real account on a physical Android phone.

In **0.1.1**, account/model RPCs use the required `/v1internal:action` syntax. Bootstrap uses `cloudcode-pa.googleapis.com`; runtime uses `daily-cloudcode-pa.googleapis.com`, with a Google-host fallback for connection failures and read-only discovery endpoint mismatches, matching the [upstream routing profile](https://github.com/diegosouzapw/OmniRoute/blob/release/v3.8.52/open-sse/config/antigravityUpstream.ts).

Token exchange uses `https://oauth2.googleapis.com/token`. Only DNS, connection-refused or no-route failures before connection permit a retry at Google's legacy `https://www.googleapis.com/oauth2/v4/token` endpoint. Neither TLS failures, read timeouts nor OAuth/HTTP rejections trigger authorization-code replay. TLS certificate and hostname verification remain enabled; redirects are not followed, and no third-party proxy receives credentials. Refresh uses the same bounded recovery.

GitHub push-protection notifications concern the two published client defaults in source control. They are separate from a phone's DNS/connection failure. Personal Google tokens are never published. A Google client rejection (`invalid_client`) is identified separately and requires a compatible client configuration or API connection, rather than repeated network retries.

OmniRoute attribution and its MIT license are included in [OmniRoute-MIT.txt](OmniRoute-MIT.txt).
