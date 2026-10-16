# Sotto for Android

<!-- impeccable:product-schema 1 -->

## Platform
android

## Stack
Native Android Views in Java, Material 3 components, Android InputMethodService keyboard. Android 8.0 and later. Build an installable APK without a server owned by Sotto.

## Users
People dictating in Bangla, English or mixed languages into chats, browser forms and other Android text fields, often one-handed.

## Product Purpose
Record explicitly, faithfully rewrite speech, and insert the result directly into the active field through the Sotto keyboard. Offer the same five writing modes, direct AI connections and searchable local history as desktop Sotto.

## Operating Context
An Android phone app and optional keyboard. Tap the microphone to start and stop, replacing the desktop Ctrl+P shortcut. People enable and select the keyboard through Android Settings themselves. Recording and processing states must be visible; focus changes must never send text to a different destination. Password fields disable voice dictation. Typed keyboard input is neither logged nor sent to an AI.

## Capabilities and Constraints
Constructive, Precise (default), Professional, Prompting and Casual writing modes. Explicit language selection and optional spoken language cues. Gemini API, compatible transcription/chat APIs and experimental direct Google/Antigravity login with a live model picker. Record audio locally; send audio or transcript to the selected provider for cloud processing. Keep credentials encrypted with Android Keystore and history in app-private storage. Google availability depends on the account and unofficial integration; never imply guaranteed free access. No desktop sessions or personal credentials may be copied into the APK. No accessibility service or overlay permission is needed for keyboard insertion.

The keyboard includes a native QWERTY typing layout and a top-right microphone, plus Shift/Caps Lock, symbols, editing and manual Enter. Independent settings control offline English suggestions and reversible autocorrect. Spelling reads the current word locally, with no typing logs or cloud requests; sensitive/structured fields suppress spelling. Configurable A–Z shortcuts choose the voice output language only while recording, defaulting to B for Bangla and E for English. This familiar layout is Sotto's own implementation, not bundled Gboard. English typing/spelling and multilingual AI voice output are distinct capabilities.

## Brand Commitments
Preserve Sotto's black-and-white palette, Manrope brand lettering, understated editorial hierarchy and capsule controls. Adapt structure to Android Material 3: native app bar, chips, navigation bar on compact width and rail on expanded width, system Back, window insets, 48dp touch targets, scalable text, light and dark themes.

## Open Decisions
User explicitly requested project folder E:/SottoApk and the mobile version of Sotto. Android APK is inferred from the folder name. Phone-first portrait and landscape, with responsive expanded layout rather than locked orientation. Physical-device microphone behavior and account login need honest verification limits.

## Product Principles
- Preserve meaning, names, numbers, negations and language intent.
- Prompting writes a prompt without answering it.
- Only record after an explicit tap; show microphone and processing state.
- Insert at the original active editor without submitting the message.
- Keep failed work recoverable and private keys out of logs and history.
