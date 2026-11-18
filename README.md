# Class Recorder

**Class Recorder** is an Android lecture recorder made for students. Instead of leaving you with a pile of `Recording_042.m4a` files at finals week, it syncs with your class timetable, automatically tags and sorts every recording by course, and lets you review audio alongside lecture notes.

---

## Why Use Class Recorder?

- **Knows What Class You're In**: Import your `uni-campus` share link or NUTC timetable once. Open the app during class (or up to 10 minutes early) and the right course, room, and period are already selected.
- **Stays Out of Your Way**: Start, pause, resume, or finish with one thumb. Keeps recording reliably in the background with your screen locked, with live timer controls in your notification shade — plus **Android 16 Live Updates** and **Xiaomi HyperOS Island** support.
- **Auto-Sorted Lecture Library**: Every `.m4a` recording is saved into its own course folder using a filename format you control. Filter by course, search across titles and notes, scrub through the visual waveform, adjust playback speed (`0.75×` to `2.0×`), or long-press to export multiple lectures as a `.zip`.
- **Easy on the Eyes**: Built with the **Mist Glass** interface — choose Light, Dark, or System mode, pick from 6 soft color themes (*Mist*, *Daiqing*, *Dusk*, *Turquoise*, *Amber*, *Graphite*), or match your phone's wallpaper.
- **Works with Your AI Study Tools *(Optional)***: Flip on the built-in **MCP Server** in Settings to let desktop or mobile AI assistants check your schedule, control recordings, or save structured Markdown summaries straight onto a lecture.

---

## Quick Start

### 1. Install the App
1. Install the Class Recorder `.apk` on your Android phone (**Android 10+**).
2. Open the app and allow **Microphone** access (and **Notifications** so you can see and control recordings while the screen is off).

### 2. Import Your Class Schedule
1. Switch to the **Schedule** tab (or open **Settings** in the top-right corner).
2. Tap **Sync** and paste your `uni-campus` share link (for example `https://uni-campus-app.pages.dev/s/...`), share code, or NUTC timetable JSON link.
3. Your weekly classes will appear by day — tap any class card if you want to start recording for it right away.

### 3. Record a Lecture
1. On the **Recorder** tab, check the class card at the top:
   - If a class is currently in session, it is selected automatically.
   - Outside class hours (or if a class moved rooms), tap the card to pick any course manually or record as **Unscheduled**.
2. Tap the **Record** button at the bottom.
3. While recording, you can **Pause**, **Resume**, **Discard**, or **Finish** from either the app or the lock-screen notification.

### 4. Listen Back, Take Notes & Export
1. Open the **Recordings** tab to browse lectures grouped by day, or tap a course chip to see just one subject.
2. Tap any recording to open the player:
   - Drag across the waveform to jump to key moments.
   - Change playback speed between `0.75×`, `1×`, `1.25×`, `1.5×`, and `2×`.
   - Jot down timestamps or summaries in the **Lecture Notes** box and tap **Save**.
   - Tap the **Share** icon in the top-right to send the `.m4a` file to Google Drive, Notion, Telegram, or your laptop.
3. Need to back up a whole subject? **Long-press** any recording in the list to select multiple files and export them together as a single `.zip` archive.

---

## Customizing File Names

In **Settings → Storage & Naming**, you can customize how recordings are named inside each course folder using placeholders:

| Placeholder | Example Output | Meaning |
| :--- | :--- | :--- |
| `{course}` | `Linear_Algebra` | Course name (or `Unscheduled`) |
| `{date}` | `2026-11-18` | Recording date (`YYYY-MM-DD`, `YYYYMMDD`, or `MM-DD`) |
| `{time}` | `0910` | Start time (`HHmm`) |
| `{lesson_number}` | `03` | Sequential recording count for that course |
| `{room}` | `3401` | Classroom number from your schedule |

**Default template:** `{date}_{time}_{course}_{room}` → `Linear_Algebra/2026-11-18_0910_Linear_Algebra_3401.m4a`

---

## Optional Features

### Live Island / Promoted Notifications (Android 16 & HyperOS)
Class Recorder always shows a standard recording notification out of the box. On **Android 16+** and **Xiaomi HyperOS**, Android restricts live status-bar pills ("Live Updates") unless granted via shell permission:

1. Install and start [Shizuku](https://shizuku.rikka.app/).
2. Open **Class Recorder** (or tap the prompt in **Settings**).
3. Approve the one-time Shizuku permission prompt — live recording timers will now appear directly in the status bar / HyperOS Island.

### Connecting an AI Assistant (MCP)
If you use an MCP-compatible AI app (such as Claude Code, Cursor, or a local study agent), you can connect it directly to Class Recorder:

1. Open **Settings → MCP Server** and turn it **On** (default port `8765`; you can also set an optional Bearer password).
2. **Over Wi-Fi (same network):** Point your MCP client to the URL shown on screen (`http://<your-phone-ip>:8765/mcp`).
3. **Over USB cable:** Forward the port with `adb forward tcp:8765 tcp:8765` and connect to `http://127.0.0.1:8765/mcp`.

Once connected, your assistant can look up your timetable, check what class is happening right now, start/pause/stop a recording, list past lectures, and write study notes directly into a recording's notes field.

---

<details>
<summary><strong>Building from Source (For Developers)</strong></summary>

### Project Layout
- `android-app/` — Jetpack Compose Android app (Kotlin 2.0, `minSdk 29`, `compileSdk 36`, DSH Go Mist Glass UI)
- `rust-core/` — Rust core library (`rusqlite` SQLite storage, timetable parser, JNI bridge, `axum` MCP server)
- `scripts/build-rust-android.sh` — Portable Android NDK build script for `libclass_recorder_core.so`
- `PRODUCT.md` & `DESIGN.md` — Product overview and Mist Glass design system specification

### Prerequisites
- **JDK 17+**
- **Android SDK** (`compileSdk 36`) and **Android NDK r27+** (`ANDROID_HOME` or `ANDROID_NDK_HOME` set)
- **Rust** (`rustup target add aarch64-linux-android`)

### Build Commands
```bash
# 1. Run Rust tests & build the arm64-v8a native library into jniLibs/
cargo test --manifest-path rust-core/Cargo.toml
./scripts/build-rust-android.sh

# 2. Run Android JVM unit tests & assemble debug APK
cd android-app
./gradlew :app:testClassrecorderTestDebugUnitTest :app:assembleClassrecorderDebug
```

The APK will be generated at `android-app/app/build/outputs/apk/classrecorder/debug/app-classrecorder-debug.apk`.  
*(A side-by-side `classrecorderTest` flavor with isolated app ID `com.ryno.classrecorder.uitest` is also available via `:app:assembleClassrecorderTestDebug` so UI testing never touches your real recordings.)*

</details>
