# Class Recorder (Material 3 + Rust Core + MCP) Design Spec

**Date:** 2026-11-13  
**Project Root:** `/home/ryno/.gemini/antigravity/scratch/class-recorder`  
**Target Device:** Android (Pixel 7, `panther`, API 35/36)

## 1. Overview & Goals
Build a Material Design 3 Android audio recorder tailored for university classes and AI agent workflows:
1. **Schedule-Aware Recording & File Organization:** Pulls class schedules from `uni-campus-app.pages.dev/s/<code >` (resolved via `kewutong.cloud-service.win/api/v2/share/<code>`) and `Personal-NUTC-Eportal` JSON endpoints/exports. Automatically tags and organizes recordings into per-course folders (`<CourseName>/YYYY-MM-DD_HHmm_<CourseName>_<Room>.m4a`), with a manual class picker override.
2. **Agent-First MCP Connectivity:** Embeds an on-device **MCP (Model Context Protocol)** Streamable HTTP + SSE server (`0.0.0.0:8765`, reachable over Wi-Fi LAN or `adb forward tcp:8765 tcp:8765`) so AI agents can query schedules, browse/download recordings, attach notes/tags, and start/stop recordings.
3. **Hybrid Architecture (Kotlin Material 3 UI + Rust Core):**
   - `rust-core/` (`libclass_recorder_core.so`, `aarch64-linux-android`): Schedule sync & normalization, active-class time matching, SQLite metadata index, file path generation, and the embedded `axum`/`tokio` MCP server.
   - `android-app/` (Kotlin + Jetpack Compose + Material 3): Dynamic Color UI, `RECORD_AUDIO` permission handling, `ForegroundService` (`microphone`) for reliable background `.m4a` recording, inline playback, and JNI bridge to `rust-core`.

## 2. Architecture & Component Boundaries

### 2.1 `rust-core/` (Rust `cdylib` + host unit-testable library)
- **`schedule` module:**
  - Parses `uni-campus-app` share URLs/codes (`https://uni-campus-app.pages.dev/s/qVaaFS5A` -> `https://kewutong.cloud-service.win/api/v2/share/qVaaFS5A`).
  - Parses `Personal-NUTC-Eportal` schedule JSON (array of `{ slot, time, days }` or `{ schedule: { slots: ... } }` / `CLASS_SCHEDULE_TIME_SLOTS` format).
  - Normalizes into `UnifiedSchedule { school_name, source_url, synced_at, slots: Vec<TimeSlotRow> }` where each `days[0..6]` maps Monday–Sunday to `Option<CourseSlot { name, teacher, room, color }>`.
  - `match_current_class(schedule, local_datetime, grace_minutes = 15)` returns `CurrentClassMatch` (`active` or `upcoming`) or `None`.
- **`storage` module (`rusqlite` with `bundled` feature):**
  - Tables: `schedules`, `recordings`, `settings`.
  - `recordings` schema: `id (TEXT PK)`, `file_path (TEXT)`, `course_name (TEXT)`, `teacher (TEXT)`, `room (TEXT)`, `slot (TEXT)`, `started_at (INTEGER)`, `duration_ms (INTEGER)`, `status (TEXT: recording|completed)`, `notes (TEXT)`, `tags (TEXT JSON)`.
  - `build_recording_path(base_dir, course_match, timestamp)`: sanitizes course/room strings for Android filesystem safety and produces `<base_dir>/<CourseName>/<YYYY-MM-DD_HHmm>_<CourseName>_<Room>.m4a` (or `Uncategorized/<YYYY-MM-DD_HHmm>_Recording.m4a`).
- **`mcp` module (`tokio` + `axum`):**
  - Endpoints:
    - `POST /mcp` — JSON-RPC 2.0 handler (`initialize`, `notifications/initialized`, `ping`, `tools/list`, `tools/call`, `resources/list`, `resources/read`).
    - `GET /mcp` — SSE stream for MCP server-to-client notifications / keepalive.
    - `GET /files/:id` — Streams the `.m4a` audio file for a recording ID.
    - `GET /health` — Simple JSON health/status endpoint.
  - Optional Bearer token auth (configured in app settings; disabled by default for local `adb forward` / trusted LAN).
  - **MCP Tools:**
    - `get_schedule(day?: string)`
    - `get_current_class()`
    - `list_recordings(course_name?: string, limit?: number)`
    - `get_recording_details(id: string)`
    - `update_recording_notes(id: string, notes?: string, tags?: string[])`
    - `start_recording(course_name?: string, teacher?: string, room?: string, slot?: string)`
    - `stop_recording()`
    - `sync_schedule(source_url?: string)`
  - **MCP Resources:**
    - `schedule://weekly` (`application/json`)
    - `recordings://index` (`application/json`)
- **`jni_bridge` module:**
  - Exports `Java_com_ryno_classrecorder_core_RustCore_*` functions:
    - `initCore(filesDir, recordingsDir, callbackObj)`
    - `syncSchedule(sourceUrl)` -> JSON
    - `getScheduleJson()` -> JSON
    - `getCurrentClassJson()` -> JSON
    - `prepareRecording(courseOverrideJson)` -> JSON `{ id, filePath, courseName, ... }`
    - `completeRecording(id, durationMs)` -> JSON
    - `listRecordings(courseFilter)` -> JSON
    - `updateRecordingNotes(id, notes, tagsJson)` -> JSON
    - `deleteRecording(id)` -> JSON
    - `startMcpServer(port, authToken)` / `stopMcpServer()` / `getMcpStatus()`
  - Holds a `JavaVM` reference + global ref to `RecordingBridge` so MCP `start_recording` / `stop_recording` can invoke Kotlin `RecordingService` commands safely from Tokio threads.

### 2.2 `android-app/` (Kotlin + Jetpack Compose + Material 3)
- **Theme:** Material 3 (`MaterialTheme` with `dynamicDarkColorScheme` / `dynamicLightColorScheme` on Android 12+, fallback tonal palette).
- **Screens (Bottom `NavigationBar`):**
  1. **Record:** Active/upcoming class banner (auto-detected from schedule, with "Change Class" bottom sheet), live timer & audio level visualizer, Material 3 `LargeFloatingActionButton` (Record/Stop) + Pause/Resume button, MCP server status pill.
  2. **Schedule:** Today / Weekly view of NUTC classes with course color accents, tap class to filter/start recording, top-bar action to open **Schedule Sources** sheet (`uni-campus-app` link/code + `Personal-NUTC-Eportal` URL/JSON import, pre-filled with `https://uni-campus-app.pages.dev/s/qVaaFS5A` as default/example).
  3. **Library:** Course-grouped folders & recording cards, inline audio player (`MediaPlayer`), recording detail sheet showing metadata + agent-written notes/tags (editable by user too), and **MCP Server** sheet (Start/Stop toggle, port, LAN IP URL, `adb forward tcp:8765 tcp:8765` copyable command).
- **Foreground Service (`RecordingService`):**
  - `android:foregroundServiceType="microphone"` with notification channel and Stop/Pause notification actions.
  - Records AAC `.m4a` (`MediaRecorder.OutputFormat.MPEG_4`, `AudioEncoder.AAC`, 44.1kHz, 128kbps).

## 3. Error Handling
- Network errors during schedule sync preserve the last cached schedule in SQLite and return a structured error to UI/MCP.
- Missing `RECORD_AUDIO` permission returns a clear JSON-RPC tool error (`Microphone permission not granted on device`) when `start_recording` is called via MCP.
- Port binding failures on MCP startup report the bind error cleanly to UI without crashing the app.

## 4. Verification Plan
1. `cargo test` in `rust-core/` covering schedule parsing (`qVaaFS5A` & Eportal formats), time-slot matching, path sanitization, SQLite operations, and MCP JSON-RPC handlers.
2. Cross-compile `libclass_recorder_core.so` (`aarch64-linux-android`) using Android NDK, build APK with Gradle, install on connected Pixel 7 (`28221FDH2001LB`).
3. Verify on-device: launch app, grant mic permission, sync `qVaaFS5A` schedule, record a sample clip, and verify live MCP calls from laptop via `adb forward tcp:8765 tcp:8765` (`initialize`, `tools/list`, `get_schedule`, `list_recordings`, `update_recording_notes`).
