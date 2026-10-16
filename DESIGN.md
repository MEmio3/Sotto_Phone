---
name: Sotto for Android
description: A little less typing, faithful speech into active Android fields.
colors:
  primary: "#191919"
  on-primary: "#FFFFFF"
  surface: "#F8F8F8"
  on-surface: "#191919"
  surface-variant: "#EAEAEA"
  on-surface-variant: "#606060"
  outline: "#777777"
  outline-variant: "#D2D2D2"
  recorder-surface: "#1C1C1C"
  recorder-text: "#F7F7F7"
  recorder-muted: "#B8B8B8"
  error: "#B3261E"
  dark-primary: "#F5F5F5"
  dark-on-primary: "#191919"
  dark-surface: "#111111"
  dark-on-surface: "#F5F5F5"
  dark-surface-variant: "#2D2D2D"
  dark-on-surface-variant: "#BEBEBE"
  dark-outline: "#A0A0A0"
  dark-outline-variant: "#444444"
  dark-error: "#F2B8B5"
typography:
  headline:
    fontFamily: "Manrope"
    fontSize: "30sp"
    fontWeight: 650
  wordmark:
    fontFamily: "Manrope"
    fontSize: "26sp"
    fontWeight: 700
  title:
    fontFamily: "Manrope"
    fontWeight: 600
  body:
    fontFamily: "Manrope"
    fontSize: "14sp"
    fontWeight: 400
  label:
    fontFamily: "Manrope"
    fontSize: "14sp"
    fontWeight: 600
  caption:
    fontFamily: "Manrope"
    fontWeight: 400
  keyboard-letter:
    fontFamily: "Manrope"
    fontSize: "22sp"
    fontWeight: 400
  keyboard-control:
    fontFamily: "Manrope"
    fontSize: "12sp"
rounded:
  keycap: "8dp"
  card: "20dp"
  chip: "24dp"
  button: "28dp"
spacing:
  tight: "4dp"
  inline: "8dp"
  small: "12dp"
  section: "16dp"
  card-inset: "20dp"
  page-inset: "24dp"
  bottom-inset: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label}"
    rounded: "{rounded.button}"
  button-outlined:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    typography: "{typography.label}"
    rounded: "{rounded.button}"
  chip-selected:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.chip}"
  chip-unselected:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.chip}"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.card}"
    padding: "20dp"
  recorder:
    backgroundColor: "{colors.recorder-surface}"
    textColor: "{colors.recorder-text}"
    rounded: "{rounded.card}"
    padding: "20dp"
  keycap:
    backgroundColor: "{colors.surface-variant}"
    textColor: "{colors.on-surface}"
    typography: "{typography.keyboard-letter}"
    rounded: "{rounded.keycap}"
    height: "48dp"
---

# Design System: Sotto for Android

## Overview

**Creative North Star: "Sotto's Quiet Editorial Instrument"**

Sotto carries its desktop monochrome Manrope identity into a native Android tool. Clear sentences, generous page margins and a restrained type hierarchy make speaking feel direct and deliberate. Capsule actions and the dark recorder supply the recognizable silhouette.

The implementation is Java Views with Material 3 controls and an InputMethodService keyboard. Native navigation, system Back, scalable text and window insets are part of the design language. Layouts reflow and scroll in portrait, landscape and larger text; an editorial opening is never a reason to hide an action.

**Key Characteristics:**
- Monochrome semantic roles with a persistent dark recorder.
- Manrope lettering with weight-led editorial hierarchy.
- Capsule controls, flat outlined containers and soft rectangular keycaps.
- Native Android navigation and explicit recording state.

This is a scan of the shipped native build, not a proposed web redesign. Evidence: `app/src/main/res/values/colors.xml`, `values-night/colors.xml`, `values/themes.xml`, `font/manrope.xml`, `Ui.java`, `MainActivity.java`, `SottoKeyboard.java` and review captures in `.impeccable/review/`. The corresponding artifact is `dist/Sotto-0.1.0-Android.apk`.

## Colors

Soft paper, near-black ink and functional grays provide the entire everyday palette. Error feedback is the intentional chromatic exception.

### Primary
- **Ink / inverse ink:** `primary` and `on-primary` define filled actions and selected writing modes. The dark-theme counterparts invert that relationship.
- **Recorder paper:** `recorder-text` is the light recorder action and main recorder text. The action's text and icon use the fixed ink role, independent of theme.

### Neutral
- **Paper / night paper:** `surface` and `dark-surface` are the app and keyboard canvas.
- **Reading ink:** `on-surface` and its dark counterpart carry primary copy.
- **Soft gray / charcoal:** `surface-variant` and its dark counterpart support keycaps and native selection indicators.
- **Secondary reading gray:** `on-surface-variant` and its dark counterpart carry captions and supporting detail.
- **Control outline:** `outline` carries control boundaries; `outline-variant` carries quieter card and chip boundaries. Each resolves through the active theme.
- **Recorder charcoal:** `recorder-surface` remains dark in both themes. `recorder-muted` carries its waveform and supporting copy.

### Functional feedback
- **Error red:** `error` and `dark-error` identify recoverable error text and native error states. Red is a status role, not a general accent.

**The Semantic Inversion Rule.** Resolve ordinary surfaces and controls through Android light/dark resources. Keep the recorder's charcoal, light text and fixed ink action contrast stable.

## Typography

**Display Font:** Manrope variable, supplied in the app.
**Body Font:** Manrope, with Android's native glyph fallback for scripts the bundled face does not cover.

**Character:** A single family creates continuity from wordmark to field label. Weight and size establish hierarchy; sentence case keeps controls conversational.

### Hierarchy
- **Headline:** the app's custom headline role establishes the editorial opening. The helper applies variable weight (650) over the headline appearance.
- **Wordmark:** the app bar uses the dedicated wordmark appearance and its stronger weight (700).
- **Title:** native Material 3 TitleLarge sizing with Manrope weight (600) introduces subsections and recorder messages.
- **Body:** the custom body appearance uses regular Manrope (400). Results add a small amount of line spacing and use first-strong text direction.
- **Label:** Manrope semibold (600) marks controls and section labels. Button labels use the frontmatter's label size; other native labels retain their Material appearance.
- **Caption:** native Material 3 BodySmall sizing, regular Manrope and the secondary text role support metadata.
- **Keyboard:** letter keycaps use the larger keyboard-letter role. Tool capsules and special-key labels use the compact keyboard-control size. Model-picker radio labels use weight (500).

Text dimensions are Android `sp`; spatial dimensions are `dp`. Do not reinterpret them as fixed device pixels. Native Material appearances own inherited line heights and unspecified sizes.

**The One Family Rule.** Use Manrope across Sotto's own controls and editorial copy; retain platform fallback for multilingual text instead of forcing unsupported glyphs.

## Layout

A native app bar sits above one scrollable content column. Compact widths use a labeled bottom navigation bar. At screen width (600dp) and above, navigation becomes a left rail (144dp) with a minimum item height (64dp), top alignment and labels allowed to span two lines.

Content is centered and capped at (760dp), including its page padding. The standard content inset is (24dp) horizontally, (12dp) above and (32dp) below. Cards use (20dp) internal padding. The recurring rhythm is the spacing scale in the frontmatter; inline chip spacing is (8dp) in both directions. Small optical gaps are local adjustments rather than additional global tokens.

The dictation opening contains the headline, writing-mode chips, output-language capsule and recorder in that order. Chips wrap with available width. The page scrolls to preserve access when height or text scale is constrained. Bottom sheets use their own scrollable content; the model picker keeps its actions outside the scrolling model list.

App windows account for system bars, display cutouts and the keyboard. The IME keeps its own bottom navigation clearance and never switches into Android's fullscreen extract layout. Keyboard rows distribute available width by weight, retain (48dp) key height and use (2dp) margins around keycaps.

**The Native Reach Rule.** Preserve window insets, scalable text and native navigation. Primary app controls and key rows keep their observed touch dimensions; do not shrink them to force a fixed first viewport.

## Elevation & Depth

The app's custom cards and navigation are flat at rest, explicitly using zero elevation. Contrast, thin boundaries and inset spacing establish grouping. The recorder's dark fill gives it emphasis without a decorative shadow. Native dialogs, bottom sheets, ripples and other Material surfaces retain their platform behavior; the flat card rule does not prohibit native modal elevation.

**The Flat Surface Rule.** Use tonal fill and quiet outlines for ordinary grouping; do not add decorative shadows to the app's cards or navigation.

## Shapes

Controls are capsules: buttons use the button radius, while writing chips use the chip radius with their observed minimum height (48dp). App cards use the card radius and a thin boundary (1dp); the recorder removes that boundary. The QWERTY keyboard deliberately uses the smaller keycap radius so neighboring letters remain distinct.

Icons are custom stroke drawings on a (24-unit) canvas, with rounded caps and joins. Buttons display their icons at (20dp); navigation and app-bar icons use native sizing. Icon controls carry content descriptions and tooltips. Text labels such as Shift, Delete and Enter are keyboard controls rather than icon substitutes.

## Components

### Buttons

Calm capsules make actions explicit. Filled buttons use the active primary/on-primary pair; outlined buttons use native Material outlined styling. Shared buttons have a minimum height (48dp), zero top/bottom insets, sentence case and semibold Manrope. Icon-only buttons use a (48dp) square target, internal horizontal padding (12dp) and a descriptive accessibility label. Native Material supplies pressed, focus and disabled feedback.

### Chips

Writing modes use native checkable chips with one required selection. Selected chips reverse to primary/on-primary; unselected chips use surface/on-surface. All use the quieter outline role and hide the redundant checked icon. Recording or processing disables mode changes. The native chip group wraps rather than clipping.

### Cards / Containers

Quiet containers use surface fill, the card radius, the standard inset and a thin outline-variant stroke. History previews and provider cards reuse this shell; text hierarchy supplies the internal structure. Cards have no resting elevation.

### Inputs / Fields

Native outlined TextInputLayout and TextInputEditText provide floating labels and platform focus/error behavior. Editable fields have a minimum height (56dp), regular Manrope and a consistent section gap. Password fields expose the native visibility toggle. Multiline draft text expands its lines rather than being forced into a compact capsule.

### Navigation

Four labeled destinations remain stable: Dictation, History, Connections and Settings. Material bottom navigation becomes a Material rail at the observed width threshold. Navigation text uses Manrope weight (600), tonal selection indicators and stroke icons. Native selection, focus and system Back behavior remain intact.

### Recorder

A charcoal card centers a short title, supporting copy, waveform and full-width light action. The microphone becomes a stop icon during recording; labels describe ready, recording and processing states. Uppercase status text is visible only during busy/error states and communicates operational state; it is not a decorative eyebrow. A cancel action appears while work is active. The waveform animates only with an active input level and respects Android's animator-duration setting.

### Voice keyboard

A compact toolbar places writing mode and output language before settings, contextual insertion/cancel actions and the filled microphone at the top right. Below it sit conditional status, an optional local suggestion row and familiar QWERTY rows. Keycaps use surface-variant fill with native ripple feedback; unavailable letter keys reduce opacity. Offline suggestions and reversible autocorrect are independent settings. Recording-only mapped letters change output language; ordinary typing retains letter behavior. Password fields visibly disable voice and suggestions. Insertion reports completion without submitting the destination editor.

## Do's and Don'ts

### Do:
- **Do** preserve the monochrome Manrope identity through native Android components.
- **Do** resolve theme-sensitive controls through semantic resource colors and preserve the recorder's stable contrast.
- **Do** let mode chips wrap and content scroll at larger text sizes and constrained heights.
- **Do** retain native focus, ripple, disabled states, content descriptions and system insets.
- **Do** distinguish ordinary typing, recording, processing and insertion with explicit visible state.

### Don't:
- **Don't** use the error color as an everyday brand accent.
- **Don't** add decorative card shadows or replace stroke icons with text glyphs.
- **Don't** copy the capsule silhouette onto individual QWERTY letter keys.
- **Don't** turn operational recorder status into decorative uppercase section kickers.
- **Don't** treat the external integration-test editor above the keyboard as Sotto interface design.

No craft-floor defect is canonized. Test-provider names, external editor styling and screenshot-only content are evidence fixtures, not reusable product tokens.
