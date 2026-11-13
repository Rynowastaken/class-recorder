# Class Recorder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Material Design 3 Android audio recorder with NUTC class-schedule integration (`uni-campus-app` & `Personal-NUTC-Eportal`), course-organized `.m4a` storage, and an embedded on-device Rust MCP server, verified on the connected Pixel 7.

**Architecture:** A Rust core (`rust-core/`, compiled to `libclass_recorder_core.so` for `aarch64-linux-android`) owns schedule fetching/normalization, active-class time matching, SQLite metadata indexing, and the `axum`/`tokio` MCP Streamable HTTP server. A thin Kotlin + Jetpack Compose Material 3 Android app (`android-app/`) provides Dynamic Color UI, `RECORD_AUDIO` permissions, and a `microphone` `ForegroundService` bridged to Rust via JNI.

**Tech Stack:** Rust 1.99+ (`tokio`, `axum`, `reqwest`, `rusqlite` bundled, `serde`, `chrono`, `jni`), Android SDK 35, NDK `28.2.13676358`, Gradle 8.14, Kotlin, Jetpack Compose Material 3.

**Spec:** `docs/superpowers/specs/2026-11-13-class-recorder-design.md`

## Global Constraints
- Target Android API: `minSdk = 29`, `compileSdk = 35`, `targetSdk = 35`, ABI `arm64-v8a` (`aarch64-linux-android`).
- Audio format: AAC `.m4a` (`MediaRecorder.OutputFormat.MPEG_4`, `AudioEncoder.AAC`).
- File path layout: `<recordings_dir>/<CourseName>/<YYYY-MM-DD_HHmm>_<CourseName>_<Room>.m4a` (or `Uncategorized/<YYYY-MM-DD_HHmm>_Recording.m4a`).
- MCP endpoint: `POST /mcp` (JSON-RPC 2.0), `GET /mcp` (SSE), `GET /files/:id` (audio download), `GET /health`.
- All Rust unit tests must run and pass on Linux host (`cargo test`) without an Android device.

## Review Focus
- **Invalid/malformed schedule URLs or offline network:** `sync_schedule` must return a structured error while preserving any previously cached schedule in SQLite.
- **Special characters / slashes in course or room names (e.g., `"C++/Java"`, `"資工 1/2"`):** Path sanitization must strip/replace `/`, `\`, `:`, `*`, `?`, `"`, `<`, `>`, `|` so `File` creation never fails or traverses directories.
- **MCP `start_recording` without `RECORD_AUDIO` permission:** Must return a JSON-RPC tool error (`isError: true`) rather than crashing the Android process.
- **Duplicate/overlapping MCP `start_recording` calls:** Must return a clear error if a recording is already active.
- **Unauthenticated vs. Bearer-protected MCP requests:** When a non-empty auth token is configured, missing/wrong `Authorization: Bearer <token>` must return `401 Unauthorized`.

---

### Task 1: Rust Core Crate & Schedule Engine

**Files:**
- Create: `rust-core/Cargo.toml`
- Create: `rust-core/src/lib.rs`
- Create: `rust-core/src/schedule.rs`

**Interfaces:**
- Produces:
  - `UnifiedSchedule`, `TimeSlotRow`, `CourseSlot`, `CurrentClassMatch`, `MatchKind`
  - `resolve_schedule_source_url(input: &str) -> Result<String, String>`
  - `parse_schedule_json(raw: &str, source_url: &str) -> Result<UnifiedSchedule, String>`
  - `match_current_class(schedule: &UnifiedSchedule, local_dt: NaiveDateTime, grace_minutes: i64) -> Option<CurrentClassMatch>`

- [ ] **Step 1: Create `rust-core/Cargo.toml` and `rust-core/src/lib.rs` with module declarations**
  Include `serde`, `serde_json`, `rusqlite` (`bundled`), `tokio` (`full`), `axum`, `reqwest` (`json`, `rustls-tls`, no default features), `chrono`, `uuid` (`v4`), `jni`, `thiserror`, `tracing`, and `tower` in `[dev-dependencies]`. Set `[lib] crate-type = ["cdylib", "rlib"]`.

- [ ] **Step 2: Write failing unit tests in `rust-core/src/schedule.rs`**
  Add `#[cfg(test)] mod tests` testing:
  - `test_resolve_uni_campus_share_url`: `"https://uni-campus-app.pages.dev/s/qVaaFS5A"` -> `"https://kewutong.cloud-service.win/api/v2/share/qVaaFS5A"` and raw code `"qVaaFS5A"` -> same URL.
  - `test_parse_kewutong_schedule`: parses sample `{"status":"ok","schoolName":"國立臺中科技大學","data":[{"slot":"1","time":"08:10~09:00","days":[{},{"name":"體育","teacher":"李俊杰","room":"0000","color":"#FFD600"},{},{},{},{},{}]}]}` into `UnifiedSchedule`.
  - `test_parse_eportal_schedule`: parses `Personal-NUTC-Eportal` style `{ "schoolName": "NUTC", "slots": [...] }` or bare array `[...]`.
  - `test_match_current_class_active_and_grace`: Tuesday `08:30` matches `"體育"` as `MatchKind::Active`; Tuesday `07:58` (12 min before `08:10`) matches `"體育"` as `MatchKind::Upcoming`; Sunday `23:00` returns `None`.

- [ ] **Step 3: Run `cargo test` to verify tests fail**
  Run: `cargo test --manifest-path rust-core/Cargo.toml schedule`
  Expected: FAIL (functions not implemented).

- [ ] **Step 4: Implement `rust-core/src/schedule.rs`**
  Implement `resolve_schedule_source_url`, `parse_schedule_json`, `match_current_class`, and `fetch_schedule(source_url: &str)` using `reqwest`.

- [ ] **Step 5: Run `cargo test` to verify schedule tests pass**
  Run: `cargo test --manifest-path rust-core/Cargo.toml schedule`
  Expected: PASS (4+ tests).

- [ ] **Step 6: Commit**
  ```bash
  git add rust-core/Cargo.toml rust-core/src/lib.rs rust-core/src/schedule.rs
  git commit -m "feat(rust-core): add schedule parser and active-class matcher"
  ```

---

### Task 2: SQLite Storage & Course-Based File Organizer

**Files:**
- Create: `rust-core/src/storage.rs`
- Modify: `rust-core/src/lib.rs`

**Interfaces:**
- Consumes: `UnifiedSchedule`, `CurrentClassMatch` from `schedule.rs`
- Produces:
  - `RecordingRecord { id, file_path, course_name, teacher, room, slot, started_at, duration_ms, status, notes, tags }`
  - `Storage::open(db_path: &Path) -> Result<Self, String>`
  - `Storage::save_schedule(&self, schedule: &UnifiedSchedule) -> Result<(), String>`
  - `Storage::load_schedule(&self) -> Result<Option<UnifiedSchedule>, String>`
  - `Storage::insert_recording(&self, rec: &RecordingRecord) -> Result<(), String>`
  - `Storage::complete_recording(&self, id: &str, duration_ms: i64) -> Result<RecordingRecord, String>`
  - `Storage::list_recordings(&self, course_filter: Option<&str>, limit: usize) -> Result<Vec<RecordingRecord>, String>`
  - `Storage::get_recording(&self, id: &str) -> Result<Option<RecordingRecord>, String>`
  - `Storage::update_recording_notes(&self, id: &str, notes: Option<&str>, tags: Option<Vec<String>>) -> Result<RecordingRecord, String>`
  - `Storage::delete_recording(&self, id: &str) -> Result<(), String>`
  - `sanitize_segment(s: &str) -> String`
  - `build_recording_path(base_dir: &Path, course: Option<&CurrentClassMatch>, dt: NaiveDateTime) -> PathBuf`

- [ ] **Step 1: Write failing unit tests in `rust-core/src/storage.rs`**
  Test:
  - `test_sanitize_and_build_path`: `"C++/Web: Lab"` + room `"33/09"` produces `<base>/C++_Web_ Lab/2026-11-13_0810_C++_Web_ Lab_33_09.m4a` (no `/` or `:` in filename/folder segment); `None` course produces `<base>/Uncategorized/2026-11-13_0810_Recording.m4a`.
  - `test_sqlite_recording_lifecycle_and_notes`: open `:memory:`, insert recording, complete with `duration_ms = 12000`, update notes/tags, list by course filter, verify fields.
  - `test_sqlite_schedule_roundtrip`: save `UnifiedSchedule`, load back, verify `school_name` and slots.

- [ ] **Step 2: Run `cargo test` to verify tests fail**
  Run: `cargo test --manifest-path rust-core/Cargo.toml storage`
  Expected: FAIL.

- [ ] **Step 3: Implement `rust-core/src/storage.rs`**
  Use `rusqlite::Connection` behind a `std::sync::Mutex` so `Storage` is `Send + Sync` for `axum` state.

- [ ] **Step 4: Run `cargo test` to verify storage tests pass**
  Run: `cargo test --manifest-path rust-core/Cargo.toml storage`
  Expected: PASS.

- [ ] **Step 5: Commit**
  ```bash
  git add rust-core/src/storage.rs rust-core/src/lib.rs
  git commit -m "feat(rust-core): add SQLite storage and course-based file path builder"
  ```

---

### Task 3: On-Device MCP Server (`axum` + `tokio`)

**Files:**
- Create: `rust-core/src/mcp.rs`
- Modify: `rust-core/src/lib.rs`

**Interfaces:**
- Consumes: `Storage` (`storage.rs`), `schedule` functions (`schedule.rs`)
- Produces:
  - `RecordingController` trait (`start_recording(&self, course_json: &str) -> Result<RecordingRecord, String>`, `stop_recording(&self) -> Result<RecordingRecord, String>`)
  - `McpState`
  - `build_router(state: Arc<McpState>) -> axum::Router`
  - `McpServerHandle` (`start(port: u16, auth_token: Option<String>, state: Arc<McpState>)`, `stop()`)

- [ ] **Step 1: Write failing unit tests in `rust-core/src/mcp.rs`**
  Using `tower::ServiceExt::oneshot` and a `MockRecordingController`:
  - `test_mcp_initialize_and_tools_list`: `POST /mcp` with `initialize` returns `protocolVersion` and `capabilities.tools`; `tools/list` returns all 8 tools.
  - `test_mcp_schedule_and_recordings_tools`: `get_schedule`, `list_recordings`, `update_recording_notes`, and `start_recording`/`stop_recording` via `MockRecordingController`.
  - `test_mcp_bearer_auth`: when `auth_token` is `Some("secret")`, missing/wrong header returns `401`, valid `Bearer secret` returns `200`.

- [ ] **Step 2: Run `cargo test` to verify tests fail**
  Run: `cargo test --manifest-path rust-core/Cargo.toml mcp`
  Expected: FAIL.

- [ ] **Step 3: Implement `rust-core/src/mcp.rs`**
  Implement JSON-RPC 2.0 dispatcher on `POST /mcp`, SSE keepalive on `GET /mcp`, audio file download on `GET /files/:id`, `GET /health`, and `McpServerHandle` with graceful shutdown channel.

- [ ] **Step 4: Run `cargo test` to verify all Rust unit tests pass**
  Run: `cargo test --manifest-path rust-core/Cargo.toml`
  Expected: PASS (all `schedule`, `storage`, `mcp` tests).

- [ ] **Step 5: Commit**
  ```bash
  git add rust-core/src/mcp.rs rust-core/src/lib.rs
  git commit -m "feat(rust-core): add axum MCP Streamable HTTP server and unit tests"
  ```

---

### Task 4: JNI Bridge & `aarch64-linux-android` Cross-Compilation

**Files:**
- Create: `rust-core/src/jni_bridge.rs`
- Create: `rust-core/.cargo/config.toml`
- Modify: `rust-core/src/lib.rs`

**Interfaces:**
- Produces JNI symbols in package `com.ryno.classrecorder.core.RustCore`:
  - `initCore(filesDir: String, recordingsDir: String, bridge: RecordingBridge): String`
  - `syncSchedule(sourceUrl: String): String`
  - `getScheduleJson(): String`
  - `getCurrentClassJson(): String`
  - `prepareRecording(courseOverrideJson: String): String`
  - `completeRecording(id: String, durationMs: Long): String`
  - `listRecordings(courseFilter: String): String`
  - `updateRecordingNotes(id: String, notes: String, tagsJson: String): String`
  - `deleteRecording(id: String): String`
  - `startMcpServer(port: Int, authToken: String): String`
  - `stopMcpServer(): String`
  - `getMcpStatus(): String`

- [ ] **Step 1: Implement `rust-core/src/jni_bridge.rs`**
  Wire `OnceLock` global runtime (`CoreContext`: `tokio::runtime::Runtime`, `Arc<Storage>`, `recordings_dir`, `JavaVM` + `GlobalRef` to `RecordingBridge`, `Mutex<Option<McpServerHandle>>`). Implement `JniRecordingController` calling `RecordingBridge.startRecordingFromAgent(String)` and `RecordingBridge.stopRecordingFromAgent()`.

- [ ] **Step 2: Configure `rustup` with `aarch64-linux-android` and `rust-core/.cargo/config.toml`**
  Point `[target.aarch64-linux-android]` `linker` to `/home/ryno/Android/Sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android29-clang`.

- [ ] **Step 3: Build `libclass_recorder_core.so` and run `cargo test`**
  Run: `cargo test --manifest-path rust-core/Cargo.toml && cargo build --manifest-path rust-core/Cargo.toml --release --target aarch64-linux-android`
  Expected: `target/aarch64-linux-android/release/libclass_recorder_core.so` created and all host tests pass.

- [ ] **Step 4: Commit**
  ```bash
  git add rust-core/src/jni_bridge.rs rust-core/src/lib.rs rust-core/.cargo/config.toml
  git commit -m "feat(rust-core): add JNI bridge and aarch64-linux-android NDK build config"
  ```

---

### Task 5: Android Project Scaffold, Material 3 Theme & JNI Wrapper

**Files:**
- Create: `android-app/settings.gradle.kts`
- Create: `android-app/build.gradle.kts`
- Create: `android-app/gradle.properties`
- Create: `android-app/app/build.gradle.kts`
- Create: `android-app/app/src/main/AndroidManifest.xml`
- Create: `android-app/app/src/main/res/values/strings.xml`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/core/RustCore.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/core/Models.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Theme.kt`
- Copy: `rust-core/target/aarch64-linux-android/release/libclass_recorder_core.so` -> `android-app/app/src/main/jniLibs/arm64-v8a/libclass_recorder_core.so`

- [ ] **Step 1: Create Gradle project files & `gradlew` (using cached Gradle 8.14)**
  Configure Kotlin + Compose plugin, Material 3, Material Icons Extended, `compileSdk = 35`, `minSdk = 29`, `targetSdk = 35`.

- [ ] **Step 2: Create `AndroidManifest.xml`, `RustCore.kt`, `Models.kt`, and `Theme.kt`**
  Include `RECORD_AUDIO`, `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`, and `ClassRecorderTheme` with Material 3 Dynamic Color.

- [ ] **Step 3: Verify Kotlin compilation**
  Run: `./gradlew :app:compileDebugKotlin` in `android-app/`
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**
  ```bash
  git add android-app/
  git commit -m "feat(android): scaffold Material 3 Compose app and Rust JNI wrapper"
  ```

---

### Task 6: Foreground `RecordingService` & `MainViewModel`

**Files:**
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/service/RecordingService.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/MainViewModel.kt`
- Modify: `android-app/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Implement `RecordingService.kt`**
  `Service` with `foregroundServiceType="microphone"`, `MediaRecorder` outputting `.m4a` (AAC 44.1kHz 128kbps), `StateFlow` for `isRecording`, `isPaused`, `elapsedMs`, `amplitude`, `activeRecording`, plus Stop/Pause notification actions and `RecordingBridge` integration for MCP-triggered recordings.

- [ ] **Step 2: Implement `MainViewModel.kt`**
  Initializes `RustCore`, exposes UI state (`currentClass`, `schedule`, `recordings`, `mcpStatus`, `lanIp`, `errorMessage`), and handles `syncSchedule`, `startRecording`, `stopRecording`, `togglePause`, `updateNotes`, `deleteRecording`, `toggleMcpServer`.

- [ ] **Step 3: Verify compilation**
  Run: `./gradlew :app:compileDebugKotlin`
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**
  ```bash
  git add android-app/app/src/main/
  git commit -m "feat(android): add foreground RecordingService and MainViewModel"
  ```

---

### Task 7: Material 3 Screens (Record, Schedule, Library & MCP Connect)

**Files:**
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/RecordScreen.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/ScheduleScreen.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/LibraryScreen.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/MainActivity.kt`

- [ ] **Step 1: Implement `RecordScreen.kt`**
  Current Class Material 3 card (auto-detected or manual override via `ModalBottomSheet`), live waveform bars driven by `amplitude`, elapsed timer, `LargeFloatingActionButton` for Record/Stop + Pause button, and clickable MCP status chip.

- [ ] **Step 2: Implement `ScheduleScreen.kt`**
  Today / Weekly NUTC schedule view with course color indicators, tap-to-record-for-class action, and **Schedule Sources** sheet supporting `uni-campus-app` share URL/code (defaulting to `https://uni-campus-app.pages.dev/s/qVaaFS5A`) and `Personal-NUTC-Eportal` URL/JSON.

- [ ] **Step 3: Implement `LibraryScreen.kt` and `MainActivity.kt`**
  Course-grouped recordings list, inline `MediaPlayer` playback, Recording Detail & Agent Notes bottom sheet, and **MCP Connect** sheet (Start/Stop toggle, port `8765`, LAN URL, copyable `adb forward tcp:8765 tcp:8765` command). Wire 3-tab Material 3 `NavigationBar` and runtime `RECORD_AUDIO` permission launcher in `MainActivity.kt`.

- [ ] **Step 4: Build debug APK**
  Run: `./gradlew :app:assembleDebug`
  Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` generated.

- [ ] **Step 5: Commit**
  ```bash
  git add android-app/app/src/main/
  git commit -m "feat(android): implement Material 3 Record, Schedule, and Library/MCP screens"
  ```

---

### Task 8: Pixel 7 On-Device & Live MCP Verification

- [ ] **Step 1: Install APK on connected Pixel 7 (`28221FDH2001LB`)**
  Run: `adb -s 28221FDH2001LB install -r android-app/app/build/outputs/apk/debug/app-debug.apk`
  Grant `RECORD_AUDIO` and launch `com.ryno.classrecorder/.MainActivity`.

- [ ] **Step 2: Forward MCP port and test live MCP endpoints from laptop**
  Run: `adb -s 28221FDH2001LB forward tcp:8765 tcp:8765`
  Verify `GET http://127.0.0.1:8765/health`, `POST /mcp` (`initialize`, `tools/list`, `sync_schedule` with `qVaaFS5A`, `get_schedule`, `start_recording`, `stop_recording`, `list_recordings`, `update_recording_notes`).

- [ ] **Step 3: Capture screenshot/logcat evidence and verify no crashes**
  Confirm `adb logcat` has no fatal exceptions and UI renders cleanly on Pixel 7.
