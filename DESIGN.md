---
name: Class Recorder
description: Calm pocket-sized Mist Glass lecture recorder and on-device MCP workbench for Android.
colors:
  mist-primary-light: "#4B67B4"
  mist-primary-dark: "#97B1F0"
  daiqing-seed: "#236185"
  dusk-seed: "#726291"
  turquoise-seed: "#34766F"
  amber-seed: "#8D6636"
  graphite-seed: "#2C2E32"
  brick-rose-light: "#A54743"
  brick-rose-dark: "#E69089"
  celadon-ok-light: "#337250"
  celadon-ok-dark: "#8BCCA6"
  ochre-warn-light: "#936123"
  ochre-warn-dark: "#E6BD7C"
  morning-mist-top: "#EBF1FE"
  morning-mist-bottom: "#F6F7FC"
  morning-mist-ink: "#1E232E"
  morning-mist-secondary: "#4F5562"
  morning-mist-tertiary: "#646974"
  morning-mist-code-well: "#EDF0F6"
  slate-night-top: "#1D222C"
  slate-night-bottom: "#1B1C23"
  slate-night-paper: "#E6E9F0"
  slate-night-secondary: "#B6BBC5"
  slate-night-tertiary: "#9A9EA9"
  slate-night-smoke: "#292E39"
  slate-night-code-well: "#14171E"
typography:
  display:
    fontFamily: "sans-serif"
    fontSize: "48px"
    fontWeight: 300
    lineHeight: 1.167
    letterSpacing: "-1px"
    fontFeature: "tnum"
  headline:
    fontFamily: "sans-serif"
    fontSize: "26px"
    fontWeight: 600
    lineHeight: 1.308
  title:
    fontFamily: "sans-serif"
    fontSize: "16px"
    fontWeight: 600
    lineHeight: 1.438
  body:
    fontFamily: "sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.571
  label:
    fontFamily: "sans-serif"
    fontSize: "12px"
    fontWeight: 600
    lineHeight: 1.333
    letterSpacing: "0.2px"
  mono:
    fontFamily: "monospace"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.5
    fontFeature: "tnum"
rounded:
  xs: "6px"
  sm: "10px"
  md: "14px"
  lg: "20px"
  xl: "28px"
  pill: "9999px"
spacing:
  xxs: "2px"
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "20px"
  xxl: "24px"
  xxxl: "32px"
components:
  button-primary:
    backgroundColor: "{colors.mist-primary-dark}"
    textColor: "{colors.slate-night-bottom}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "10px 20px"
    height: "48px"
  button-record-trigger:
    backgroundColor: "{colors.brick-rose-dark}"
    textColor: "{colors.slate-night-bottom}"
    rounded: "{rounded.pill}"
    size: "52px"
  button-secondary-glass:
    backgroundColor: "{colors.slate-night-smoke}"
    textColor: "{colors.slate-night-paper}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "10px 18px"
    height: "48px"
  chip-filter:
    backgroundColor: "{colors.slate-night-smoke}"
    textColor: "{colors.slate-night-secondary}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "6px 13px"
    height: "34px"
  chip-filter-selected:
    backgroundColor: "{colors.slate-night-paper}"
    textColor: "{colors.slate-night-bottom}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "6px 13px"
    height: "34px"
  card-glass:
    backgroundColor: "{colors.slate-night-smoke}"
    textColor: "{colors.slate-night-paper}"
    rounded: "{rounded.lg}"
    padding: "16px"
  input-search-capsule:
    backgroundColor: "{colors.slate-night-smoke}"
    textColor: "{colors.slate-night-paper}"
    typography: "{typography.body}"
    rounded: "{rounded.pill}"
    padding: "10px 14px"
    height: "48px"
  code-well:
    backgroundColor: "{colors.slate-night-code-well}"
    textColor: "{colors.slate-night-paper}"
    typography: "{typography.mono}"
    rounded: "{rounded.sm}"
    padding: "8px 12px"
---

# Design System: Class Recorder

## Overview

**Creative North Star: "The Pocket Mist Workbench"**

Class Recorder adopts DSH Go's Mist Glass design language to turn classroom lecture capture and on-device AI agent connectivity into a quiet, tactile pocket workbench. Instead of stacking opaque Material app bars, heavy bottom navigation slabs, or saturated neon timetable blocks, the interface floats translucent capsules and softly rimmed cards over a stationary full-bleed atmospheric gradient. Students opening the app seconds before a lecture begins see their auto-matched class, room, MCP server endpoint, and record trigger immediately, with every secondary control receding into mist-tinted neutral glass.

Both Light ("Morning Mist") and Dark ("Slate Night") themes derive their surfaces and neutral inks in perceptual OKLCH space from six low-saturation presets (`Mist`, `Daiqing`, `Dusk`, `Turquoise`, `Amber`, `Graphite`) plus Android 12+ `Follow wallpaper`. Roughly 70% of every viewport belongs to the gradient backdrop and translucent glass tiers, 25% to crisp ink or paper typography and `0.5dp` hairlines, and 5% or less to active focal accents.

**Key Characteristics:**
- Stationary full-bleed vertical gradient backdrop (`DshBackdrop`) with two soft corner radial glows shared across root pager tabs.
- Floating Chrome-tier top `SegmentedPill` (`Recorder · Schedule · Recordings`) and bottom Composer dock with zero full-width bar slabs.
- Four-tier Mist Glass hierarchy (`Chrome`, `Sheet`, `Card`, `Pill`) combining outer-only clipped shadows, `0.5dp` inner hairlines, and top-45% vertical specular rims.
- Contrast-guarded OKLCH palette derivation across six presets, live wallpaper color, and deterministic FNV-1a course identity hues.
- Two-weight structural CJK/Latin typography (`400` / `600`) paired with `300 Light` `48sp` tabular clocks and recessed monospace `CodeWell` blocks.

## Colors

The palette balances cool atmospheric mist and slate grounds with low-chroma (`C <= 0.13`) OKLCH accents and restrained semantic signals that never shout over lecture metadata.

### Primary
- **Periwinkle Mist** (`mist-primary-light` / `mist-primary-dark`): Default interactive accent used for the active weekday pill on Schedule, playback transport Play/Pause, selected Appearance rings, and active switches. Five sibling OKLCH presets (**Daiqing** `daiqing-seed`, **Dusk** `dusk-seed`, **Turquoise** `turquoise-seed`, **Amber** `amber-seed`, and **Graphite** `graphite-seed`) retint both primary accents and neutral undertones when chosen in Settings.

### Secondary
- **Brick Rose Capture** (`brick-rose-light` / `brick-rose-dark`): Reserved exclusively for live audio capture (`CaptureComposerDock` record trigger, live waveform bars/playhead, `Live` status chip, and destructive delete/discard confirmations).

### Tertiary
- **Celadon Active** (`celadon-ok-light` / `celadon-ok-dark`): Marks an in-session class (`In class` / `Now`) and an active on-device MCP server status dot.
- **Warm Ochre Pause** (`ochre-warn-light` / `ochre-warn-dark`): Signals a paused recording clock and `Paused` status chip without alarming the user.

### Neutral
- **Morning Mist Ground** (`morning-mist-top` to `morning-mist-bottom`): Cool daylight vertical wash in Light mode, paired with deep slate **Morning Mist Ink** (`morning-mist-ink`), **Secondary Ink** (`morning-mist-secondary`), **Tertiary Ink** (`morning-mist-tertiary`), and recessed **Light Code Well** (`morning-mist-code-well`).
- **Slate Night Ground** (`slate-night-top` to `slate-night-bottom`): Low-glare evening backdrop for dimmed lecture halls, paired with **Slate Paper** (`slate-night-paper`), **Secondary Paper** (`slate-night-secondary`), **Tertiary Paper** (`slate-night-tertiary`), frosted **Smoke Chrome** (`slate-night-smoke`), and recessed **Dark Code Well** (`slate-night-code-well`).

### Named Rules
**The Five-Percent Voice Rule.** Primary accent and Brick Rose never flood cards or toolbars; at rest, only the single primary action on screen (the `52dp` circular record trigger on Recorder, the `Now` class mic button on Schedule, or the `Play` capsule on Playback) carries a solid color fill.

**The OKLCH Contrast Guard Rule.** Every preset and wallpaper-derived seed is binary-searched along OKLCH lightness (`L`) with chroma clamped (`C <= 0.13`, `AppAccent.MAX_CHROMA`) so `primary` vs `onPrimary` achieves `>= 4.5:1` and `primary` vs `surfaceContainerHighest` achieves `>= 3.0:1` in both Light and Dark modes.

**The Polarity-Aware Course Dot Rule.** Deterministic 8-slot FNV-1a course indicators (`DshColors.courseDot`) render at `L = 0.79` on dark surfaces and `L = 0.45` on light surfaces, inverting automatically on ink-selected filter chips so `4dp–9dp` dots and timetable pips always maintain `>= 3.5:1` non-text contrast.

## Typography

**Display Font:** Platform Sans (`FontFamily.Default` — Roboto / Noto Sans CJK)
**Body Font:** Platform Sans (`FontFamily.Default` — Roboto / Noto Sans CJK)
**Label/Mono Font:** Platform Monospace (`FontFamily.Monospace`) for endpoints, ADB commands, seek readouts, and filenames

**Character:** Quiet, native-first CJK and Latin pairing that avoids synthetic weight steps so Traditional Chinese course titles (`國立臺中科技大學`, `民法`, `大學之道`) and Latin numerals sit on an even vertical baseline (`includeFontPadding = false`, centered line-height trim).

### Hierarchy
- **Display Timer** (`300 Light`, `48sp`, `56sp` line-height, `-1sp` tracking, `tnum`): Live and idle recording clock on the Recorder stage.
- **Headline Large / Medium / Small** (`600 SemiBold`, `26sp` / `22sp` / `20sp`): Scroll-fading `AppPage` titles (`Settings`), school header on Schedule, and `PageHero` course titles on Playback.
- **Title Large / Medium / Small** (`600 SemiBold`, `18sp` / `16sp` / `14sp`): `AppSheet` headers, `OverviewCard` and timetable course names, compact rising bar titles, and Composer dock summaries.
- **Body Large / Medium / Small** (`400 Normal`, `16sp` / `14sp` / `12sp`): Lecture notes, row subtitles, empty-state guidance, and settings footnotes.
- **Label Large / Medium / Small** (`600 SemiBold` at `14sp` / `12sp`, `400 Normal` at `11sp`): Sentence-case section labels, `SegmentedPill` tabs, `CapsuleChoiceChip` filters, and `StatusChip` pills.
- **Mono / Mono Small** (`400 Normal`, `13sp` / `12sp`, `tnum`): MCP `http://<ip>:8765/mcp` URLs, `adb forward` commands, `.m4a` filename previews, and playback seek timestamps.

### Named Rules
**The Two-Weight Discipline Rule.** Structural UI text uses only `400 Normal` and `600 SemiBold` (plus `300 Light` solely on large display numerals) because CJK system fonts synthesize muddy intermediate weights.

**The Tabular Clock Rule.** Every clock, duration, period range (`13:25–14:15`), weekday date numeral, count badge, and seek label enables OpenType tabular figures (`tnum`) and normalizes range separators to an en-dash (`–`).

**The No-Eyebrow Rule.** Never place tracked uppercase kicker or eyebrow labels above headings or cards; section headers use quiet sentence-case `labelLarge` (`14sp`) in `textTertiary`.

## Layout

The app runs edge-to-edge (`enableEdgeToEdge()`) over a single stationary `DshBackdrop`. Rather than dividing the viewport with horizontal app-bar slabs, floating controls hover `12dp` (`AppSpace.floatInset`) inside the window edges while scrollable `LazyColumn` content flows beneath feathered `BackdropVeil` bands (`12dp–20dp` eased vertical alpha masks) at both the status bar and the system navigation handle.

- **Reading Column & Gutters:** Content centers within a `720dp` maximum reading column (`AppSpace.readingWidth`) on tablets and foldables, with `16dp` horizontal page gutters (`AppSpace.gutter`) on phone viewports and `20dp` horizontal gutters (`AppSpace.sheetGutter`) inside bottom sheets.
- **Vertical Rhythm:** `12dp` between Recorder workspace cards, `10dp` between Schedule period cards, and `24dp` between Settings section groups.
- **Font-Scale Resilience (`1.3x`):** Interactive rows use `defaultMinSize` (`52dp` single-line, `64dp` two-line) rather than fixed heights; `SegmentedPill` measures segment labels via `TextMeasurer` to step down uniformly (`13.5sp` to `10.5sp`) before any label can truncate, and `OverviewCard` renders metadata atoms inside a `FlowRow` so CJK instructor names never split mid-word.

## Elevation & Depth

Depth is expressed through four Mist Glass tiers rather than Material tonal elevation steps or heavy drop shadows. Blur is reserved strictly for floating chrome and modal sheets that pass over moving content, while scrolling list cards remain zero-blur translucent surfaces to preserve 60/120fps frame pacing.

### Shadow Vocabulary
- **Chrome Ambient & Key** (`blurRadius: 24dp, offsetY: 8dp` + `blurRadius: 3dp, offsetY: 1dp`): Clipped strictly outside the shape perimeter on floating `SegmentedPill`, `GlassIconButton`, `CaptureComposerDock`, `SelectionDock`, and `Snackbar` surfaces.
- **Sheet Lift** (`blurRadius: 32dp, offsetY: 8dp`): Soft upward separation on `28dp` `AppSheet` modals paired with a dimmed `scrim`.
- **Card & Pill Rest** (`none`): Scrolling cards and inline pills carry zero drop shadow, separating from `DshBackdrop` via translucent fill (`5.5%–6.5%` white in Dark; `56%–64%` white in Light), a `0.5dp` inner hairline, and a vertical top-to-45% specular rim highlight.

### Named Rules
**The Exterior-Only Shadow Rule.** Every `GlassSurface` shadow path is clipped with `PathOperation.Difference` (`outerBounds - innerShape`) before drawing so dark shadow blur never muddies the translucent glass interior.

**The Blur-Where-Needed Rule.** `Chrome` (`18dp` `GraphicsLayer` `BlurEffect`) and `Sheet` (`24dp` Android 12+ cross-window blur) refract underlying pixels when supported and automatically fall back to high-opacity `fallbackFill` on Android 10–11 or when system Reduce Motion (`ANIMATOR_DURATION_SCALE == 0`) is active; `Card` and `Pill` never blur (`blurSigma = 0`).

## Shapes

The geometry uses a five-stop rounded-rectangle ladder (`6dp`, `10dp`, `14dp`, `20dp`, `28dp`) paired with full stadium capsules (`CircleShape`):
- **Stadium / Circle (`9999px`):** `SegmentedPill` tracks and thumbs, `GlassIconButton` (`44dp` visual disc inside a `>= 48dp` touch box), `SearchCapsule`, `CapsuleChoiceChip`, `StatusChip`, and primary/secondary action buttons.
- **Extra-Large (`28dp`):** Floating `CaptureComposerDock`, `SelectionDock`, and top-only `AppSheet` / `DshConfirmSheet` modals (`RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`).
- **Large (`20dp`):** `OverviewCard`, `ListPageCard`, `SheetGroup`, schedule period cards, and waveform stage cards.
- **Medium (`14dp`):** Selected weekday cell in `WeekStripCard`, `DshTextField` input wells, miniature theme-mode phone previews, and expanded `GlassPill` panels.
- **Small (`10dp`):** Recessed monospace `CodeWell` blocks (`0.75dp` border).

## Components

### Buttons
- **Shape:** Full stadium (`9999px`) for labeled actions (`>= 48dp` touch height; `56dp` on Playback transport) and circles (`44dp` visual / `48dp` hit target, or `52dp` on Composer/Playback) for icon triggers.
- **Primary (`DshPrimaryButton`):** Solid `primary` fill with `onPrimary` label/icon (`danger` / `onDanger` when destructive or finishing a live recording). Scales to `0.97` on press via a non-bouncy spring (`DshMotion.pressSpring`).
- **Secondary (`DshSecondaryButton` & `GlassIconButton`):** `Pill`-tier or `Chrome`-tier Mist Glass with `0.5dp` hairline and specular rim, keeping secondary controls quiet beside the single primary action.

### Chips
- **Filter Chips (`CapsuleChoiceChip`):** Borderless stadium capsules (`34dp` visual height inside `48dp` touch target). Unselected chips use `7%` white (Dark) / `5.5%` ink (Light); selected chips invert to crisp ink fill (`onSurface` background, `surface` label) rather than accent flood, with polarity-aware `8dp` course dots.
- **Status Chips (`StatusChip`):** Compact non-interactive or tappable stadium badges (`26dp` visual height) pairing a `6dp` `StatusDot` (gently pulsing on `Busy` or `Error` unless Reduce Motion is enabled) with a `12sp` tabular label.

### Cards / Containers
- **Hub Card (`OverviewCard`):** `20dp` `Card`-tier glass with a `52dp` `OverviewGlyph` circle, inline `titleBadge`, `FlowRow` metadata atoms, and an optional `0.5dp` hairline + `48dp` status footer (`OverviewFooterRow`) aligning its `8dp` `StatusDot` directly beneath the `52dp` glyph center.
- **Grouped Card (`ListPageCard` & `SheetGroup`):** `20dp` `Card`-tier glass housing touching `ListPageRow` items (`52dp` single-line / `64dp` two-line) separated by `0.5dp` inset hairlines, topped by a quiet sentence-case `labelLarge` group header.
- **Code Well (`CodeWell`):** `10dp` recessed container (`codeWellBg` + `0.75dp` `codeWellBorder`) with optional `codeWellHeader` label bar and `48dp` one-tap clipboard copy row for MCP URLs, ADB commands, and `.m4a` filenames.

### Inputs / Fields
- **Search (`SearchCapsule`):** Borderless stadium capsule (`48dp` height) with leading search icon, single-line input, and a `48dp` circular clear button when non-empty.
- **Form Input (`DshTextField`):** `14dp` recessed well (`6.5%` white in Dark / `5%` ink in Light + `0.5dp` `outlineVariant` border) supporting sans or monospace text and `>= 48dp` height.

### Navigation
- **Root Chrome Bar:** Floating `56dp` row (`12dp` side inset) combining a 3-slot `Chrome`-glass `SegmentedPill` (`Recorder`, `Schedule`, `Recordings`)—whose sliding thumb tracks `HorizontalPager` swipe offset continuously and supports horizontal finger-drag with haptic ticks—and a separate `44dp` circular `GlassIconButton` (`Settings`) carrying a live MCP status dot.
- **Secondary Pages (`AppPage`):** Full-screen push overlays with interactive `PredictiveBackHandler` scaling, pointer-event and accessibility-traversal isolation from background tabs, a scroll-fading `headlineLarge` title or `PageHero` (with `HeroStat` tabular figures), and a compact `titleMedium` bar title rising over a top `BackdropVeil`.

### Capture Composer Dock
- **Idle State:** Floating `28dp` `Chrome`-glass dock (`12dp` inset above navigation bar) displaying a tappable course target summary on the left (`9dp` course dot + `Record · <Course>` + `<Course>/<filename>.m4a` live preview) and a `52dp` circular Brick Rose microphone button on the right.
- **Live / Paused State:** Morphs smoothly inside the same `Chrome` silhouette into a 3-control bar: `52dp` circular `Discard` (`DshConfirmSheet` guarded), flexible `Pause` / `Resume` capsule, and Brick Rose `Finish` capsule.

## Do's and Don'ts

### Do:
- **Do** wrap all secondary screens in `AppPage` and all modal interactions or destructive confirmations in `AppSheet` / `DshConfirmSheet` (`28dp` top corners, `Sheet`-tier glass).
- **Do** keep `GlassSurface` child text legible across Light and Dark modes by relying on `LocalContentColor` (`colorScheme.onSurface`) and `DshColors.textTertiary` (`>= 4.5:1` for text, `>= 3.0:1` for waveform bars and course dots).
- **Do** decode real audio envelopes (`AudioWaveformDecoder`) for `ScrubWaveformCanvas` and show an honest flat baseline when audio is silent or unavailable.
- **Do** maintain `>= 48dp × 48dp` touch targets on every interactive control and verify layouts at `1.3x` system font scale with feathered top and bottom `BackdropVeil`s.

### Don't:
- **Don't** introduce opaque `TopAppBar`, `NavigationBar`, or Material `AlertDialog` slabs that break the full-bleed `DshBackdrop` and Mist Glass material system.
- **Don't** place uppercase tracked eyebrows or kickers above headings, card titles, or hero blocks.
- **Don't** parse raw neon timetable hex strings onto card surfaces or use low-contrast container pastels (`identityBg`) for small `4dp–9dp` dots; map course names through `DshColors.identityPair` for large badges and `DshColors.courseDot` for dots and pips.
- **Don't** apply real-time blur to scrolling `Card` or `Pill` list items, or run looping/spring animations when `LocalReducedMotion.current` is true.
