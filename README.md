# Class Recorder

Schedule-aware Android lecture recorder built with Jetpack Compose (**DSH Go Mist Glass** design system) and an embedded **Rust** SQLite + Model Context Protocol (MCP) server.

## Highlights

- **Timetable-Aware Capture**: Syncs `uni-campus-app` share links/codes and `Personal-NUTC-Eportal` JSON schedules, automatically tagging recordings with the active course, room, instructor, and period.
- **Course-Organized Storage**: Saves 44.1 kHz / 128 kbps AAC `.m4a` recordings into sanitized per-course folders with customizable filename templates (`{course}`, `{date}`, `{time}`, `{lesson_number}`, `{room}`).
- **On-Device MCP Server (`:8765`)**: Embedded Rust `axum` / `tokio` Streamable HTTP + SSE MCP server (`POST /mcp`, `GET /mcp/sse`, `GET /health`) exposing live recorder controls, schedule lookup, and synced Markdown lecture notes to desktop or phone AI agents over LAN or `adb forward`.
- **Mist Glass UI & OKLCH Theming**: Translucent `GlassSurface` materials, Android 12+ `RenderEffect` backdrop blur, real `MediaCodec` PCM waveform scrubbing, 6 perceptual OKLCH accent presets (*Mist*, *Daiqing*, *Dusk*, *Turquoise*, *Amber*, *Graphite*), and contrast-guarded `Follow wallpaper` Material You integration.
- **Android 16 & HyperOS Live Updates**: Foreground recording service with live timer/status island notifications and optional one-tap Shizuku `setCanBePromoted` grant.

## Repository Structure

```text
├── android-app/          # Jetpack Compose Android app (Kotlin 2.0, minSdk 29, compileSdk 36)
├── rust-core/            # Rust core library (SQLite, timetable parser, JNI bridge, Axum MCP server)
├── scripts/              # Portable NDK build helper for rust-core -> jniLibs
├── PRODUCT.md            # Product overview, core workflows, and technical boundaries
├── DESIGN.md             # DSH Go "Mist Glass" design system specification & tokens
└── .impeccable/          # Machine-readable design tokens (design.json) & surface brief
```

## Prerequisites

- **JDK 17** (`JAVA_HOME` pointing to JDK 17+)
- **Android SDK** (`compileSdk 36`, **Android NDK r27+** installed under `$ANDROID_HOME/ndk/`)
- **Rust toolchain** with Android target:
  ```bash
  rustup target add aarch64-linux-android
  ```

## Building & Testing

### 1. Build the Rust Core (`libclass_recorder_core.so`)

Run the Rust unit tests and compile the `arm64-v8a` JNI library into `android-app/app/src/main/jniLibs/`:

```bash
cargo test --manifest-path rust-core/Cargo.toml
./scripts/build-rust-android.sh
```

*(Pass `--with-x86_64` after `rustup target add x86_64-linux-android` if running native calls on an `x86_64` emulator, and add `"x86_64"` to `abiFilters` in `android-app/app/build.gradle.kts`.)*

### 2. Build & Test the Android App

Two product flavors are configured in `android-app/app/build.gradle.kts`:

- **`classrecorder`** (`com.ryno.classrecorder`) — main app install.
- **`classrecorderTest`** (`com.ryno.classrecorder.uitest`) — side-by-side UI test install with isolated app data so real recordings are never touched during testing.

```bash
cd android-app

# Run JVM unit tests (OKLCH contrast, FNV-1a identity hashes, formatters, preferences)
./gradlew :app:testClassrecorderTestDebugUnitTest

# Assemble debug APKs
./gradlew :app:assembleClassrecorderDebug :app:assembleClassrecorderTestDebug
```

## Connecting an MCP Client

Enable the MCP server in **Settings** (default port `8765`, optional Bearer token). For local USB debugging:

```bash
adb forward tcp:8765 tcp:8765
curl http://127.0.0.1:8765/health
```

Point your MCP client at `http://127.0.0.1:8765/mcp` (or `http://<device-lan-ip>:8765/mcp` on trusted Wi-Fi). Exposed tools:

- `get_schedule` / `sync_schedule` / `get_current_class`
- `get_recorder_status` / `start_recording` / `pause_recording` / `resume_recording` / `stop_recording`
- `list_recordings` / `get_recording` / `update_recording_notes` / `rename_recording` / `delete_recording`
