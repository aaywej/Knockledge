---
name: Knockledge
description: Tactile retroactive audio recording and anchor capture system for Android with Material 3 integration
colors:
  primary: "#b8913f"
  on-primary: "#1b1833"
  primary-container: "#3f3010"
  on-primary-container: "#f2e8d0"
  primary-glow: "#d4af5a"
  secondary: "#f2e8d0"
  on-secondary: "#1b1833"
  secondary-container: "#2b274e"
  on-secondary-container: "#f2e8d0"
  tertiary: "#2de2d0"
  on-tertiary: "#003733"
  surface: "#14122b"
  on-surface: "#f2e8d0"
  surface-variant: "#1e1b3a"
  on-surface-variant: "#b9b09a"
  surface-container-low: "#181632"
  surface-container: "#1e1b3a"
  surface-container-high: "#252146"
  outline: "#b8913f"
  outline-variant: "rgba(184, 145, 63, 0.4)"
  error: "#ff5449"
  on-error: "#690005"
  bg-midnight: "#14122b"
  surface-indigo: "#1e1b3a"
  card-parchment: "#f2e8d0"
  text-ink: "#1b1833"
  text-cream: "#f2e8d0"
  text-muted: "#b9b09a"
  waveform-teal: "#2de2d0"
  waveform-pink: "#ff3d9a"
typography:
  display:
    fontFamily: "KiwiSoda, sans-serif"
    fontSize: "32px"
    fontWeight: 500
    lineHeight: 1.2
  headline:
    fontFamily: "KiwiSoda, sans-serif"
    fontSize: "24px"
    fontWeight: 500
    lineHeight: 1.25
  title:
    fontFamily: "KiwiSoda, sans-serif"
    fontSize: "18px"
    fontWeight: 500
    lineHeight: 1.3
  body:
    fontFamily: "VCR OSD Mono, monospace"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.4
  label:
    fontFamily: "VCR OSD Mono, monospace"
    fontSize: "12px"
    fontWeight: 400
    letterSpacing: "0.05em"
rounded:
  sm: "2px"
  md: "4px"
  lg: "12px"
  full: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.md}"
    padding: "12px 16px"
    height: "48px"
  button-primary-hover:
    backgroundColor: "{colors.primary-glow}"
  button-outline:
    backgroundColor: "{colors.surface-variant}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.md}"
    padding: "12px 16px"
    height: "48px"
  card-anchor:
    backgroundColor: "{colors.secondary}"
    textColor: "{colors.on-secondary}"
    rounded: "{rounded.md}"
    padding: "16px"
  card-surface:
    backgroundColor: "{colors.surface-container}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.md}"
    padding: "16px"
---

# Design System: Knockledge

## Overview

**Creative North Star: "The Obsidian & Brass Terminal"**

Knockledge operates as a physical instrumentation deck for audio preservation. The visual world combines deep obsidian midnight foundations (`#14122b`) with the mechanical authority of burnished antique brass (`#b8913f`) and tactile parchment cream (`#f2e8d0`). Material Design 3 (M3) governs architecture, semantic roles, navigation, and accessibility. Brand identity expresses through custom M3 color schemes, typography, and shape overrides.

The interface pairs Android platform conventions with analog instrument design. Android Material 3 components map directly to the retro terminal aesthetic. Controls enforce 48dp minimum interaction targets, edge-to-edge system bar insets, and predictive back gestures.

**Key Characteristics:**
- **Material 3 Semantic Architecture:** Complete mapping to official Android M3 color, typography, shape, and elevation roles.
- **Atmospheric Obsidian Substrate:** Deep midnight violet ground illuminated by diffuse brass gradients.
- **Physical Anchor Cards:** High-contrast vintage parchment surfaces housing captured spoken notes.
- **Dual Typographic Personality:** Friendly KiwiSoda display titles paired with precise VCR OSD Monospace telemetry.
- **Hardware-Grade Tactility:** Solid 1px borders, 4px micro-radii, and 48dp minimum interaction targets.

## Colors

The color system maps the Obsidian & Brass aesthetic into official Material 3 `ColorScheme` roles.

### Primary Roles
- **Primary / Antique Brass** (`#b8913f`): Primary button fill, active tab indicators, and key interactive outlines.
- **On Primary / Text Ink** (`#1b1833`): Dark text and icons displayed on primary brass surfaces.
- **Primary Container** (`#3f3010`): Low-intensity brass container fill.
- **On Primary Container** (`#f2e8d0`): High-contrast cream text on brass container surfaces.
- **Primary Glow / Accent Gold** (`#d4af5a`): Active states, slider tracks, telemetry headers, and glowing halos.

### Secondary Roles
- **Secondary / Parchment Cream** (`#f2e8d0`): High-contrast card background for captured audio notes.
- **On Secondary / Text Ink** (`#1b1833`): High-legibility dark ink used for note titles, summaries, and transcripts.
- **Secondary Container** (`#2b274e`): Muted purple container for secondary elevated elements.
- **On Secondary Container** (`#f2e8d0`): Cream text for secondary container content.

### Tertiary Roles
- **Tertiary / Waveform Teal** (`#2de2d0`): Audio frequency pulse visualization.
- **On Tertiary** (`#003733`): High-contrast icon color on teal markers.
- **Waveform Pink** (`#ff3d9a`): Harmonic spike accents within dynamic audio visualizers.

### Surface & Neutral Roles
- **Surface / Obsidian Midnight** (`#14122b`): Baseline application canvas and background for all primary screens.
- **On Surface / Text Cream** (`#f2e8d0`): High-contrast text on dark backgrounds.
- **Surface Variant / Surface Indigo** (`#1e1b3a`): Primary card and container fill for settings and dialogue panels.
- **On Surface Variant / Muted Sand** (`#b9b09a`): Low-emphasis metadata, parameter labels, and secondary counters.
- **Outline** (`#b8913f`): Primary component borders and active dividing strokes.
- **Outline Variant** (`rgba(184, 145, 63, 0.4)`): Passive container borders and subtle dividers.
- **Error** (`#ff5449`): Validation errors, network failures, and destructive deletion dialogues.
- **On Error** (`#690005`): Text on error surfaces.

### Named Rules
**The Material Semantic Rule.** All Compose UI code references `MaterialTheme.colorScheme` tokens, never raw hex values.
**The Telemetry Rarity Rule.** Electric teal and pink appear only within active audio frequency waveforms. They never decorate buttons, icons, or navigation controls.
**The Ink on Parchment Rule.** Light parchment cards (`#f2e8d0`) must always carry deep dark ink (`#1b1833`), never white or light gold text.

## Typography

The type system pairs friendly retro-chunky headings with laser-precise hardware monospace data. Both fonts reside in the native Android resource directory `androidApp/src/main/res/font/`.

### 1. KiwiSoda

- **Asset Path:** [androidApp/src/main/res/font/kiwi_soda.ttf](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/res/font/kiwi_soda.ttf)
- **Classification:** Retro-Chunky Pixel Display
- **Supported Weights:** Normal / Medium (500)
- **Fallback Stacks:** `sans-serif-condensed`, `sans-serif`
- **Design Intent:** KiwiSoda delivers a warm, analog feel for headlines without digital coldness. Its rounded geometric construction feels human and approachable while grounding the hardware aesthetic.
- **Application Scope:** Screen hero titles, modal headers, card titles, and primary button labels.

### 2. VCR OSD Mono

- **Asset Path:** [androidApp/src/main/res/font/vcr_osd_mono.ttf](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/res/font/vcr_osd_mono.ttf)
- **Classification:** CRT / On-Screen Display Monospace
- **Supported Weights:** Regular (400)
- **Fallback Stacks:** `monospace`
- **Design Intent:** VCR OSD Mono mimics classic video recording equipment and telemetry instruments. Fixed character widths ensure numeric stability so timers, countdowns, and duration meters do not jitter during updates.
- **Application Scope:** Recording timers, session clocks, buffer bounds, status pills, transcripts, and settings descriptions.

### Material 3 Scale Mapping

| Role | Font Family | Size | Line Height | Weight | Usage |
|---|---|---|---|---|---|
| **Display Large** | KiwiSoda | 32sp | 40sp | 500 | Hero titles ("Your Anchors", "Settings") with gold glow |
| **Headline Medium** | KiwiSoda | 24sp | 32sp | 500 | Modal titles and prominent section headers |
| **Title Medium** | KiwiSoda | 18sp | 24sp | 500 | Anchor card titles and dialog headings |
| **Body Large** | VCR OSD Mono | 16sp | 24sp | 400 | Long-form transcripts and key summary paragraphs |
| **Body Medium** | VCR OSD Mono | 14sp | 20sp | 400 | Note summaries, snippets, and setting descriptions |
| **Label Large** | KiwiSoda | 16sp | 20sp | 500 | Primary button labels and prominent actions |
| **Label Medium** | VCR OSD Mono | 12sp | 16sp | 400 | Telemetry counters, session timers, and buffer bounds |

### Kotlin Jetpack Compose Definition

```kotlin
// androidApp/src/main/kotlin/group/two/knockledge/ui/theme/Type.kt
val KiwiSoda = FontFamily(Font(R.font.kiwi_soda, FontWeight.Normal))
val VcrOsdMono = FontFamily(Font(R.font.vcr_osd_mono, FontWeight.Normal))

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = KiwiSoda,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = KiwiSoda,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = KiwiSoda,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = KiwiSoda,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = KiwiSoda,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = KiwiSoda,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = VcrOsdMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = VcrOsdMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = VcrOsdMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
```

### Named Rules
**The Dual Identity Rule.** Section headers and titles always use KiwiSoda. Timestamps, timers, statuses, and data counters always use VCR OSD Mono.
**The Numeric Stability Rule.** Real-time numeric counters and timers must use VCR OSD Mono to prevent layout jitter from proportional glyph widths.
**The System Scale Rule.** All text units use sp dimensions to respect user system accessibility scale settings.

## Layout

Layout follows Material 3 responsive guidelines adapted for Android hardware.

- **Baseline Grid:** 8dp structural spacing grid with 4dp sub-grid for micro-adjustments.
- **Screen Margins:** 16dp outer screen margin on horizontal edges.
- **Touch Target Accessibility:** Every interactive control measures at least 48 × 48 dp with 8dp separation.
- **Window Insets:** Full edge-to-edge support using Compose `WindowInsets.safeDrawing`.
- **Predictive Back Navigation:** All screens and dialogues integrate with the Android predictive back gesture.

## Elevation & Depth

Knockledge maps Material 3 surface elevation to tonal containers augmented by ambient brass luminescence.

### Material 3 Surface Tonal Levels
- **Surface Container Lowest** (`#100e22`): Recessed gesture test area.
- **Surface Container Low** (`#181632`): Subtle panel backgrounds.
- **Surface Container** (`#1e1b3a`): Standard card elevation for settings, dialogs, and top app bars.
- **Surface Container High** (`#252146`): Active dropdown menus and dialogue surfaces.
- **Surface Container Highest** (`#302b5a`): Hover and focus highlights.

### Luminescence Vocabulary
- **Ambient Glow Low** (`box-shadow: 0px 2px 8px 0px rgba(212, 175, 90, 0.12)`): Resting elevation for Parchment Anchor Cards.
- **Ambient Glow Medium** (`box-shadow: 0px 4px 12px 0px rgba(212, 175, 90, 0.25)`): Active state for Primary Antique Brass Buttons.
- **Halo Radial Glow** (`radial-gradient(ellipse, rgba(212, 175, 90, 0.10) 0%, rgba(212, 175, 90, 0) 70%)`): Ambient background illumination.

### Named Rules
**The Luminescent Depth Rule.** Elevation emerges from glowing brass ambient light against obsidian midnight, not from black drop shadows.

## Shapes

Knockledge overrides Material 3 `Shapes` to establish precision hardware geometry.

### Material 3 Shape Tokens
- **Shape Extra Small (`2.dp`):** Waveform audio bars and slider track ends.
- **Shape Small (`4.dp`):** Primary buttons, cards, text fields, and dialogue surfaces.
- **Shape Medium (`4.dp`):** Standard container panels and note cards.
- **Shape Large (`12.dp`):** Microphone capsule indicator.
- **Shape Full (`CircleShape`):** Status pills (`READY`), avatars, and toggle switch thumbs.

## Components

All user interface controls utilize official Material 3 Compose composables styled with design system tokens.

### Buttons
- **FilledButton (Primary):** Styled with `ButtonDefaults.buttonColors(containerColor = AntiqueBrass, contentColor = TextInk)`. Minimum 48dp height and 4dp corner radius.
- **OutlinedButton (Secondary):** Styled with `BorderStroke(1.dp, AntiqueBrass)`, Surface Indigo container, and Text Cream label.
- **Quick Play Button:** Compact 64 × 48 dp tactile button inside `AnchorCard`.

### Cards & Containers
- **Card (Anchor Card):** High-contrast `CardDefaults.cardColors(containerColor = ParchmentCream, contentColor = TextInk)` with 1px border.
- **ElevatedCard (Settings Card):** Surface Indigo container with 1px muted brass border (`OutlineVariant`).

### Selection Controls
- **Switch:** Custom M3 `Switch` with 48dp hit box. Antique Brass thumb with Surface Indigo track.
- **Slider:** Custom M3 `Slider` with Antique Brass track, 20dp thumb, and glowing halo feedback.

### Waveform Visualizer
- **Custom Visualizer:** Multi-bar frequency meter built with Teal (`#2de2d0`), Pink (`#ff3d9a`), and Parchment Cream (`#f2e8d0`).

## Do's and Don'ts

### Do:
- **Do** map all colors and typography through `MaterialTheme.colorScheme` and `MaterialTheme.typography`.
- **Do** maintain a strict 48 × 48 dp minimum touch target for all interactive elements.
- **Do** render saved audio notes on Parchment Cream cards (`#f2e8d0`) with dark ink text (`#1b1833`).
- **Do** use Material 3 tonal elevation and brass luminescence instead of black drop shadows.
- **Do** preserve 16dp screen edge margins on mobile layouts.

### Don't:
- **Don't** use default Material 3 purple or pastel tint colors.
- **Don't** use Teal (`#2de2d0`) or Pink (`#ff3d9a`) outside audio frequency waveforms.
- **Don't** place light cream or gold text directly on Parchment Cream cards.
- **Don't** use rounded corners larger than 4px on cards and buttons.
- **Don't** hardcode raw hex values in Compose UI composables.
