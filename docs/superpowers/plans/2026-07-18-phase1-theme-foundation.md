# Phase 1: Theme Foundation — Implementation Plan

> **For agentic workers:** Implement task-by-task. Steps use checkbox (`- [ ]`) syntax. Each task ends with a green build/test and a commit.

**Goal:** Give MaxDot a skinnable game-theme system with three switchable looks (Playful Pop, Neon Arcade, Retro Quest) that recolor, re-type, and re-shape the whole app, plus a Settings theme picker — replacing the ebook-reader palette. Full HUD/map/juice come in later phases; this phase is the token system + component primitives + app-wide adoption.

**Architecture:** A `GameTheme` token bundle is provided via a Compose `CompositionLocal` (`LocalGameTheme`). `MaxDotTheme` resolves the player's selected theme, provides `LocalGameTheme`, and derives a Material 3 `ColorScheme` from it so stock M3 components stay on-brand. A small themed component kit (`GameBackground`, `GameCard`, `GameButton`, `GameTitle`) reads the local theme. Existing screens adopt the new look automatically through the theme; the settings theme picker drives `SettingsState.selectedTheme`.

**Tech Stack:** Kotlin 2.1.21, Jetpack Compose (BOM 2025.05.00), Material 3, minSdk 26, JVM 17.

## Global Constraints

- Build env: `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`; SDK at `C:/Users/white/AppData/Local/Android/Sdk` (already in `local.properties`).
- Verify compile: `./gradlew.bat :app:assembleDebug --console=plain` (Windows) — this is the primary gate for Compose code.
- Verify pure logic: `./gradlew.bat :core:test --console=plain` and `:app:testDebugUnitTest`.
- UI copy is sentence case. No new Gradle dependencies in this phase (fonts use built-in `FontFamily` families for now; custom font files are a later polish).
- Package root: `com.maxdot.app`. Theme code lives in `com.maxdot.app.ui.theme`; primitives in `com.maxdot.app.ui.components`.
- Do not break existing `core` tests or existing gameplay. `AppFont` and `feedbackColors` stay callable (feedback colors are re-sourced from the theme).

---

## File structure (Phase 1)

- Create `app/src/main/java/com/maxdot/app/ui/theme/GameTheme.kt` — token data classes, enums, `LocalGameTheme`.
- Create `app/src/main/java/com/maxdot/app/ui/theme/GameThemes.kt` — the three theme definitions, `resolve`, `toColorScheme`, theme fonts.
- Modify `app/src/main/java/com/maxdot/app/ui/theme/Theme.kt` — `MaxDotTheme` resolves + provides the game theme; keep `feedbackColors`/`AppFont.toFamily`.
- Modify `app/src/main/java/com/maxdot/app/data/ProfileRepository.kt` — add `selectedTheme` to `SettingsState` + persistence + `setTheme`.
- Create `app/src/main/java/com/maxdot/app/ui/components/GamePrimitives.kt` — `GameBackground`, `GameCard`, `GameButton`, `GameTitle`.
- Modify `app/src/main/java/com/maxdot/app/ui/App.kt` — background + theme wiring.
- Modify `app/src/main/java/com/maxdot/app/ui/screens/SettingsScreen.kt` — theme picker replaces the system/light/dark control.
- Create `app/src/test/java/com/maxdot/app/ui/theme/GameThemesTest.kt` — resolution table tests.

---

## Task 1: Theme token model + CompositionLocal

**Files:**
- Create: `app/src/main/java/com/maxdot/app/ui/theme/GameTheme.kt`

**Interfaces (Produced — later tasks/phases rely on these exact names):**
- `enum class GameThemeId { PLAYFUL, NEON, RETRO }`
- `enum class ButtonStyle { PRESSABLE_3D, NEON_GLOW, PIXEL_BLOCK }`
- `enum class NodeShape { SQUIRCLE, GLOW_CIRCLE, DIAMOND }`
- `data class GameColors(bg: List<Color>, surface, surfaceAlt, primary, onPrimary, accent, hearts, success, warning, textPrimary, textSecondary, errorHighlight, dark: Boolean, glow: Boolean)` — all `Color` except the two flags.
- `data class GameTypography(display: FontFamily, hud: FontFamily, body: FontFamily)`
- `data class GameShapes(button: ButtonStyle, node: NodeShape, cardCorner: Dp, controlCorner: Dp)`
- `data class GameTheme(id: GameThemeId, name: String, colors: GameColors, type: GameTypography, shapes: GameShapes)`
- `val LocalGameTheme: ProvidableCompositionLocal<GameTheme>` (default = the Playful theme).

- [ ] **Step 1: Write `GameTheme.kt`** with the enums and data classes above and `val LocalGameTheme = staticCompositionLocalOf { GameThemes.PLAYFUL }`. (Forward reference to `GameThemes` is fine — same module, resolved in Task 2.)
- [ ] **Step 2: Compile gate** — `./gradlew.bat :app:compileDebugKotlin --console=plain` after Task 2 exists (Task 1 alone won't compile due to the forward reference; Tasks 1–2 compile together). Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** (folded into Task 2's commit, since they compile as a pair).

---

## Task 2: The three theme definitions + resolution + M3 bridge

**Files:**
- Create: `app/src/main/java/com/maxdot/app/ui/theme/GameThemes.kt`
- Test: `app/src/test/java/com/maxdot/app/ui/theme/GameThemesTest.kt`

**Interfaces (Produced):**
- `object GameThemes { val PLAYFUL: GameTheme; val NEON: GameTheme; val RETRO: GameTheme; val ALL: List<GameTheme>; fun byId(id: GameThemeId): GameTheme; fun resolve(id: GameThemeId): GameTheme }`
- `fun GameTheme.toColorScheme(): ColorScheme` — maps tokens onto a Material 3 `lightColorScheme`/`darkColorScheme` (primary→primary, accent→secondary, surface, background=bg[0], error=hearts, on* colors, chooses light/dark base by `colors.dark`).

Palette per the spec table:

| Token | PLAYFUL | NEON | RETRO |
|---|---|---|---|
| bg | `[#F4EEFF]` | `[#0B0F1C, #10182F]` | `[#161233]` |
| surface | `#FFFFFF` | `#121A2E` | `#0E0A26` |
| primary | `#6C4CF1` | `#25E0D4` | `#F5C542` |
| accent | `#FFC531` | `#FF3D8B` | `#9B6BFF` |
| hearts | `#FF5A6E` | `#FF3D8B` | `#F5C542` |
| success | `#22C39A` | `#B6FF3D` | `#5CE08B` |
| errorHighlight | `#FFE1E5` | `#3A1020` | `#3A0E1C` |
| textPrimary | `#2E2542` | `#DCE6FA` | `#E8E0FF` |
| dark | false | true | true |
| glow | false | true | false |
| button | PRESSABLE_3D | NEON_GLOW | PIXEL_BLOCK |
| node | SQUIRCLE | GLOW_CIRCLE | DIAMOND |
| cardCorner | 16.dp | 14.dp | 0.dp |
| type.display / hud | `FontFamily.SansSerif` (bold used at call sites) | `FontFamily.SansSerif` | `FontFamily.Monospace` |
| type.body | `FontFamily.SansSerif` | `FontFamily.SansSerif` | `FontFamily.SansSerif` (legible prose even in Retro) |

- [ ] **Step 1: Write the failing test** `GameThemesTest.kt`:

```kotlin
package com.maxdot.app.ui.theme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameThemesTest {
    @Test fun everyIdResolves() {
        GameThemeId.entries.forEach { id -> assertNotNull(GameThemes.resolve(id)) }
    }
    @Test fun idsAreConsistent() {
        GameThemeId.entries.forEach { id -> assertEquals(id, GameThemes.resolve(id).id) }
    }
    @Test fun themesAreDistinct() {
        val primaries = GameThemes.ALL.map { it.colors.primary }.toSet()
        assertEquals(3, GameThemes.ALL.size)
        assertEquals(3, primaries.size)
    }
    @Test fun neonAndRetroAreDark() {
        assertTrue(GameThemes.NEON.colors.dark)
        assertTrue(GameThemes.RETRO.colors.dark)
        assertTrue(!GameThemes.PLAYFUL.colors.dark)
    }
    @Test fun colorSchemeBridgesPrimary() {
        val scheme = GameThemes.PLAYFUL.toColorScheme()
        assertEquals(GameThemes.PLAYFUL.colors.primary, scheme.primary)
    }
}
```

- [ ] **Step 2: Add `kotlin.test` to app test deps** — in `app/build.gradle.kts` add `testImplementation(libs.kotlin.test)` beside the existing `testImplementation(libs.junit)`.
- [ ] **Step 3: Run test, expect FAIL** — `./gradlew.bat :app:testDebugUnitTest --tests "com.maxdot.app.ui.theme.GameThemesTest" --console=plain`. Expected: FAIL (unresolved `GameThemes`).
- [ ] **Step 4: Implement `GameThemes.kt`** with the three `GameTheme` values (palette table above), `ALL`, `byId`, `resolve` (= `byId`), and `toColorScheme()`.
- [ ] **Step 5: Run test, expect PASS** — same command. Expected: PASS (5 tests).
- [ ] **Step 6: Compile the app** — `./gradlew.bat :app:compileDebugKotlin --console=plain`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 7: Commit** — `git add` the theme files + test + build.gradle; `git commit -m "Phase 1: GameTheme token system + three theme definitions"`.

---

## Task 3: Persist the selected theme

**Files:**
- Modify: `app/src/main/java/com/maxdot/app/data/ProfileRepository.kt`

**Interfaces (Produced):**
- `SettingsState.selectedTheme: GameThemeId` (default `GameThemeId.PLAYFUL`).
- `ProfileRepository.setTheme(id: GameThemeId)`.

**Interfaces (Consumed):** `GameThemeId` (Task 1).

- [ ] **Step 1:** Add `val selectedTheme: com.maxdot.app.ui.theme.GameThemeId = com.maxdot.app.ui.theme.GameThemeId.PLAYFUL` to `SettingsState`.
- [ ] **Step 2:** In `saveSettings`, add `.putString("selectedTheme", s.selectedTheme.name)`. In `loadSettings`, add `selectedTheme = enumOrDefault(prefs.getString("selectedTheme", null), GameThemeId.PLAYFUL)`.
- [ ] **Step 3:** Add setter `fun setTheme(id: GameThemeId) = updateSettings { it.copy(selectedTheme = id) }`.
- [ ] **Step 4: Compile** — `./gradlew.bat :app:compileDebugKotlin --console=plain`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** — `git commit -m "Phase 1: persist selectedTheme in settings"`.

---

## Task 4: `MaxDotTheme` provides the game theme

**Files:**
- Modify: `app/src/main/java/com/maxdot/app/ui/theme/Theme.kt`

**Interfaces (Produced):**
- `@Composable fun MaxDotTheme(settings: SettingsState, content: @Composable () -> Unit)` — resolves `GameThemes.resolve(settings.selectedTheme)`, provides `LocalGameTheme`, wraps `MaterialTheme(colorScheme = theme.toColorScheme())`.
- Keep `feedbackColors(dark)`; add overload/helper `feedbackColors(theme: GameTheme)` returning `FeedbackColors(theme.colors.success, theme.colors.hearts)`.
- Keep `AppFont.toFamily()`.

- [ ] **Step 1:** Rewrite `MaxDotTheme` to take `settings`, resolve the theme, and provide `LocalGameTheme` + derived `MaterialTheme`. Remove reliance on `isAppInDarkTheme` for color selection (the theme carries `dark`). Keep `isAppInDarkTheme` for now (used by callers) but have it return `LocalGameTheme.current.colors.dark` when a theme is active, or keep taking `settings` and delegate to `GameThemes.resolve(settings.selectedTheme).colors.dark`.
- [ ] **Step 2:** Update `App.kt` call site if the signature changed (it already passes `settings`).
- [ ] **Step 3: Compile** — `./gradlew.bat :app:compileDebugKotlin --console=plain`. Fix any references to `isAppInDarkTheme(settings)` in `GameScreen.kt`/`GameScreenAdvanced.kt` (they should still resolve).
- [ ] **Step 4: Commit** — `git commit -m "Phase 1: MaxDotTheme resolves and provides the active game theme"`.

---

## Task 5: Themed background wiring in `App.kt`

**Files:**
- Modify: `app/src/main/java/com/maxdot/app/ui/App.kt`

- [ ] **Step 1:** Replace `Backgrounds.byId(profile.selectedBackground).brush(dark)` with a brush from the active theme: `val theme = GameThemes.resolve(settings.selectedTheme); Brush.verticalGradient(theme.colors.bg)` (single-stop lists fall back to a solid fill via `listOf(c, c)`). Remove the `Backgrounds`/`selectedBackground` dependency from `App.kt` (leave `Backgrounds.kt` file in place for now; it is retired in Phase 7).
- [ ] **Step 2: Compile** — `:app:compileDebugKotlin`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** — `git commit -m "Phase 1: app background driven by active theme"`.

---

## Task 6: Themed component primitives

**Files:**
- Create: `app/src/main/java/com/maxdot/app/ui/components/GamePrimitives.kt`

**Interfaces (Produced — later phases build on these):**
- `@Composable fun GameCard(modifier, content: @Composable ColumnScope.() -> Unit)` — surface + theme `cardCorner`; Retro draws a 2px border, others a soft elevation.
- `@Composable fun GameButton(text: String, onClick: () -> Unit, modifier, enabled: Boolean = true)` — renders per `theme.shapes.button`: PRESSABLE_3D (primary fill + darker bottom border, presses down), NEON_GLOW (primary fill + layered translucent glow ring), PIXEL_BLOCK (primary fill + 2px sharp border, no rounding).
- `@Composable fun GameTitle(text: String, modifier)` — theme `display` font, heavy weight, `textPrimary`.
- `@Composable fun GameBackground(modifier, content: @Composable BoxScope.() -> Unit)` — Box filling max size with the theme bg brush (so any screen can wrap itself).

- [ ] **Step 1:** Implement the four composables reading `LocalGameTheme.current`. Use `@Preview` functions at the bottom (one per theme via a `CompositionLocalProvider(LocalGameTheme provides GameThemes.NEON)` wrapper) for visual sanity.
- [ ] **Step 2: Compile** — `:app:compileDebugKotlin`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** — `git commit -m "Phase 1: themed component kit (card, button, title, background)"`.

---

## Task 7: Settings theme picker

**Files:**
- Modify: `app/src/main/java/com/maxdot/app/ui/screens/SettingsScreen.kt`

- [ ] **Step 1:** Replace the "Theme" system/light/dark `SingleChoiceSegmentedButtonRow` with a **theme picker**: a row of three tappable preview tiles (one per `GameThemes.ALL`). Each tile shows the theme name over a mini preview (a small `Box` painted with the theme bg brush + a dot in `primary` + a chip in `accent`), highlighted when `settings.selectedTheme == theme.id`, calling `mainViewModel.profiles.setTheme(theme.id)`.
- [ ] **Step 2:** Remove the now-unused `ThemeMode` import/segmented control from this screen. Keep reading font/size (still valid). The reading-font `FilterChip`s stay.
- [ ] **Step 3: Compile** — `:app:compileDebugKotlin`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** — `git commit -m "Phase 1: settings theme picker with live previews"`.

---

## Task 8: Full-build verification + manual smoke

**Files:** none (verification only).

- [ ] **Step 1:** Full assemble — `./gradlew.bat :app:assembleDebug --console=plain`. Expected: BUILD SUCCESSFUL, APK produced under `app/build/outputs/apk/debug/`.
- [ ] **Step 2:** Run `:core:test` + `:app:testDebugUnitTest` — both green.
- [ ] **Step 3:** Manual (user or emulator): open Settings → switch between the three themes → confirm the whole app recolors (background, cards, buttons, dialogs) and persists across relaunch.
- [ ] **Step 4:** Tag the phase — `git commit --allow-empty -m "Phase 1 complete: three switchable game themes"`.

---

## Self-review

- **Spec coverage (Phase 1 slice):** theme token system ✓ (T1), three themes ✓ (T2), selectable + persisted ✓ (T3/T7), whole-app adoption via M3 bridge + background ✓ (T4/T5), component kit for later phases ✓ (T6). ThemeMode absorbed ✓ (T7 removes its control; enum left inert, fully removed in a later cleanup). Backgrounds retirement deferred to Phase 7 (noted). Cosmetic variants, combo meter, map, boss, blitz, SFX, juice — all later phases, correctly out of Phase 1 scope.
- **Placeholder scan:** none — palette values, signatures, and commands are concrete.
- **Type consistency:** `GameThemeId`, `GameThemes.resolve`, `GameTheme.toColorScheme`, `LocalGameTheme`, `SettingsState.selectedTheme`, `setTheme` used consistently across T1–T7.
- **Note:** `resolve(id)` currently ignores cosmetic variants (Phase 7 adds `resolve(id, variant)`); keeping the single-arg form now avoids a premature signature.
