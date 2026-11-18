---
version: 1
slug: "va-com-ryno-classrecorder-mainactivity-kt-ececd0c7"
primary_target: "android-app/app/src/main/java/com/ryno/classrecorder/MainActivity.kt"
related_targets: ["android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/RecordScreen.kt","android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/ScheduleScreen.kt","android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/LibraryScreen.kt","android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/PlaybackScreen.kt","android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/SettingsScreen.kt"]
---

# Surface Brief: LectureShelf App Shell & Screens

- **Scope**: Full Android app UI (`MainActivity.kt`, `RecordScreen.kt`, `ScheduleScreen.kt`, `LibraryScreen.kt`, `PlaybackScreen.kt`, `SettingsScreen.kt`)
- **Mode**: Operate
- **Audience & Job**: University students capturing, browsing, and reviewing schedule-matched `.m4a` lecture recordings and managing the on-device MCP server.
- **Constraints**: Preserve `RustCore` JNI, `RecordingService` foreground & live-update notifications, `.uitest` side-by-side flavor, `>= 48dp` Android touch targets, `1.3x` font-scale layout resilience, and system Reduce Motion.

## Direction contract

THESIS: LectureShelf operates as a calm, pocket-sized DSH Go "Mist Glass" workbench rather than a loud voice-memo utility or opaque Material slab stack, refusing heavy app-bar bands and raw neon timetable fills.

OWN-WORLD: Low-saturation OKLCH Mist Glass across Light ("Morning Mist") and Dark ("Slate Night") modes with 6 presets + wallpaper accent (70% gradient/glass ground, 25% ink/paper type and 0.5dp hairlines, <=5% accent), 4 glass tiers (`chrome` floating frosted capsules with top-45% rim and outer-only shadow, `sheet` 28dp modals, `card` 14/20dp translucent groups, `pill` expandable capsules), 8 FNV-1a muted identity pairs, and recessed `codeWell` mono blocks.

STORY: Opening the app between lectures, the student sees the auto-detected class, room, and MCP agent status on a clean glass overview card, switches seamlessly across Recorder, Schedule, and Recordings via the floating chrome pill, and starts or reviews recordings with one thumb.

FIRST VIEWPORT: Full-bleed `AppBackdrop` (vertical gradient + two soft corner radial glows); top floating 56dp chrome row (12dp inset, no bar slab) holding the `Recorder · Schedule · Recordings` sliding `SegmentedPill` and a 44dp circular `GlassIconButton` (`⚙`); center `OverviewCard` displaying `StatusChip`, active/upcoming course headline, slot/room metadata, MCP status footer, and quick course `CapsuleChoiceChip`s; bottom floating `chrome` composer dock with target summary on the left and circular brick-rose record trigger on the right.

FORM: Top Chrome Pill + Composer-style Recorder Dock in DSH Go Mist Glass; `seed: none (pinned)` — `"redesign the entire UI with dsh go's UI"`, `"I think the first one speaks well with me"`, `"Full DSH Go Appearance section: Light / Dark / System mode + the 6 DSH Go accent presets + Follow Wallpaper (persisted across launches)"`.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
