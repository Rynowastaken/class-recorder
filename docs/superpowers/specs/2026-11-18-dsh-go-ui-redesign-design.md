# Class Recorder — DSH Go ("Mist Glass") UI Redesign Spec

**Date:** 2026-11-18  
**Project Root:** `/home/ryno/project/class-recorder`  
**Android Module:** `/home/ryno/project/class-recorder/android-app`  
**Reference System:** DSH Go (`dsh_palette.dart`, `dsh_surfaces.dart`, `glass.dart`, `glass_pill.dart`, `dsh_typography.dart`, `dsh_shapes.dart`, `accent.dart`, `semantic_colors.dart`, `page_frame.dart`, `segmented_pill.dart`, `overview_card.dart`, `list_page_row.dart`, `app_sheet.dart`)

---

## 1. Goals & Scope

Replace Class Recorder's hybrid NUTC Portal / Dusk Compose UI with a faithful Jetpack Compose implementation of **DSH Go's "Mist Glass" design system** while preserving 100% of existing recording, schedule sync, `.zip` export, and MCP server functionality:

1. **Full DSH Go Theme Engine (`ui/theme/`)**: Handcrafted low-saturation OKLCH color architecture supporting **Light** ("Morning Mist"), **Dark** ("Slate Night"), and **Follow System** modes, **6 DSH Go accent presets** (`Mist`, `Daiqing`, `Dusk`, `Turquoise`, `Amber`, `Graphite`) plus **Follow Wallpaper** (Android 12+ dynamic hue with OKLCH chroma cap and WCAG contrast guard), persisted across launches.
2. **4-Tier Glass & Backdrop System (`ui/components/`)**: Full-bleed `AppBackdrop` (vertical gradient + two large corner radial glows) + `BackdropVeil` scroll fades + `GlassSurface` (`chrome`, `sheet`, `card`, `pill` tiers with inset `0.5dp` hairline, top-to-45% vertical rim highlight, and even-odd outer-only shadows).
3. **Top Chrome Pill Shell + Composer-Style Recorder Dock**: Floating `56dp` top chrome bar (no opaque slab) housing a sliding `SegmentedPill` (`Recorder · Schedule · Recordings`) and a circular `GlassIconButton` (`⚙`), paired with a DSH Go `Composer`-style floating `chrome` bottom dock on `RecordScreen`.
4. **Complete Screen & Sheet Rebuild**: Rebuild `RecordScreen`, `ScheduleScreen`, `LibraryScreen`, `PlaybackScreen`, and `SettingsScreen` (replacing `SettingsSheet`) using DSH Go's `AppPage`, `PageHero`, `OverviewCard`, `SettingsSection`, `ListPageCard` / `ListPageRow`, `SearchCapsule`, `CapsuleChoiceChip`, `StatusChip`, `GlassPill`, `AppSheet`, and `EmptyView`. Every hand-written Kotlin file stays well under 1,000 lines.
5. **Android & Accessibility Conformance**: `>= 48×48dp` hit targets on all interactive controls, `sp` typography verified at `1.3x` font scale, WCAG AA contrast (`>= 4.5:1` text, `>= 3:1` controls) across all palettes in both Light and Dark, predictive `BackHandler` support, edge-to-edge status/navigation/cutout/IME insets, and `LocalReducedMotion` honoring Android's `ANIMATOR_DURATION_SCALE == 0`.

### Out of Scope / Unchanged
- `rust-core/` (`libclass_recorder_core.so` JNI methods, SQLite schema, schedule parser, `axum` MCP server).
- `RecordingService.kt` foreground `MediaRecorder` pipeline and Android 16 / Xiaomi HyperOS live-update & focus notification payloads.
- `PromotionGrant.kt` Shizuku notification promotion helper.

---

## 2. Architecture & File Map

```text
android-app/app/src/main/java/com/ryno/classrecorder/
├── MainActivity.kt                      # Edge-to-edge host, AppBackdrop, floating top SegmentedPill + Settings button, HorizontalPager, AppPage overlays
├── ui/
│   ├── AppearancePreferences.kt         # SharedPreferences store for ThemeMode (SYSTEM/LIGHT/DARK) & AccentSource (6 presets + WALLPAPER)
│   ├── MainViewModel.kt                 # Existing state/JNI bridge + AppearanceState flow & setters
│   ├── theme/
│   │   ├── Oklch.kt                     # sRGB <-> Linear sRGB <-> OKLab <-> OKLCH + retint() + WCAG contrastRatio() + guard()
│   │   ├── AppAccent.kt                 # 6 DSH Go presets (mist, daiqing, dusk, turquoise, amber, graphite) + derive() for wallpaper
│   │   ├── DshPalette.kt                # NeutralTones (baseHue 266°, hue-shifted per accent) + lightScheme() / darkScheme()
│   │   ├── DshSurfaces.kt               # GlassTier (Chrome, Sheet, Card, Pill), GlassStyle, DshSurfaces Light/Dark factories
│   │   ├── DshColors.kt                 # Semantic fixed tokens: 8 FNV-1a identity pairs, codeWell, success/warning/danger/info, textTertiary/textDisabled
│   │   ├── Type.kt                      # DshTypography (w300 display, w400 body, w600 titles/labels) + MonoTypography (tnum)
│   │   ├── Shape.kt                     # AppShape (6/10/14/20/28dp + pill) & AppSpace (gutter=16dp, sheetGutter=20dp, readingWidth=720dp)
│   │   ├── Motion.kt                    # DshMotion springs/tweens + LocalReducedMotion (reads ANIMATOR_DURATION_SCALE)
│   │   └── Theme.kt                     # ClassRecorderTheme(themeMode, accentId) wiring ColorScheme, DshSurfaces, DshColors, dynamic status/nav bars
│   ├── components/
│   │   ├── AppBackdrop.kt               # Full-bleed vertical gradient + top-right/bottom-left radial glows + BackdropVeil (feathered top/bottom scroll fades)
│   │   ├── GlassSurface.kt              # 4-tier glass box: fill/fallbackFill, 0.5dp inner hairline, 0..45% vertical rim, even-odd outer shadow, press scale
│   │   ├── GlassControls.kt             # GlassIconButton (44dp visual / 48dp hit), GlassPill (34dp stadium -> 14dp expanded card), StatusDot, StatusChip
│   │   ├── SegmentedPill.kt             # Draggable & pager-synced sliding capsule control + CapsuleChoiceChip + SearchCapsule
│   │   ├── AppPage.kt                   # Secondary page frame: floating back circle, scroll-fading large title / PageHero, rising bar title, 720dp max column
│   │   ├── AppSheet.kt                  # ModalBottomSheet wrapper in sheet-tier glass: 36x4dp SheetDragHandle, SheetHeader, SheetGroup
│   │   └── CardsAndRows.kt              # OverviewCard (+ OverviewGlyph, OverviewFooter), ListPageCard, ListPageRow, SettingsSection, SettingsTile, CodeWell, EmptyView
│   └── screens/
│       ├── RecordScreen.kt              # Class OverviewCard + MCP GlassPill/footer + quick course chips + live waveform/timer + floating Composer dock
│       ├── ScheduleScreen.kt            # School header + 7-day WeekStrip card + ListPageCard timetable rows + ScheduleSource AppSheet
│       ├── LibraryScreen.kt             # SearchCapsule + action circles + CapsuleChoiceChip filter + day-grouped ListPageCards + SelectionDock + Export/MCP/Notes sheets
│       ├── PlaybackScreen.kt            # AppPage + PageHero + waveform scrubber card + accent Play/Pause dock + SegmentedPill speed + Notes & CodeWell
│       ├── SettingsScreen.kt            # AppPage + MCP OverviewCard + Appearance (ThemeModePicker + PalettePicker + Wallpaper) + Schedule + File Naming
│       └── WaveformCanvas.kt            # Extracted live recording & playback scrubbable waveform canvases
```

Legacy files removed during implementation: `ui/theme/Color.kt`, `ui/theme/Backdrop.kt`, `ui/theme/Expressive.kt`, `ui/screens/ExpressiveScaffold.kt`, and `ui/screens/SettingsSheet.kt`.

---

## 3. Design Tokens & Visual Physics

### 3.1 Color Discipline (`70 / 25 / <= 5%`)
- **70% Neutral Ground**: `AppBackdrop` vertical gradient + translucent `GlassSurface` fills. Neither pure `#FFFFFF` nor pure `#000000` is used as a page background.
- **25% Typography & Hairlines**:
  - **Light ("Morning Mist")**: `mistTop = #EBF1FE`, `mistBottom = #F6F7FC`, `mist = #F0F4FD`, `ink = #1E232E` (`>= 13:1`), `inkMuted = #4F5562` (`>= 6.2:1`), `inkFaint = #646974` (`>= 4.6:1`), `ruleLight = #DCE0E8`.
  - **Dark ("Slate Night")**: `nightTop = #1D222C`, `nightBottom = #1B1C23`, `night = #1C1F28`, `paper = #E6E9F0` (`>= 10.5:1`), `paperMuted = #B6BBC5`, `paperFaint = #9A9EA9`, `ruleDark = #343841`, `smoke = #292E39`, `sheetDark = #232832`.
- **<= 5% Theme Accent (`primary`)**: Reserved strictly for primary action capsules, selected `ThemeModePicker` / `PalettePicker` rings, active switches, links, and playback scrub progress.
- **6 Presets + Wallpaper (`AppAccent`)**:
  - `mist` (`#4B67B4` light / `#97B1F0` dark, `tintHue = 266°`, `tintChroma = 1.0`)
  - `daiqing` (`#236185` / `#8CB9D7`, `238°`, `0.9`)
  - `dusk` (`#726291` / `#B8ABD2`, `295°`, `0.85`)
  - `turquoise` (`#34766F` / `#8DBEB7`, `190°`, `0.7`)
  - `amber` (`#8D6636` / `#CCAC87`, `75°`, `0.75`)
  - `graphite` (`#2C2E32` / `#E5E8ED`, `262°`, `0.25`)
  - `wallpaper`: Reads Android 12+ (`API 31+`) `android.R.color.system_accent1_500`, converts to OKLCH, caps chroma at `0.13`, shifts `NeutralTones` to the hue, and binary-searches lightness via `AppAccent.guard()` until `contrastRatio(accent, surfaceHighest) >= 3.0` and `contrastRatio(onAccent, accent) >= 4.5`. Falls back to `mist` below API 31.
- **Semantic Fixed Colors (`DshColors`)**:
  - **Recording & Destructive (`danger`)**: Low-saturation brick-rose (`#A54743` on `#FFE2E0` in Light; `#E69089` on `#542C29` in Dark). Never recolored by theme accent.
  - **Status**: `success` (`#337250` / `#8BCCA6`), `warning` (`#936123` / `#E6BD7C`), `info` (`#2971A7` / `#8CBDE8`).
  - **8 Identity Pairs**: Deterministic FNV-1a hash (`identityIndex(courseName, 8)`) selects `(identity[i], onIdentity[i])` for course dots, badges, and settings category icons so timetable colors remain calm and legible in both Light and Dark modes (replacing raw external schedule hexes).
  - **Recessed `codeWell`**: `#EDF0F6` (border `#DBDEE5`) in Light; `#14171E` (border `#2F333A`) in Dark. All monospace endpoints, CLI commands, templates, and filenames sit inside a `codeWell`.

### 3.2 4-Tier Glass Material (`DshSurfaces` + `GlassSurface`)
| Tier | Default Shape | Light Fill | Dark Fill | Rim (Top -> 45% Transparent) | Hairline (`0.5dp` inset) | Outer Shadow |
|---|---|---|---|---|---|---|
| `Chrome` | `28dp` / `Circle` / `Stadium` | `White @ 70%` (fallback `mistLowest @ 96%`) | `smoke @ 76%` (fallback `smoke @ 97%`) | `White @ 85%` (L) / `White @ 12%` (D) | `shadowInk @ 8%` (L) / `White @ 9%` (D) | `24dp` blur `(0, 8dp)` + `3dp` `(0, 1dp)`, clipped outside shape |
| `Sheet` | `28dp` top corners | `mistLow @ 90%` | `sheetDark @ 92%` | `White @ 90%` (L) / `White @ 10%` (D) | `shadowInk @ 7%` (L) / `White @ 8%` (D) | `32dp` blur `(0, 8dp)` |
| `Card` | `14dp` (`md`) or `20dp` (`lg`) | `White @ 62%` | `White @ 5%` | `White @ 70%` (L) / `White @ 6%` (D) | `shadowInk @ 6%` (L) / `White @ 7%` (D) | None (`blurSigma = 0`) |
| `Pill` | `Stadium` (`14dp` when expanded) | `White @ 55%` | `White @ 6%` | `White @ 75%` (L) / `White @ 8%` (D) | `shadowInk @ 7%` (L) / `White @ 8%` (D) | None (`blurSigma = 0`) |

- **Outer-Only Shadow Painting**: `GlassSurface` clips the canvas to the exterior of the rounded shape (`PathOperation.Difference` between inflated bounds and shape path) before drawing blurred drop shadows so shadows never tint the translucent interior.
- **Blur Budget**: `Card` and `Pill` tiers never allocate offscreen blur layers (`blurSigma = 0`), keeping scrolling 60/120 Hz lists completely free of GPU render-target thrashing.

### 3.3 Typography, Shapes & Motion
- **Typography (`sp` only)**:
  - System sans (`FontFamily.Default`) with two active structural weights (`w400` Normal, `w600` SemiBold) plus `w300` Light for display numerals/timers:
    - `displayLarge`: `48.sp w300` (`tnum`, live recording timer)
    - `displaySmall`: `28.sp w300` (empty/hero display)
    - `headlineLarge`: `26.sp w600` (`AppPage` large title)
    - `headlineMedium`: `22.sp w600`, `headlineSmall`: `20.sp w600`
    - `titleLarge`: `18.sp w600`, `titleMedium`: `16.sp w600`, `titleSmall`: `14.sp w600`
    - `bodyLarge`: `16.sp w400`, `bodyMedium`: `14.sp w400`, `bodySmall`: `12.sp w400`
    - `labelLarge`: `14.sp w600`, `labelMedium`: `12.sp w600`, `labelSmall`: `11.sp w400`
  - Monospace (`FontFamily.Monospace`, `fontFeatureSettings = "tnum"`): `mono` (`13.sp`), `monoSmall` (`12.sp`), `monoTimer` (`44.sp w300` tabular).
  - Any UI text showing times, dates, counts, or durations enables `tnum` (`tabularFigures`). Time ranges use an en-dash (`08:10–09:00`).
- **Radii (`AppShape`)**: `xs = 6.dp`, `sm = 10.dp`, `md = 14.dp`, `lg = 20.dp`, `xl = 28.dp`, `pill = CircleShape` (50% stadium).
- **Motion (`DshMotion`)**:
  - `press` (`150ms` critically damped spring to `0.97f` scale), `quick` (`160ms`), `standard` (`240ms`), `collapse` (`280ms`), `emphasized` (`340ms`), `breath` (`2400ms` ambient status dot fade `1.0 <-> 0.35`).
  - `LocalReducedMotion` observes `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`; when true, animations snap/crossfade in `0ms` and ambient loops hold steady at `1.0f`.

---

## 4. Shell & Screen Specifications

### 4.1 Root Shell ([MainActivity.kt](file:///home/ryno/project/class-recorder/android-app/app/src/main/java/com/ryno/classrecorder/MainActivity.kt))
- `ClassRecorderTheme` wraps a single root `AppBackdrop` so the gradient and radial glows stay stationary across tab swipes and secondary pages.
- **Floating Top Chrome Bar**: Positioned below `statusBarsPadding()`, `56.dp` tall, `12.dp` horizontal padding, **no bar background**:
  - **Left/Center (`Modifier.weight(1f)`)**: `SegmentedPill` wrapped in `GlassSurface(tier = Chrome, stadium = true)` with 3 segments: `Recorder`, `Schedule`, `Recordings`. The active thumb (`White @ 20%` in Dark, solid `White` with subtle `1dp` shadow in Light) tracks `pagerState` continuously during horizontal swipes and supports direct tap or horizontal drag selection with haptic ticks.
  - **Right**: `GlassIconButton` (`44.dp` visual circle inside a `48.dp` touch target, `Icons.Outlined.Settings`) with an optional `6.dp` `success` status dot badge when the MCP server is listening. Tapping pushes full-screen `SettingsScreen`.
- **Scroll Veil**: Each page reports `scrolledUnder` (`0f..1f` over the first `24.dp` of scroll) to render `BackdropVeil.top` behind the floating top bar only when content scrolls underneath.
- **Adaptive Width**: Content columns constrain to `maxWidth = 720.dp` centered horizontally on tablets, foldables, and landscape phones, with `displayCutoutPadding()` respected.

### 4.2 `RecordScreen`
- **Idle State**:
  1. **Class Context `OverviewCard` (`GlassSurface(Card, 20.dp)`)**:
     - Leading: `52.dp` circular `OverviewGlyph` colored with the active/selected course's FNV-1a `identity` pair (microphone/school glyph when idle with no class).
     - Header: `StatusChip` (`In class now` in `ok` tone, `Upcoming` in `busy` tone, `Manual class` in `busy` tone, or `Ready` in `idle` tone) + optional compact `Auto` reset pill when manually overridden.
     - Body: Course name (`headlineSmall`, up to 2 lines) + metadata line (`Slot 7 · Room 3806 · Teacher`, `tnum`, `onSurfaceVariant`). Tapping the card opens the **Course Picker `AppSheet`**.
     - Footer (under `0.5dp` hairline): Live MCP status row (`AnimatedStatusDot` + `MCP :8765 · Listening` or `MCP server stopped` + chevron), tapping opens the **MCP Server `AppSheet`**.
  2. **Recent Agent Activity `GlassPill` (conditional)**: Shown when `ui.mcpStatus.recentCalls.isNotEmpty()`; collapsed `34.dp` stadium pill (`● N agent calls · latest tool`) that smoothly expands into a `14.dp` card listing recent MCP calls in `monoSmall`.
  3. **Quick Course Strip**: Horizontal `LazyRow` of `CapsuleChoiceChip`s (`>= 48.dp` touch height) with `8.dp` `identity` dots for one-tap manual course override.
  4. **Mic Permission Banner (when `!hasMicPermission`)**: Calm `GlassSurface(Card, 14.dp)` notice with `Allow microphone` tonal capsule button.
- **Live / Paused State**:
  - Smoothly transitions center stage to a `GlassSurface(Card, 20.dp)` live panel containing:
    - Active course title + `StatusChip` (`Recording` in pulsing `error`/brick-rose tone, or `Paused` in `warn` tone) + room/slot subtitle.
    - Scrolling `PixelWaveformCanvas` (`180–260.dp` tall) rendered in brick-rose (`danger`) when live and muted tertiary when paused.
    - Large light tabular timer (`48.sp w300` `tnum`, e.g. `12:48`).
- **Floating Bottom Composer Dock (`GlassSurface(Chrome, 28.dp)`, `12.dp` horizontal inset + `navigationBarsPadding()`)**:
  - **Idle**: Left column displays `Ready to record` (or `Mic permission needed`) + live target folder/filename preview (`<Course>/...m4a` in `textTertiary`); right side holds a `48.dp` circular brick-rose (`danger`) record button (`Icons.Rounded.Mic`).
  - **Live / Paused**: Left holds a `44.dp` (`48.dp` hit) circular `Discard` button (`Icons.Rounded.DeleteOutline`); center holds a full-height `Pause` / `Resume` pill button; right holds a brick-rose `Stop & Save` pill button (`Icons.Rounded.Stop`).

### 4.3 `ScheduleScreen`
- **Header Row**: School name (`titleLarge`) + tabular summary (`5 classes today · 24 this week` in `onSurfaceVariant`) on the left; two `GlassIconButton`s (`Refresh` with inline progress spinner when syncing, and `Schedule source` link icon) on the right.
- **7-Day Week Strip (`GlassSurface(Card, 20.dp)`)**:
  - 7 equal columns (`Mon`–`Sun`), fixed `96.dp` height, with a spring-animated `primary` selection capsule behind the chosen day (`onPrimary` text), a `4.dp` `primary` dot marking today, tabular date numerals (`20.sp w600`), and up to 5 compact horizontal `identity`-colored density pips per day.
- **Day Timetable (`ListPageCard` / `GlassSurface(Card, 20.dp)`)**:
  - When `ui.schedule == null`: `EmptyView` (`Icons.Outlined.CalendarMonth`, `"No schedule synced"`, `"Import your uni-campus share link or NUTC ePortal timetable"`, action `"Sync schedule"`).
  - When selected day has 0 classes: `EmptyView` (`"No classes on <Day>"`, action `"Jump to today"` when viewing another day).
  - Otherwise: `LazyColumn` of class cards (`GlassSurface(Card, 20.dp)`, `10.dp` vertical spacing) where each row displays:
    - Left `88.dp` tabular time gutter (`08:10–09:00` in `labelSmall tnum`, `P1` badge below).
    - `8.dp` `identity` course dot + Course name (`titleMedium`, max 2 lines) + `StatusChip` (`NOW` in `ok` / `NEXT` in `busy` when matched) + room & teacher subtitle (`3806 · 陳俊智`).
    - Trailing `48.dp` circular record trigger: `primary` filled with `onPrimary` mic icon on the `NOW` class (the single primary emphasis on screen), subtle `pill`-glass mic icon on all other classes.
- **Schedule Source `AppSheet`**: `SheetHeader("Schedule Source")`, `SheetGroup` holding a borderless recessed input well (`imePadding()` safe) pre-filled with `ui.scheduleSourceInput` and a `primary` `"Sync schedule"` capsule button.

### 4.4 `LibraryScreen`
- **Header Bar**:
  - Normal mode: `SearchCapsule` (`weight(1f)`, `44.dp` visual / `48.dp` hit, borderless `7%` white in Dark / `5%` ink in Light, clear `✕` button when non-empty) + three compact `GlassIconButton`s: `Select` (when recordings exist), `Export .zip`, and `MCP`.
  - Selection mode: Transitions to a compact header row (`✕ Cancel`, `<N> selected`, `Select all` text action) + floating bottom `chrome`-glass `SelectionDock` (`Export (.zip)` + `Delete` in `danger`).
- **Course Filter Row**: Horizontal `LazyRow` of `CapsuleChoiceChip`s (`All (<N>)` + distinct courses). Selected chip uses DSH Go's high-contrast ink fill (`onSurface` bg, `surface` text) rather than loud accent flood.
- **Day-Grouped Recordings**:
  - Empty / no-match states use `EmptyView`.
  - Each day section renders a `PageSectionLabel` (`Today`, `Yesterday`, or `EEE · MMM d` + right-aligned tabular count) followed by a `ListPageCard` (`GlassSurface(Card, 20.dp)`) containing touching `ListPageRow`-proportioned items (`minHeight = 64.dp`, `0.5dp` subtle hairline between items inside the same day card):
    - Leading: `36.dp` circular `identity` badge (or `SelectionMark` check circle in selection mode; pulsing brick-rose equalizer icon while inline-playing).
    - Title: Course name (`15.sp w600`) + optional `#tag` pill.
    - Subtitle: Tabular start time + duration (`10:15 · 48:12`) + room + single-line note preview if present.
    - Trailing: `48.dp` inline Play/Stop toggle button + `48.dp` disclosure tap on the row opening full-screen `PlaybackScreen` (long-press enters selection mode; `More` action in `PlaybackScreen` or swipe/menu edits notes).
- **Bottom Sheets (`AppSheet`)**:
  - **Mass Export Sheet**: `SheetHeader("Export Recordings (.zip)")`, description, primary `"Export all (<N>)"` capsule, and per-course `ListPageCard` export rows.
  - **MCP Server Sheet**: Mirrors the MCP `OverviewCard` + `CodeWell` copyable `LAN` / `ADB` endpoints + recent agent calls list.

### 4.5 `PlaybackScreen` (DSH Go `AppPage`)
- Pushed/overlaid full-screen `AppPage(title = recording.courseName)` with predictive `BackHandler`:
  - Floating top bar: `44.dp` (`48.dp` hit) `GlassIconButton` (`←`) on the left, scroll-rising compact title in the center, and `GlassIconButton` (`Edit notes` `Icons.Outlined.EditNote`) on the right.
  - **`PageHero`**: `52.dp` `OverviewGlyph` (course `identity` tint), `headlineSmall` course title, `tnum` metadata (`2026-11-18 10:15 · P3 · Room 3806 · Teacher`), and `#tag` `StatusChip`s. Fades smoothly on scroll.
  - **Waveform Card (`GlassSurface(Card, 20.dp)`)**: `160.dp` scrubbable `PlaybackWaveformCanvas` (`primary` played bars, `outlineVariant` unplayed bars), tabular `currentPos` / `duration` labels, and slim `3.dp` progress track.
  - **Transport Row**: `52.dp` circular `GlassSurface(Card, circle = true)` `-5s` button, `weight(1f)` `56.dp` `primary` Play/Pause stadium button (`onPrimary` icon + label), and `52.dp` `+10s` button.
  - **Speed Section (`SettingsSection("Playback speed")`)**: Full-width `SegmentedPill` (`0.5×`, `1×`, `1.25×`, `1.5×`, `2×`).
  - **Notes & File Identity (`SettingsSection`)**:
    - Notes `GlassSurface(Card, 20.dp)` displaying synced agent/user notes (or `"Add lecture notes or tags…"` prompt tapping into the `NotesSheet`).
    - Recessed `CodeWell` showing `<filename>.m4a` + delete action in `danger` with confirmation dialog.

### 4.6 `SettingsScreen` (DSH Go `AppPage` + Full Appearance Picker)
- Full-screen `AppPage(title = "Settings")` with `BackHandler`:
  1. **MCP `OverviewCard`**:
     - `52.dp` `OverviewGlyph(Icons.Outlined.Hub, tint = 0)`, title `"On-device MCP server"`, subtitle `"Listening on port 8765"` / `"Stopped"`, trailing `Switch`, and `OverviewFooter` showing endpoint status.
     - Expandable `CodeWell` section below with one-tap copy rows for `LAN` (`http://<ip>:8765/mcp`) and `ADB` (`adb forward tcp:8765 tcp:8765`) plus recent agent calls.
  2. **`SettingsSection("Appearance")`**:
     - **`ThemeModePicker`**: 3 side-by-side `0.8`-aspect miniature Mist Glass preview cards (`Light`, `Dark`, `System` split-diagonal) drawn with `Canvas` in the active `AppAccent`, surrounded by an animated `2.dp` `primary` ring when selected.
     - **`PalettePicker`**: 6 circular `30.dp` guarded color swatches (`Mist`, `Daiqing`, `Dusk`, `Turquoise`, `Amber`, `Graphite`) with animated outer selection rings and labels.
     - **`Follow Wallpaper` Row (`SettingsSwitchTile`, API 31+)**: Shows live wallpaper-derived `AccentDot` + toggle switching `accentId` between `"wallpaper"` and the last preset.
  3. **`SettingsSection("Class schedule")`**:
     - School/period summary row + recessed URL/share-code input field + `"Sync schedule"` button.
  4. **`SettingsSection("File naming")`**:
     - Template input field, live `CodeWell` `.m4a` preview, `CapsuleChoiceChip` date-format picker, tappable placeholder chips (`{class}`, `{date}`, `{lesson number}`, `{room}`, `{teacher}`, `{time}`, `{slot}`), and `Reset` / `Save` row.

---

## 5. Data Flow & Error Handling

- **Unidirectional State**:
  - `MainViewModel` owns `ui: StateFlow<MainUiState>` and `recorderState: StateFlow<RecorderState>`.
  - `AppearancePreferences` (`SharedPreferences` `"dsh_appearance"`) loads synchronously in `MainViewModel.init` so the very first frame renders with the user's saved `ThemeMode` and `AppAccent` (zero theme flash on cold start).
- **Error & Edge-Case Handling**:
  - **Schedule sync failure**: Keeps existing cached schedule in SQLite and surfaces the error via a `chrome`-glass snackbar without clearing the timetable.
  - **Port `:8765` already bound** (e.g., side-by-side `.uitest` install): `runCatching` in `MainViewModel` keeps the app running and reflects `running = false` cleanly in the MCP `OverviewCard` and top status dot.
  - **Long CJK / English text & `1.3x` font scale**: `SegmentedPill` scales down single-line labels gracefully if constrained, `SettingsSegmentTile` stacks vertically when inline width overflows, and course names allow up to 2 lines with ellipsis.

---

## 6. Verification Plan

1. **JVM Unit Tests (`./gradlew :app:testClassrecorderTestDebugUnitTest`)**:
   - `OklchContrastTest`: Verifies WCAG contrast ratios across all 6 `AppAccent` presets + synthetic wallpaper hues in both Light and Dark schemes (`ink`/`paper` on `surface` `>= 10:1`, `onPrimary` on `primary` `>= 4.5:1`, `primary` on `surfaceContainerHighest` `>= 3.0:1`).
   - `IdentityHashTest`: Verifies deterministic FNV-1a `identityIndex` stability across strings and CJK course names.
2. **Build Verification**:
   - `./gradlew :app:assembleClassrecorderTestDebug` and `:app:assembleClassrecorderDebug` compile with zero errors.
   - Verify every hand-written `.kt` file under `app/src/main/java/` is `< 1,000` lines.
3. **Device / Emulator Visual & Accessibility Pass (`.uitest` flavor)**:
   - Install `app-classrecorderTest-debug.apk` (isolated from real user data), seed/sync NUTC schedule, and capture screenshots into `.impeccable/review/` across `Record` (idle & live), `Schedule`, `Library`, `Playback`, and `Settings` in **Dark Mist**, **Light Mist**, an alternate accent preset, and **`1.3x` system font scale**.
   - Run `impeccable-finish-reviewer` against the captures, surface brief, `craft-floor.md`, and `android.md`, resolve any findings, and generate `DESIGN.md` + `.impeccable/design.json`.
