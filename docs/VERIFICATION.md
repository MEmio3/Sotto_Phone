# Android verification

The **0.1.0 baseline** was verified on **17 October 2026**, using a disposable Android 15 / API 35 x86_64 emulator. No personal phone, desktop credentials or real account session was used. The app targets API 36 and declares Android 8.0 / API 26 and later. Update checks are recorded separately below.

## 0.1.2 browser return and Android compatibility

The report concerns two physical phones: Nothing Phone 3a completed browser consent before failing in Sotto; Samsung S24 Ultra showed a token-exchange DNS error. Their exact Android/network settings have not been inspected. A browser receiving an authorization code does not prove successful token exchange or model access.

Using the app's **normal UID without an instrumentation runner**, public HTTPS requests to both Google token hosts returned **400** with deliberately incomplete data while Sotto was visible. After enabling Data Saver on metered emulator mobile data and putting a separate app in front, both failed with **UnknownHostException / EAI_NODATA**. In a fresh process, the new foreground gate waited without making those requests while the other app was in front. Opening `studio.sotto.mobile://signin` resumed Sotto; both endpoints returned **400** while Data Saver remained enabled. This demonstrates the reproduced mechanism and handoff recovery, not the physical phones' cause or a successful real-account login.

The full normal-UID report is [ANDROID-GOOGLE-NETWORK.txt](ANDROID-GOOGLE-NETWORK.txt). Early instrumentation experiments are excluded as proof of background policy: Android's runner kept the process in foreground importance. The committed test instead uses a **debug-only**, credential-free service and reads its private report with `run-as`. The release APK must contain neither that service nor its manifest entry.

Run `scripts/test-google-network.ps1 -Serial emulator-5562` on a disposable emulator with working mobile data. It builds/install debug fixtures, temporarily changes Data Saver/Wi-Fi, checks foreground → restricted background → return handoff, then disables Data Saver and re-enables Wi-Fi. It rejects physical-device serials. No authorization code, API key, access token or refresh token is used.

The JVM suite now has **31 passing tests**, including foreground blocking, cancellation/timeout and another browser switch. Compatibility fixes guard the older keyboard-switch path and move newer navigation-bar/font attributes into API-qualified resources. Windows SDK paths are escaped correctly in generated properties. The universal Java APK has no CPU or brand filter and no Google Play Services dependency. This does not establish compatibility with every manufacturer/editor or Android versions below 8.0. Physical-device/account retry remains required.

Full **`:app:lintRelease` passed with zero errors** (23 non-blocking warnings remain). The native integration suite was rerun after the compatibility changes and passed: five modes, encrypted storage, normal keyboard typing, suggestions/autocorrect and undo, mapped output languages, automatic Bangla/English insertion into a separate app UID, no automatic submission, editor/password guards, light/dark appearance, font scaling and landscape. The fresh report is [ANDROID-0.1.2-TEST-RESULTS.txt](ANDROID-0.1.2-TEST-RESULTS.txt). This run used 720 × 1280 portrait and 1280 × 720 landscape; the earlier expanded-layout captures remain baseline evidence.

## 0.1.1 Google connection regression checks

The reported screen showed the generic error produced by Android `UnknownHostException` after browser consent. The phone's resolver/ISP state cannot be inspected from that screenshot. Separately, the Android port constructed incorrect Google RPC paths (`/v1internal/action`). Those paths were reproduced against Google: the wrong path returned **404**, while the correct `/v1internal:loadCodeAssist` path returned **401 UNAUTHENTICATED** without a token. This demonstrates valid routing, not account access.

Public probes with deliberately invalid authorization data reached both Google token hostnames and received **400 invalid_request**. No private credentials or real authorization code was used.

New regression tests cover exact RPC paths/headers, DNS failure after consent, fallback restricted to Google HTTPS hosts, unchanged token grants, refresh recovery, safe errors, OAuth rejection, cancellation, discovery fallback, and prevention of retries after TLS/read-timeout/permission/quota errors. An actual loopback HTTP test checks that OAuth error descriptions cannot leak private data into displayed errors. Original writing-mode/keyboard/provider tests remain in the suite.

**All 28 JVM tests passed**, instrumentation test compilation passed, and the 0.1.1 signed release build and release lint checks passed.

This update is signed with the existing release key and increments Android versionCode to **2**, so it can update 0.1.0 in place. Actual Google login on the affected physical phone still needs a retry with this APK; no successful real-account login is claimed by the mock or unauthenticated probes.

## Build and automated checks

- **16 JVM tests passed**: five distinct prompt modes, language precedence, strict output validation, Unicode search, safe endpoint validation, provider responses, configurable keyboard commands, offline spelling and word boundaries.
- **Native integration passed** with Android's actual input-method service and a separate app UID as the destination editor.
- **Signed release build passed**, including Android's release lint checks. The installable package is `dist/Sotto-0.1.0-Android.apk`; its SHA-256 is in `dist/SHA256SUMS.txt`.
- Android `apksigner` verified the release's RSA-3072 signature. A clean installation of that signed APK launched successfully on the emulator. The final landscape rail capture comes from that release. APK inspection found no private signing key, saved credentials/history or synthetic test key.

The native run checked:

| Behavior | Result |
| --- | --- |
| Android Keystore encryption hides the synthetic API key in saved credential data | Passed |
| Each writing mode reaches the provider and stays saved | Passed |
| Explicit output language overrides mode and spoken cues | Passed |
| Settings switches independently control suggestions and autocorrect | Passed |
| Settings can add X → Spanish | Passed |
| QWERTY keys type normal text into a separate app | Passed |
| Suggestion selection completes the current word | Passed |
| `teh` + Space corrects to `the ` | Passed |
| Immediate Backspace restores `teh`; a subsequent Space keeps it | Passed |
| Disabling autocorrect preserves `teh ` | Passed |
| Disabling suggestions hides the suggestion strip | Passed |
| While recording, X selects Spanish and B selects Bangla without typing those letters | Passed |
| Microphone capture → local mock transcription/editing → automatic multiline Bangla/English insertion | Passed |
| Dictation never submits the destination message | Passed |
| Changing the active editor prevents automatic insertion | Passed |
| Password fields disable voice dictation | Passed |

The raw native report is [ANDROID-TEST-RESULTS.txt](ANDROID-TEST-RESULTS.txt). The test records **emulated microphone audio** and uses a loopback HTTP provider with synthetic responses. It verifies capture, transport, state management and insertion; it does **not** measure speech-recognition accuracy or real provider availability.

Run unit tests with `gradlew.bat :app:testDebugUnitTest`. `scripts/test-android.ps1` builds and runs the native checks on a disposable emulator; it deliberately rejects physical-device serials because it installs test apps and changes keyboard settings.

## Native layout review

Reviewed actual Android screenshots in light and dark appearance, at font scale **1.3**, and in true **2400 × 1080 landscape**. Configuration values and frame dimensions were asserted in the native runner. Reviewed the app, selected navigation, model picker, keyboard suggestions, automatic insertion and keyboard settings.

Resolved font-weight rendering, Material surface colors, microphone contrast, navigation selection, keyboard gesture insets, short-height model-sheet actions and landscape navigation allocation. No browser mockup substitutes for these native captures.

## Remaining verification limits

- Physical phone microphone quality, manufacturer keyboard behavior and real apps such as Discord need device testing.
- Real Gemini/compatible provider requests and mobile Google browser login were not exercised with personal credentials. Google/Antigravity remains explicitly experimental; its compatibility OAuth flow currently lacks PKCE.
- On-device recognition depends on the phone's Android speech service and installed language model. It was not evaluated for Bangla or English accuracy.
- Offline spelling is English-only. The keyboard does not yet include swipe typing, emoji search, learned personal vocabulary or Gboard's contextual prediction model.
- AI output can still misunderstand speech. Original transcripts and uncertain-word review are available; no accuracy guarantee is made.
