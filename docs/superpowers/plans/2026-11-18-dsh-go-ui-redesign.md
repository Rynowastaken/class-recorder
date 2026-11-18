# DSH Go ("Mist Glass") UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Class Recorder's Compose UI with DSH Go's complete "Mist Glass" design system — Light/Dark/System modes, 6 OKLCH accent presets + Follow Wallpaper, 4-tier glass surfaces, floating top `SegmentedPill` shell, Composer-style bottom record dock, and `AppPage` secondary screens — while keeping `rust-core` JNI and `RecordingService` intact.

**Architecture:** Build a pure-Kotlin OKLCH + WCAG contrast-guarded theme engine (`ui/theme/`) and a reusable DSH Go Mist Glass component kit (`ui/components/`), then rebuild all five screens (`RecordScreen`, `ScheduleScreen`, `LibraryScreen`, `PlaybackScreen`, `SettingsScreen`) and `MainActivity` on top of those primitives, deleting legacy Portal/Dusk files once migrated so every task compiles and tests cleanly on its own.

**Tech Stack:** Kotlin 2.x, Jetpack Compose (`compose-bom:2025.09.01`, `material3:1.4.0`), Android SDK 36 (`minSdk = 29`, `targetSdk = 35`), JUnit 4 (`junit:4.13.2`), Gradle 8.14 (`/home/ryno/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle`, `ANDROID_HOME=/home/ryno/Android/Sdk`), Impeccable v4.9.2.

**Spec:** `docs/superpowers/specs/2026-11-18-dsh-go-ui-redesign-design.md`

## Global Constraints

- Gradle command on this machine (no wrapper script checked in): `ANDROID_HOME=/home/ryno/Android/Sdk /home/ryno/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle -p /home/ryno/project/class-recorder/android-app`
- Do NOT modify `rust-core/`, `RustCore.kt`, `Models.kt`, `PromotionGrant.kt`, or `RecordingService.kt`.
- Every interactive control must provide at least a `48.dp × 48.dp` touch target (`Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` or `minimumInteractiveComponentSize()`), even when its visual glass shape is `44.dp` or `34.dp`.
- Typography uses `sp` units exclusively (never `px`) and enables `fontFeatureSettings = "tnum"` on every time, date, duration, and count.
- Color ratio holds `70%` neutral gradient/glass, `25%` ink/paper text & `0.5.dp` hairlines, `<= 5%` `primary` accent; `surfaceTint` is `Color.Transparent`; `Card` and `Pill` glass tiers keep `blurSigma = 0f` (no per-item offscreen blur in scrolling lists).
- Read `/home/ryno/project/class-recorder/.agents/skills/impeccable/reference/craft-floor.md` immediately before writing any UI component or screen code (Task 3 onward), and never copy the Impeccable `Direction contract` text into Kotlin source comments or strings.
- Every hand-written `.kt` source file under `android-app/app/src/main/java/` must stay `< 1,000` lines.
- All emulator/device installs and UI screenshots use the side-by-side `classrecorderTest` flavor (`com.ryno.classrecorder.uitest`) so user data under `com.ryno.classrecorder` is never touched.

## Review Focus

- **Extreme wallpaper seed colors (`#FFFFFF`, `#000000`, `#FFFF00`, `#FF00FF`):** `AppAccent.derive()` must cap OKLCH chroma at `<= 0.13` and binary-search lightness via `AppAccent.guard()` without NaN or infinite loops so `contrastRatio(primary, surfaceContainerHighest) >= 3.0` and `contrastRatio(onPrimary, primary) >= 4.5` hold in both Light and Dark schemes (tested in Task 1).
- **Blank, whitespace, and CJK/symbol course names (`""`, `"   "`, `"程式設計與應用 (一)"`) in identity hashing:** `DshColors.identityIndex()` must normalize blank keys to `"Uncategorized"` and use unsigned 32-bit FNV-1a (`0x811c9dc5u`, prime `0x01000193u`) so modulo never returns a negative index (tested in Task 1).
- **Corrupted or legacy `SharedPreferences` values (`"portal"`, `""`, `"invalid"`):** `AppearancePreferences.parseThemeMode()` and `parseAccentId()` must fall back cleanly to `DshThemeMode.SYSTEM` and `"mist"` without throwing (tested in Task 2).
- **Inconsistent schedule time-range separators (`"08:10~09:00"`, `"08:10-09:00"`, `"08:10 - 09:00"`, `""`) and filename template typos (`{lession number}`, `{lesson_number}`, `{course}`):** `formatTimeRange()` must normalize ranges to en-dash `"08:10–09:00"` while preserving blank/single strings, and `previewFilename()` must substitute all documented tokens cleanly (tested in Task 4).
- **`1.3x` system font scale + Light/Dark contrast on real Android surfaces:** `SegmentedPill` and `SettingsSegmentTile` must not clip or wrap CJK labels vertically at `font_scale = 1.3`, and every `Text` over custom glass must resolve `LocalContentColor` / explicit theme color in both Light and Dark modes (verified in Task 6).

---

### Task 1: OKLCH Color Engine, 6 Presets, NeutralTones, DshPalette, DshSurfaces & DshColors

**Files:**
- Modify: `android-app/app/build.gradle.kts:73-105`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Oklch.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/AppAccent.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/DshPalette.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/DshSurfaces.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/DshColors.kt`
- Test: `android-app/app/src/test/java/com/ryno/classrecorder/ui/theme/OklchContrastTest.kt`
- Test: `android-app/app/src/test/java/com/ryno/classrecorder/ui/theme/IdentityHashTest.kt`

**Interfaces:**
- Produces:
  - `data class Oklch(val l: Double, val c: Double, val h: Double)` with `Oklch.fromColor(Color): Oklch`, `toColor(): Color`, `retint(color: Color, hueShift: Double, chromaScale: Double): Color`, `contrastRatio(a: Color, b: Color): Double`
  - `data class AppAccent(...)` with `AppAccent.Mist`, `Daiqing`, `Dusk`, `Turquoise`, `Amber`, `Graphite`, `presets: List<AppAccent>`, `preset(id: String?): AppAccent`, `derive(pick: Color, id: String = "wallpaper", label: String = "Wallpaper"): AppAccent`, `guard(...)`
  - `data class NeutralTones(...)` with `NeutralTones.Defaults`, `forAccent(AppAccent): NeutralTones`
  - `object DshPalette` with `lightScheme(AppAccent): ColorScheme`, `darkScheme(AppAccent): ColorScheme`
  - `enum class GlassTier { Chrome, Sheet, Card, Pill }`, `data class GlassStyle(...)`, `data class DshSurfaces(...)` with `DshSurfaces.light(AppAccent, blurEnabled: Boolean = true)`, `DshSurfaces.dark(AppAccent, blurEnabled: Boolean = true)`
  - `enum class StatusTone { Idle, Ok, Warn, Error, Busy }`, `data class DshColors(...)` with `DshColors.forAccent(darkTheme: Boolean, accent: AppAccent): DshColors`, `DshColors.identityIndex(key: String, slots: Int = 8): Int`, `DshColors.identityPair(key: String): Pair<Color, Color>`

- [ ] **Step 1: Add JUnit 4 test dependency in `android-app/app/build.gradle.kts` and write failing unit tests**

Add `testImplementation("junit:junit:4.13.2")` to `dependencies` in `android-app/app/build.gradle.kts`. Create `OklchContrastTest.kt` and `IdentityHashTest.kt`:

```kotlin
// OklchContrastTest.kt
package com.ryno.classrecorder.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class OklchContrastTest {
    @Test
    fun allPresetsMeetWcagContrastInLightAndDark() {
        for (accent in AppAccent.presets) {
            val light = DshPalette.lightScheme(accent)
            val dark = DshPalette.darkScheme(accent)
            assertTrue("light body ${accent.id}", contrastRatio(light.onSurface, light.surface) >= 10.0)
            assertTrue("dark body ${accent.id}", contrastRatio(dark.onSurface, dark.surface) >= 9.5)
            assertTrue("light onPrimary ${accent.id}", contrastRatio(light.onPrimary, light.primary) >= 4.5)
            assertTrue("dark onPrimary ${accent.id}", contrastRatio(dark.onPrimary, dark.primary) >= 4.5)
            assertTrue("light primary vs surface ${accent.id}", contrastRatio(light.primary, light.surfaceContainerHighest) >= 3.0)
            assertTrue("dark primary vs surface ${accent.id}", contrastRatio(dark.primary, dark.surfaceContainerHighest) >= 3.0)
        }
    }

    @Test
    fun extremeWallpaperSeedsGuardCleanly() {
        val extremes = listOf(Color.White, Color.Black, Color(0xFFFFFF00), Color(0xFFFF00FF), Color(0xFF00FFAA))
        for (seed in extremes) {
            val derived = AppAccent.derive(seed)
            assertTrue(Oklch.fromColor(derived.seed).c <= AppAccent.MAX_CHROMA + 0.01)
            val light = DshPalette.lightScheme(derived)
            val dark = DshPalette.darkScheme(derived)
            assertTrue(contrastRatio(light.onPrimary, light.primary) >= 4.45)
            assertTrue(contrastRatio(dark.onPrimary, dark.primary) >= 4.45)
            assertTrue(contrastRatio(light.primary, light.surfaceContainerHighest) >= 2.95)
            assertTrue(contrastRatio(dark.primary, dark.surfaceContainerHighest) >= 2.95)
        }
    }
}
```

```kotlin
// IdentityHashTest.kt
package com.ryno.classrecorder.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityHashTest {
    @Test
    fun blankKeysNormalizeAndNeverReturnNegativeIndex() {
        val expected = DshColors.identityIndex("Uncategorized")
        assertEquals(expected, DshColors.identityIndex(""))
        assertEquals(expected, DshColors.identityIndex("   "))
        val samples = listOf("程式設計與應用 (一)", "民法", "大學之道", "體育", "服務與學習", "C++/Java")
        for (s in samples) {
            val idx = DshColors.identityIndex(s, 8)
            assertTrue("idx in 0..7 for $s", idx in 0..7)
            assertEquals(idx, DshColors.identityIndex(s, 8))
        }
    }
}
```

- [ ] **Step 2: Run unit tests to verify they fail**

Run: `ANDROID_HOME=/home/ryno/Android/Sdk /home/ryno/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle -p /home/ryno/project/class-recorder/android-app :app:testClassrecorderTestDebugUnitTest`  
Expected: FAIL with unresolved references (`AppAccent`, `DshPalette`, `Oklch`, `DshColors`).

- [ ] **Step 3: Implement `Oklch.kt`, `AppAccent.kt`, `DshPalette.kt`, `DshSurfaces.kt`, and `DshColors.kt`**

Port exact OKLCH sRGB/OKLab conversion + gamut-clamped `toColor()`, `retint()`, and WCAG `contrastRatio()` in `Oklch.kt`; exact hex constants for `Mist`, `Daiqing`, `Dusk`, `Turquoise`, `Amber`, `Graphite` plus `AppAccent.derive()` and 24-step binary-search `guard()` in `AppAccent.kt`; `NeutralTones` (`baseHue = 266.0`) and `DshPalette.lightScheme()` / `darkScheme()` (`surfaceTint = Color.Transparent`) in `DshPalette.kt`; `GlassTier`, `GlassStyle`, and `DshSurfaces.light()` / `dark()` in `DshSurfaces.kt`; and `StatusTone` + `DshColors` (with the 8 Light/Dark `identity` & `onIdentity` pairs, `codeWellBg`/`codeWellBorder`/`codeWellFg`, `success`/`warning`/`danger`/`info`, and unsigned 32-bit FNV-1a `identityIndex`) in `DshColors.kt` using the exact hex values from Spec §3.1–§3.2.

- [ ] **Step 4: Run unit tests to verify they pass**

Run: `ANDROID_HOME=/home/ryno/Android/Sdk /home/ryno/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle -p /home/ryno/project/class-recorder/android-app :app:testClassrecorderTestDebugUnitTest`  
Expected: `BUILD SUCCESSFUL`, all tests in `OklchContrastTest` and `IdentityHashTest` PASS.

- [ ] **Step 5: Commit**

```bash
git -C /home/ryno/project/class-recorder add \
  android-app/app/build.gradle.kts \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Oklch.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/AppAccent.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/DshPalette.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/DshSurfaces.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/DshColors.kt \
  android-app/app/src/test/java/com/ryno/classrecorder/ui/theme/OklchContrastTest.kt \
  android-app/app/src/test/java/com/ryno/classrecorder/ui/theme/IdentityHashTest.kt
git -C /home/ryno/project/class-recorder commit -m "feat(theme): add DSH Go OKLCH palette, 6 presets, surfaces, and semantic colors"
```

---

### Task 2: Typography, Shapes, Motion, Appearance Persistence & `ClassRecorderTheme`

**Files:**
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/AppearancePreferences.kt`
- Modify: `android-app/app/src/main/java/com/ryno/classrecorder/ui/MainViewModel.kt:30-135`
- Modify: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Shape.kt`
- Modify: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Type.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Motion.kt`
- Modify: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Theme.kt`
- Test: `android-app/app/src/test/java/com/ryno/classrecorder/ui/AppearancePreferencesTest.kt`

**Interfaces:**
- Consumes: `AppAccent`, `DshPalette`, `DshSurfaces`, `DshColors` from Task 1.
- Produces:
  - `enum class DshThemeMode { SYSTEM, LIGHT, DARK }`
  - `object AppearancePreferences` with `parseThemeMode(raw: String?): DshThemeMode`, `parseAccentId(raw: String?): String`, `load(Context): Pair<DshThemeMode, String>`, `saveThemeMode(Context, DshThemeMode)`, `saveAccentId(Context, String)`
  - `MainUiState` extended with `val themeMode: DshThemeMode = DshThemeMode.SYSTEM`, `val accentId: String = "mist"`, and `MainViewModel.setThemeMode(DshThemeMode)` + `setAccentId(String)`
  - `object AppShape` (`xs=6.dp`, `sm=10.dp`, `md=14.dp`, `lg=20.dp`, `xl=28.dp`, `pill=CircleShape`) & `object AppSpace` (`gutter=16.dp`, `sheetGutter=20.dp`, `readingWidth=720.dp`, `floatInset=12.dp`)
  - `DshTypography: Typography`, `data class MonoTypography(val mono: TextStyle, val monoSmall: TextStyle, val timer: TextStyle, val timerCompact: TextStyle, val seekLabel: TextStyle, val timerCaption: TextStyle)`
  - `object DshMotion` + `LocalReducedMotion: ProvidableCompositionLocal<Boolean>` + `@Composable fun rememberReducedMotion(): Boolean`
  - `@Composable fun ClassRecorderTheme(themeMode: DshThemeMode = DshThemeMode.SYSTEM, accentId: String = "mist", content: @Composable () -> Unit)` and `ClassRecorderTheme.surfaces: DshSurfaces`, `ClassRecorderTheme.dshColors: DshColors`, `ClassRecorderTheme.mono: MonoTypography` (preserving temporary `ClassRecorderTheme.colors` compatibility shim until Task 5 removes legacy callers)

- [ ] **Step 1: Write failing unit test in `AppearancePreferencesTest.kt`**

```kotlin
package com.ryno.classrecorder.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearancePreferencesTest {
    @Test
    fun unknownOrBlankStringsFallbackSafely() {
        assertEquals(DshThemeMode.SYSTEM, AppearancePreferences.parseThemeMode(null))
        assertEquals(DshThemeMode.SYSTEM, AppearancePreferences.parseThemeMode("portal"))
        assertEquals(DshThemeMode.LIGHT, AppearancePreferences.parseThemeMode("LIGHT"))
        assertEquals(DshThemeMode.DARK, AppearancePreferences.parseThemeMode("dark"))

        assertEquals("mist", AppearancePreferences.parseAccentId(null))
        assertEquals("mist", AppearancePreferences.parseAccentId("neon_pink"))
        assertEquals("daiqing", AppearancePreferences.parseAccentId("daiqing"))
        assertEquals("wallpaper", AppearancePreferences.parseAccentId("wallpaper"))
    }
}
```

- [ ] **Step 2: Run unit test to verify it fails**

Run: `ANDROID_HOME=/home/ryno/Android/Sdk /home/ryno/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle -p /home/ryno/project/class-recorder/android-app :app:testClassrecorderTestDebugUnitTest --tests com.ryno.classrecorder.ui.AppearancePreferencesTest`  
Expected: FAIL (`AppearancePreferences` / `DshThemeMode` unresolved).

- [ ] **Step 3: Implement `AppearancePreferences.kt`, `Shape.kt`, `Type.kt`, `Motion.kt`, `Theme.kt`, and update `MainViewModel.kt`**

- In `AppearancePreferences.kt`, store `"theme_mode"` and `"accent_id"` in `SharedPreferences("dsh_appearance", Context.MODE_PRIVATE)`, validating `accentId` against `AppAccent.presets.map { it.id } + "wallpaper"`.
- In `MainViewModel.kt`, load `AppearancePreferences.load(ctx)` synchronously during `_ui` initialization so cold start never flashes the wrong theme, and add `setThemeMode(mode: DshThemeMode)` and `setAccentId(id: String)`.
- Update `Shape.kt` (`AppShape`, `AppSpace`), `Type.kt` (`DshTypography`, `MonoTypography`, keeping existing aliases compiling), `Motion.kt` (`DshMotion`, `LocalReducedMotion` reading `Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f`), and `Theme.kt` (resolving `"wallpaper"` on API 31+ via `ContextCompat.getColor(context, android.R.color.system_accent1_500)` passed through `AppAccent.derive()`, updating `WindowCompat.getInsetsController` `isAppearanceLightStatusBars = !isDark` and `isAppearanceLightNavigationBars = !isDark`, and providing `LocalDshSurfaces` + `LocalDshColors`).

- [ ] **Step 4: Run unit tests and Kotlin compile check**

Run: `ANDROID_HOME=/home/ryno/Android/Sdk /home/ryno/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle -p /home/ryno/project/class-recorder/android-app :app:testClassrecorderTestDebugUnitTest :app:compileClassrecorderTestDebugKotlin`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git -C /home/ryno/project/class-recorder add \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/AppearancePreferences.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/MainViewModel.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Shape.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Type.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Motion.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Theme.kt \
  android-app/app/src/test/java/com/ryno/classrecorder/ui/AppearancePreferencesTest.kt
git -C /home/ryno/project/class-recorder commit -m "feat(theme): wire DshTypography, AppShape, DshMotion, and persisted Appearance state"
```

---

### Task 3: DSH Go Mist Glass Component Kit (`ui/components/` + `WaveformCanvas.kt`)

**Files:**
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/components/AppBackdrop.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/components/GlassSurface.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/components/GlassControls.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/components/SegmentedPill.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/components/AppPage.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/components/AppSheet.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/components/CardsAndRows.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/WaveformCanvas.kt`

**Interfaces:**
- Consumes: `ClassRecorderTheme`, `GlassTier`, `GlassStyle`, `StatusTone`, `AppShape`, `AppSpace`, `DshMotion`, `LocalReducedMotion` from Tasks 1–2.
- Produces:
  - `@Composable fun DshBackdrop(modifier: Modifier = Modifier, glow: Boolean = true, content: @Composable BoxScope.() -> Unit)` and `@Composable fun BackdropVeil(fromTop: Boolean, solidHeight: Dp, feather: Dp = 14.dp, alpha: Float = 1f, modifier: Modifier = Modifier)`
  - `@Composable fun GlassSurface(modifier: Modifier = Modifier, tier: GlassTier = GlassTier.Card, shape: Shape? = null, circle: Boolean = false, stadium: Boolean = false, styleOverride: GlassStyle? = null, onClick: (() -> Unit)? = null, onLongClick: (() -> Unit)? = null, contentPadding: PaddingValues = PaddingValues(0.dp), content: @Composable BoxScope.() -> Unit)` (sets `LocalContentColor` to `MaterialTheme.colorScheme.onSurface`)
  - `@Composable fun GlassIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, tier: GlassTier = GlassTier.Chrome, visualSize: Dp = 44.dp, iconSize: Dp = 20.dp, enabled: Boolean = true, badgeTone: StatusTone? = null)` (guarantees `>= 48.dp` touch box)
  - `@Composable fun GlassPill(summary: @Composable RowScope.() -> Unit, modifier: Modifier = Modifier, fillWidth: Boolean = true, onClick: (() -> Unit)? = null, expandedContent: (@Composable ColumnScope.() -> Unit)? = null)`
  - `@Composable fun StatusDot(tone: StatusTone, size: Dp = 7.dp, animated: Boolean = true)` & `@Composable fun StatusChip(tone: StatusTone, label: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null)`
  - `data class PillSegment<T>(val value: T, val label: String)`, `@Composable fun <T> SegmentedPill(segments: List<PillSegment<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, fractionalIndex: Float? = null, height: Dp = 40.dp)`, `@Composable fun CapsuleChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, dotColor: Color? = null)`, `@Composable fun SearchCapsule(query: String, onQueryChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier)`
  - `@Composable fun AppPage(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, hero: (@Composable () -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}, content: LazyListScope.() -> Unit)` & `@Composable fun PageHero(leading: (@Composable () -> Unit)? = null, title: String, subtitle: String? = null, tags: List<String> = emptyList())`
  - `@Composable fun AppSheet(onDismiss: () -> Unit, title: String? = null, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit)` & `@Composable fun SheetGroup(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`
  - `@Composable fun OverviewCard(leading: @Composable () -> Unit, title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null, footer: (@Composable RowScope.() -> Unit)? = null, onFooterClick: (() -> Unit)? = null, modifier: Modifier = Modifier)` & `@Composable fun OverviewGlyph(icon: ImageVector, identityKey: String, size: Dp = 52.dp)`
  - `@Composable fun ListPageCard(title: String? = null, footer: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`, `@Composable fun ListPageRow(...)`, `@Composable fun SettingsSwitchTile(...)`, `@Composable fun CodeWell(text: String, label: String? = null, onCopy: (() -> Unit)? = null, modifier: Modifier = Modifier)`, `@Composable fun DshTextField(...)`, `@Composable fun EmptyView(icon: ImageVector, title: String, message: String? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier)`
  - `@Composable fun LiveWaveformCanvas(...)` & `@Composable fun ScrubWaveformCanvas(...)` in `WaveformCanvas.kt`

- [ ] **Step 1: Read `impeccable/reference/craft-floor.md` before authoring any UI code**

Read `/home/ryno/project/class-recorder/.agents/skills/impeccable/reference/craft-floor.md` and apply its rules throughout Tasks 3–5.

- [ ] **Step 2: Implement `AppBackdrop.kt`, `GlassSurface.kt`, and `GlassControls.kt`**

- `DshBackdrop` paints `DshSurfaces.backdropTop -> backdropBottom` vertical gradient + radial `glowPrimary` at `(0.92f * width, 0f)` radius `0.62f * maxDim` and `glowSecondary` at `(0f, height)` radius `0.55f * maxDim`. `BackdropVeil` draws the matching gradient slice masked by an eased vertical alpha ramp.
- `GlassSurface` clips exterior shadow paths via `PathOperation.Difference` (`even-odd` outer clip), paints translucent `fill` (or `fallbackFill` when reduced transparency), draws the `0.5.dp` inner hairline and `0f..0.45f` top-to-transparent `rim` brush, provides `LocalContentColor provides MaterialTheme.colorScheme.onSurface` so child `Text` never defaults to black in dark mode, and scales content to `0.97f` on press when `onClick != null` (unless `LocalReducedMotion.current`).
- `GlassIconButton`, `GlassPill`, `StatusDot`, and `StatusChip` follow Spec §3.2 & §4.

- [ ] **Step 3: Implement `SegmentedPill.kt`, `AppPage.kt`, `AppSheet.kt`, `CardsAndRows.kt`, and `WaveformCanvas.kt`**

- `SegmentedPill` supports both discrete `selected` animation and continuous `fractionalIndex` (`currentPage + currentPageOffsetFraction` for the root `HorizontalPager`), horizontal drag-to-select with `HapticFeedbackType.TextHandleMove`, and `autoSize`/single-line scaling so CJK or `1.3x` font-scale labels never wrap vertically.
- `AppPage` uses `LazyColumn` scroll state to fade/scale `hero` or large `headlineLarge` header out over the first `60%` of header height while fading `BackdropVeil(fromTop = true)` and the compact `titleMedium` bar title in, centering content within `AppSpace.readingWidth` (`720.dp`).
- `AppSheet` wraps `ModalBottomSheet` (`containerColor = Color.Transparent`, `dragHandle = null`) inside `GlassSurface(tier = GlassTier.Sheet, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))` with `SheetDragHandle` (`36.dp × 4.dp` inside a `48.dp` touch header) and `imePadding()` + `navigationBarsPadding()`.
- `CardsAndRows.kt` implements `OverviewCard`, `OverviewGlyph`, `ListPageCard` (rows separated by `0.5.dp` `outlineVariant.copy(alpha = 0.55f)` inset hairline), `ListPageRow`, `SettingsSwitchTile`, `CodeWell`, `DshTextField`, and `EmptyView`.
- `WaveformCanvas.kt` implements `LiveWaveformCanvas` and `ScrubWaveformCanvas` parameterized purely by `playedColor`, `unplayedColor`, and `playheadColor`.

- [ ] **Step 4: Compile debug Kotlin to verify zero errors**

Run: `ANDROID_HOME=/home/ryno/Android/Sdk /home/ryno/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle -p /home/ryno/project/class-recorder/android-app :app:compileClassrecorderTestDebugKotlin`  
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git -C /home/ryno/project/class-recorder add \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/components/ \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/WaveformCanvas.kt
git -C /home/ryno/project/class-recorder commit -m "feat(ui): add DSH Go Mist Glass component kit and waveform canvases"
```

---

### Task 4: Core Workspace Screens (`RecordScreen`, `ScheduleScreen`, `LibraryScreen`) & Formatters

**Files:**
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/UiFormatters.kt`
- Modify: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/RecordScreen.kt`
- Modify: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/ScheduleScreen.kt`
- Modify: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/LibraryScreen.kt`
- Test: `android-app/app/src/test/java/com/ryno/classrecorder/ui/screens/UiFormattersTest.kt`

**Interfaces:**
- Consumes: `ui/components/*` and `WaveformCanvas.kt` from Task 3; `MainUiState` and `RecorderState`.
- Produces:
  - `object UiFormatters` with `formatDuration(ms: Long): String`, `formatTimeRange(raw: String): String`, `formatClockTime(epochMs: Long): String`, `formatTimestamp(epochMs: Long): String`, `previewFilename(template: String, dateFormat: String, courseName: String = "大學之道", room: String = "3309", teacher: String = "何昕家", slot: String = "3"): String`
  - `@Composable fun McpServerSheet(ui: MainUiState, onDismiss: () -> Unit, onToggleMcp: () -> Unit, onCopied: (String) -> Unit)` (shared `AppSheet` for Record & Library)
  - Rebuilt `RecordScreen(...)`, `ScheduleScreen(...)`, and `LibraryScreen(...)` reporting `onScrolledUnderChange: (Float) -> Unit = {}` for the root `BackdropVeil`

- [ ] **Step 1: Write failing unit test in `UiFormattersTest.kt`**

```kotlin
package com.ryno.classrecorder.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class UiFormattersTest {
    @Test
    fun normalizesTimeRangesToEnDash() {
        assertEquals("08:10–09:00", UiFormatters.formatTimeRange("08:10~09:00"))
        assertEquals("13:25–14:15", UiFormatters.formatTimeRange("13:25 - 14:15"))
        assertEquals("14:20–15:10", UiFormatters.formatTimeRange("14:20-15:10"))
        assertEquals("", UiFormatters.formatTimeRange("   "))
        assertEquals("19:00", UiFormatters.formatTimeRange("19:00"))
    }

    @Test
    fun previewsFilenameWithAllSupportedTokensAndTypos() {
        val out = UiFormatters.previewFilename(
            template = "{class} {date} - {lession number} ({room})",
            dateFormat = "yyyy-MM-dd",
            courseName = "民法",
            room = "3806"
        )
        assertEquals("民法 2026-11-18 - 1 (3806).m4a", out)
    }

    @Test
    fun formatsDurationTabular() {
        assertEquals("00:05", UiFormatters.formatDuration(5_200L))
        assertEquals("48:12", UiFormatters.formatDuration((48 * 60 + 12) * 1000L))
        assertEquals("1:02:03", UiFormatters.formatDuration((3600 + 120 + 3) * 1000L))
    }
}
```

- [ ] **Step 2: Run unit test to verify it fails**

Run: `ANDROID_HOME=/home/ryno/Android/Sdk /home/ryno/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle -p /home/ryno/project/class-recorder/android-app :app:testClassrecorderTestDebugUnitTest --tests com.ryno.classrecorder.ui.screens.UiFormattersTest`  
Expected: FAIL (`UiFormatters` unresolved).

- [ ] **Step 3: Implement `UiFormatters.kt`, `RecordScreen.kt`, `ScheduleScreen.kt`, and `LibraryScreen.kt`**

- Implement `UiFormatters` (`previewFilename` uses fixed sample date `LocalDate.of(2026, 11, 18)` for deterministic previewing).
- Rebuild `RecordScreen.kt` (`< 500` lines) per Spec §4.2: `OverviewCard` with `OverviewGlyph`, `StatusChip`, course headline, slot/room/teacher `tnum` line, MCP footer row, conditional expandable recent-agent-calls `GlassPill`, quick course `CapsuleChoiceChip` `LazyRow`, live `GlassSurface(Card)` waveform + `48.sp w300` tabular clock, floating bottom `GlassSurface(Chrome, 28.dp)` Composer dock, and `CoursePicker` / `McpServerSheet` `AppSheet`s.
- Rebuild `ScheduleScreen.kt` (`< 500` lines) per Spec §4.3: header with `GlassIconButton` refresh/source controls, `96.dp` 7-day `GlassSurface(Card, 20.dp)` week strip with spring-animated `primary` selection capsule and FNV-1a `identity` pips, class rows using `UiFormatters.formatTimeRange`, `StatusChip` (`NOW`/`NEXT`), `primary` mic button only on `NOW` (`pill`-glass mic on others), `EmptyView` empty states, and `ScheduleSource` `AppSheet`.
- Rebuild `LibraryScreen.kt` (`< 650` lines) per Spec §4.4: `SearchCapsule` + `GlassIconButton`s (`Select`, `Export`, `MCP`), `CapsuleChoiceChip` course filter strip, day-grouped `ListPageCard`s with `ListPageRow` items, inline `MediaPlayer` preview toggle, long-press selection mode + floating `Chrome` `SelectionDock`, `.zip` export launcher + `Export` / `Notes` / `McpServerSheet` `AppSheet`s.

- [ ] **Step 4: Run unit tests and Kotlin compile check**

Run: `ANDROID_HOME=/home/ryno/Android/Sdk /home/ryno/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle -p /home/ryno/project/class-recorder/android-app :app:testClassrecorderTestDebugUnitTest :app:compileClassrecorderTestDebugKotlin`  
Expected: `BUILD SUCCESSFUL`, all unit tests PASS.

- [ ] **Step 5: Commit**

```bash
git -C /home/ryno/project/class-recorder add \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/UiFormatters.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/RecordScreen.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/ScheduleScreen.kt \
  android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/LibraryScreen.kt \
  android-app/app/src/test/java/com/ryno/classrecorder/ui/screens/UiFormattersTest.kt
git -C /home/ryno/project/class-recorder commit -m "feat(screens): rebuild Record, Schedule, and Library screens in DSH Go Mist Glass"
```

---

### Task 5: `PlaybackScreen`, `SettingsScreen`, Floating Shell (`MainActivity`) & Legacy Cleanup

**Files:**
- Modify: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/PlaybackScreen.kt`
- Create: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/SettingsScreen.kt`
- Modify: `android-app/app/src/main/java/com/ryno/classrecorder/MainActivity.kt`
- Modify: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Theme.kt`
- Delete: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/SettingsSheet.kt`
- Delete: `android-app/app/src/main/java/com/ryno/classrecorder/ui/screens/ExpressiveScaffold.kt`
- Delete: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Color.kt`
- Delete: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Backdrop.kt`
- Delete: `android-app/app/src/main/java/com/ryno/classrecorder/ui/theme/Expressive.kt`

**Interfaces:**
- Consumes: `ui/theme/*`, `ui/components/*`, `UiFormatters`, `RecordScreen`, `ScheduleScreen`, `LibraryScreen` from Tasks 1–4.
- Produces:
  - `@Composable fun PlaybackScreen(recording: RecordingRecord, onDismiss: () -> Unit, onUpdateNotes: (String, String, List<String>) -> Unit, onDeleteRecording: (String) -> Unit)`
  - `@Composable fun SettingsScreen(ui: MainUiState, onDismiss: () -> Unit, onSelectThemeMode: (DshThemeMode) -> Unit, onSelectAccentId: (String) -> Unit, onSaveFilenameTemplate: (String, String) -> Unit, onSyncSchedule: (String) -> Unit, onToggleMcp: () -> Unit, onShowMessage: (String) -> Unit)`
  - `MainActivity` & `ClassRecorderApp` hosting `DshBackdrop`, floating `56.dp` chrome bar (`SegmentedPill` synced to `pagerState` + `44.dp` `GlassIconButton` Settings button with MCP dot), `BackdropVeil`, `HorizontalPager`, and animated push/pop overlays for `PlaybackScreen` and `SettingsScreen`.

- [ ] **Step 1: Rebuild `PlaybackScreen.kt` using `AppPage` + `PageHero`**

Implement Spec §4.5 (`< 450` lines): `AppPage` with `BackHandler`, `PageHero` (course `OverviewGlyph`, `headlineSmall` title, `tnum` metadata, `#tag` chips), `GlassSurface(Card, 20.dp)` holding `ScrubWaveformCanvas` + `tnum` seek labels + `3.dp` progress bar, `-5s` / `Play-Pause` (`primary` stadium) / `+10s` transport row, `SegmentedPill` speed selector (`0.5×`..`2×`), notes card + `Notes` `AppSheet`, and `CodeWell` `.m4a` file stem with confirmation before delete.

- [ ] **Step 2: Create `SettingsScreen.kt` with full DSH Go Appearance Picker**

Implement Spec §4.6 (`< 650` lines): `AppPage(title = "Settings", onBack = onDismiss)` containing:
1. Top MCP `OverviewCard` + expandable `CodeWell` copy rows (`LAN` & `ADB`) + recent agent calls.
2. `ListPageCard(title = "Appearance")` with `ThemeModePicker` (3 `0.8`-aspect miniature Mist Glass phone previews drawn via `Canvas` for `Light`, `Dark`, and split `System`, surrounded by animated `2.dp` `primary` selection ring), `PalettePicker` (6 `30.dp` guarded color swatches with animated outer rings + labels), and `SettingsSwitchTile("Follow wallpaper")` on `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`.
3. `ListPageCard(title = "Class schedule")` with school summary, `DshTextField` URL/share-code well, and `"Sync schedule"` button.
4. `ListPageCard(title = "File naming")` with template `DshTextField`, live `CodeWell` `.m4a` preview via `UiFormatters.previewFilename`, date-format `CapsuleChoiceChip`s, tappable placeholder capsules, and `Reset` / `Save` actions.

- [ ] **Step 3: Rewrite `MainActivity.kt`, remove legacy files, and strip temporary shims in `Theme.kt`**

- In `MainActivity.kt`, read `vm.ui` (`themeMode`, `accentId`) above `ClassRecorderTheme(themeMode = ui.themeMode, accentId = ui.accentId)` so changing mode or accent in `SettingsScreen` crossfades the entire app and system bars live.
- Render `DshBackdrop` -> `HorizontalPager` (padded top by `statusBars + 64.dp`) -> `BackdropVeil(fromTop = true, alpha = currentScrollVeil)` -> floating `56.dp` top chrome row (`SegmentedPill` driven by `pagerState.currentPage + pagerState.currentPageOffsetFraction` + `GlassIconButton` opening `SettingsScreen`) -> `chrome`-styled `SnackbarHost` -> animated `Slide + Fade` overlays for `PlaybackScreen` and `SettingsScreen`.
- Delete `SettingsSheet.kt`, `ExpressiveScaffold.kt`, `Color.kt`, `Backdrop.kt`, and `Expressive.kt`, and remove temporary compatibility aliases from `Theme.kt` and `Shape.kt`.

- [ ] **Step 4: Run unit tests, assemble both APKs, and check `< 1,000` line ceiling**

Run:
```bash
ANDROID_HOME=/home/ryno/Android/Sdk /home/ryno/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle -p /home/ryno/project/class-recorder/android-app :app:testClassrecorderTestDebugUnitTest :app:assembleClassrecorderTestDebug :app:assembleClassrecorderDebug && \
wc -l /home/ryno/project/class-recorder/android-app/app/src/main/java/com/ryno/classrecorder/**/*.kt /home/ryno/project/class-recorder/android-app/app/src/main/java/com/ryno/classrecorder/**/**/*.kt
```
Expected: `BUILD SUCCESSFUL`, APKs produced under `app/build/outputs/apk/`, and every `.kt` file `< 1,000` lines.

- [ ] **Step 5: Commit**

```bash
git -C /home/ryno/project/class-recorder add -A android-app/app/src/main/java/com/ryno/classrecorder/
git -C /home/ryno/project/class-recorder commit -m "feat(ui): add DSH Go PlaybackScreen, SettingsScreen with Appearance picker, and floating shell"
```

---

### Task 6: Emulator Visual Verification, Impeccable Finish Review & `DESIGN.md`

**Files:**
- Create: `.impeccable/review/` screenshots (`phone-dark-record.png`, `phone-dark-live.png`, `phone-dark-schedule.png`, `phone-dark-library.png`, `phone-dark-playback.png`, `phone-dark-settings.png`, `phone-light-record.png`, `phone-light-settings.png`, `phone-fontscale130-schedule.png`)
- Create: `DESIGN.md` and `.impeccable/design.json`

**Interfaces:**
- Consumes: `app-classrecorderTest-debug.apk` from Task 5, `PRODUCT.md`, `.impeccable/surfaces/va-com-ryno-classrecorder-mainactivity-kt-ececd0c7.md`.
- Produces: Verified emulator renders across Dark/Light/Accent/`1.3x` font scale, resolved `impeccable` finish-review verdict, and committed `DESIGN.md` + `.impeccable/design.json`.

- [ ] **Step 1: Boot Android emulator (if no device attached), install `.uitest` APK, and capture batched screenshots**

Check `adb devices`; if empty, list AVDs with `/home/ryno/Android/Sdk/emulator/emulator -list-avds` and launch headless/windowed AVD in background, wait for `sys.boot_completed == 1`, install `app/build/outputs/apk/classrecorderTest/debug/app-classrecorderTest-debug.apk` (`com.ryno.classrecorder.uitest`), grant `RECORD_AUDIO` + `POST_NOTIFICATIONS`, sync `https://uni-campus-app.pages.dev/s/qVaaFS5A` (or seed via MCP `:8765`), and capture settled screenshots into `/home/ryno/project/class-recorder/.impeccable/review/` across:
- Dark Mist: `Record` (idle & live), `Schedule`, `Library`, `Playback`, `Settings`
- Light Mist + alternate accent (`Daiqing` / `Dusk`): `Record`, `Settings`
- `adb shell settings put system font_scale 1.3`: `Record`, `Schedule`, `Settings` (always read original `uimode` and `font_scale` first and restore them immediately after capture).
Inspect every PNG to confirm valid rendering before review.

- [ ] **Step 2: Run Impeccable Finish Review in a fresh subagent context**

Invoke a fresh subagent (no inherited conversation framing) following `/home/ryno/project/class-recorder/.agents/skills/impeccable/reference/degraded/finish-reviewer.md` with:
- Original request & confirmed answers
- Surface brief path (`.impeccable/surfaces/va-com-ryno-classrecorder-mainactivity-kt-ececd0c7.md`)
- Captured screenshot paths in `.impeccable/review/`
- `/home/ryno/project/class-recorder/.agents/skills/impeccable/reference/craft-floor.md` and `/home/ryno/project/class-recorder/.agents/skills/impeccable/reference/android.md`
- Explicit note: native Android Compose build — HTML/CSS detector skipped.

If disposition is `fix` or `rebuild`, batch all material fixes into code, recompile, recapture the affected viewports, and re-verify until disposition is `ship`.

- [ ] **Step 3: Generate `DESIGN.md` and `.impeccable/design.json` via Impeccable Documenter**

Follow `/home/ryno/project/class-recorder/.agents/skills/impeccable/reference/document.md` (via fresh documenter subagent or `reference/degraded/documenter.md`) to generate token-bearing `/home/ryno/project/class-recorder/DESIGN.md` and `/home/ryno/project/class-recorder/.impeccable/design.json` from the finished Compose theme and components.

- [ ] **Step 4: Final test & commit**

Run: `ANDROID_HOME=/home/ryno/Android/Sdk /home/ryno/.gradle/wrapper/dists/gradle-8.14-all/c2qonpi39x1mddn7hk5gh9iqj/gradle-8.14/bin/gradle -p /home/ryno/project/class-recorder/android-app :app:testClassrecorderTestDebugUnitTest :app:assembleClassrecorderDebug`  
Expected: `BUILD SUCCESSFUL`.

```bash
git -C /home/ryno/project/class-recorder add DESIGN.md .impeccable/design.json android-app/app/src/
git -C /home/ryno/project/class-recorder commit -m "docs(design): document DSH Go Mist Glass design system in DESIGN.md"
```
