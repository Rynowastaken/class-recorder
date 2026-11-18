# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

University students (initially National Taichung University of Science and Technology / NUTC and Taiwan campus schedule users) capturing lectures in live classrooms and reviewing, organizing, and processing recordings alongside AI agents on their phone or laptop.

## Product Purpose

LectureShelf captures classroom lectures reliably in the background, automatically tags and files `.m4a` recordings into per-course folders using the student's synced class timetable, and exposes schedules, recordings, notes, tags, and live recording controls to AI agents over an embedded on-device Model Context Protocol (MCP) server. Success means starting or reviewing a class recording takes one tap with zero manual file naming, and any local or LAN AI agent can immediately query schedules, read/write lecture notes, or trigger recordings.

## Positioning

Unlike generic voice-memo apps that dump untitled timestamps into a flat list, LectureShelf combines automatic campus-timetable class matching (`uni-campus-app` share codes and `Personal-NUTC-Eportal` JSON) with a built-in Rust `axum`/`tokio` Streamable HTTP + SSE MCP server (`:8765`) running directly on the Android device.

## Operating Context

- Used one-handed at a classroom desk under bright indoor lighting or dimmed lecture-projector rooms, often launched seconds before or after a period starts.
- Runs a foreground `microphone` `RecordingService` (`44.1 kHz`, `128 kbps` AAC `.m4a`) with Android 16 promoted live updates and Xiaomi HyperOS focus/island notifications (`RecordingService.kt`).
- Connects to desktop or mobile AI agents over Wi-Fi LAN (`http://<lan-ip>:8765/mcp`) or USB (`adb forward tcp:8765 tcp:8765`).
- Ships a side-by-side `classrecorderTest` (`com.ryno.classrecorder.uitest`) Gradle flavor so UI testing and screenshots never touch real lecture recordings under `com.ryno.classrecorder`.

## Capabilities and Constraints

- **Core stack**: Kotlin + Jetpack Compose (`compileSdk = 36`, `minSdk = 29`, `targetSdk = 35`, Compose BOM `2025.09.01`, Material 3 `1.4.0`) backed by `rust-core/` (`libclass_recorder_core.so`, `rusqlite` SQLite index + `reqwest` schedule sync + `axum` MCP server).
- **Preserved backend & service contracts**: `RustCore` JNI methods, `MainViewModel` state/actions (extended only for persisted Appearance preferences), `RecordingService` foreground lifecycle & HyperOS/Android 16 notification payloads, and `.zip` mass export with `metadata.json`.
- **Terminology**: `Recorder`, `Schedule`, `Recordings`, `Playback`, `Settings`, `On-device MCP server`, `Slot` / `P1..P9`, `Uncategorized`.

## Brand Commitments

- Adopt **DSH Go**'s "Mist Glass" design language across the entire Android app: full Light ("Morning Mist") and Dark ("Slate Night") modes, DSH Go's 6 low-saturation OKLCH accent palettes (`Mist`, `Daiqing`, `Dusk`, `Turquoise`, `Amber`, `Graphite`) plus `Follow Wallpaper`, floating `chrome`-tier glass navigation and composer dock, 4-tier glass surfaces, and DSH Go's `AppPage` / `OverviewCard` / `ListPageCard` / `SegmentedPill` / `SearchCapsule` component vocabulary.

## Evidence on Hand

- Reference DSH Go design sources in `/home/ryno/project/dsh-Go-ref/` (`dsh_palette.dart`, `dsh_surfaces.dart`, `glass.dart`, `glass_pill.dart`, `dsh_typography.dart`, `dsh_shapes.dart`) and live reference screenshot `android-app/ui-shots/dsh-ref1.png`.
- Real NUTC timetable share source (`https://uni-campus-app.pages.dev/s/qVaaFS5A`) with Traditional Chinese course names, room numbers, and instructors.
- Existing architecture spec in `docs/superpowers/specs/2026-11-13-class-recorder-design.md`.

## Product Principles

1. **Zero-friction capture**: The current or upcoming lecture and the record trigger are unmistakable on launch; tagging or overriding a class never takes more than one tap.
2. **Calm operational clarity**: Status (idle, in class, recording, paused, MCP listening) is communicated through clear labels, tabular numerals, and restrained semantic indicators rather than visual noise.
3. **Agent-native transparency**: MCP server state, connection endpoints (`LAN` and `adb forward`), and agent-synced lecture notes/tags are first-class citizens without turning the student's recorder into a developer console.
4. **Data safety by construction**: Background recording survives app switching, schedule sync failures preserve cached timetables, and UI verification uses the isolated `.uitest` package.

## Accessibility & Inclusion

- Minimum `48×48 dp` touch targets across all interactive controls, `sp`-scaled typography verified at `1.3x` system font scale, WCAG AA contrast (`>= 4.5:1` text, `>= 3:1` non-text controls) in both Light and Dark themes across every accent preset, TalkBack semantics on recording/playback/selection states, and full respect for system Reduce Motion (`ANIMATOR_DURATION_SCALE == 0`).
