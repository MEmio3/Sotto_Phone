# Android verification

The **0.1.0 baseline** was verified on **17 October 2026**, using a disposable Android 15 / API 35 x86_64 emulator. No personal phone, desktop credentials or real account session was used. The app targets API 36 and supports Android 8.0 / API 26 and later. The 0.1.1 connection regression checks are recorded below; baseline native results are not claimed as a fresh emulator run of this update.

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
