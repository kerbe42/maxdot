# Game-Feel Overhaul: themes, game systems, and juice — Design

**Date:** 2026-07-18
**Status:** Draft, awaiting review

## Goal

Today MaxDot has all the *systems* of a game (XP, 50 levels, streaks, daily
goals, achievements, unlockable backgrounds) but the *presentation* of an ebook
reader: a navy/gold/parchment literary palette, "library" / "Import book"
language, book icons, serif prose in flat cards, a thin utilitarian score bar,
and near-silent feedback (a snackbar). It reads as a Kindle with a quiz overlay.

This overhaul makes MaxDot **look and feel like a game** without discarding the
proofreading gameplay that makes it what it is. Three things stack:

1. **A skinnable game-theme system** — three full, switchable themes.
2. **New game systems** — a live combo meter, a world-map hub, boss passages,
   and an opt-in Lives + Blitz mode.
3. **Juice** — the missing ingredient: animated feedback, flying XP, combo
   pumps, celebrations, level-up fanfare, and light SFX.

The core proofreading logic in `core/` (challenge generation, grading, leveling)
is largely untouched. This is mostly a presentation, feel, and progression-shell
change plus a few additive models.

## Decisions locked in brainstorming

- **Scope:** reskin **and** new game systems (not a core-loop rewrite).
- **Systems to build:** combo meter, world-map hub, boss passages, Lives + Blitz.
- **Visual identity:** all three directions ship as **user-selectable themes**.
- **Default theme:** Playful Pop.
- **Theme access:** all three free from the start; the existing level-up unlock
  reward is **repurposed into per-theme cosmetic accents / palette variants** so
  progression still rewards the player. Existing gradient backgrounds fold in as
  theme variants rather than being discarded.
- **Sound:** include simple SFX in v1, behind a mute toggle.
- **Light/dark:** each theme commits to its own look (Playful = light, Neon &
  Retro = dark); the standalone system/light/dark `ThemeMode` toggle is absorbed
  into theme selection.

---

## 1. Theming architecture (the backbone)

Introduce a `GameTheme` value that the entire UI reads design tokens from,
provided through a Compose `CompositionLocal`. Swapping the theme transforms the
whole app.

```kotlin
enum class GameThemeId { PLAYFUL, NEON, RETRO }

enum class ButtonStyle { PRESSABLE_3D, NEON_GLOW, PIXEL_BLOCK }
enum class NodeShape   { SQUIRCLE, GLOW_CIRCLE, DIAMOND }

data class GameColors(
    val bg: List<Color>,          // page gradient (1+ stops)
    val surface: Color,           // cards / panels
    val surfaceAlt: Color,
    val primary: Color,           // main CTA / current node
    val onPrimary: Color,
    val accent: Color,            // combo / highlights
    val hearts: Color,            // lives / danger
    val success: Color,           // correct / XP fill
    val warning: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val errorHighlight: Color,    // the wrong-word chip in a passage
    val glow: Boolean,            // whether accents render a glow layer
)

data class GameTypography(
    val display: FontFamily,      // titles, big numbers
    val hud: FontFamily,          // HUD chips, combo
    val body: FontFamily,         // passage prose
)

data class GameShapes(
    val button: ButtonStyle,
    val node: NodeShape,
    val cardCorner: Dp,
    val controlCorner: Dp,
)

data class GameTheme(
    val id: GameThemeId,
    val name: String,
    val colors: GameColors,
    val type: GameTypography,
    val shapes: GameShapes,
)

val LocalGameTheme = staticCompositionLocalOf<GameTheme> { GameThemes.PLAYFUL }

@Composable
fun MaxDotTheme(themeId: GameThemeId, accentVariant: String, content: @Composable () -> Unit) {
    val theme = GameThemes.resolve(themeId, accentVariant)
    // Also derive a MaterialTheme colorScheme from `theme` so stock M3
    // components (dialogs, snackbars) stay on-brand.
    CompositionLocalProvider(LocalGameTheme provides theme) {
        MaterialTheme(colorScheme = theme.toColorScheme(), content = content)
    }
}
```

A small **themed component kit** consumes `LocalGameTheme.current` so screens
never hardcode colors/shapes:

- `GameButton` — renders pressable-3D / neon-glow / pixel-block per theme.
- `GameCard`, `PassageCard`
- `ComboMeter`, `HeartsRow`, `XpBar`, `HudBar`
- `MapNode` (squircle / glow-circle / diamond)
- `CelebrationOverlay`, `XpFly`

**Migration:** existing `Theme.kt` (`LightColors`/`DarkColors`) and
`Backgrounds.kt` are refactored into the three `GameThemes` definitions.
`toFamily()` for fonts stays but is superseded by per-theme typography.

### The three themes

| Token | Playful Pop (default) | Neon Arcade | Retro Quest |
|---|---|---|---|
| bg | `#F4EEFF` flat | `#0B0F1C → #10182F` | `#161233` |
| primary | `#6C4CF1` | `#25E0D4` (glow) | `#F5C542` |
| accent | `#FFC531` | `#FF3D8B` (glow) | `#9B6BFF` |
| hearts | `#FF5A6E` | `#FF3D8B` | `#F5C542` |
| success | `#22C39A` | `#C6FF4D` | `#5CE08B` |
| button | pressable 3D (bottom shadow) | neon fill + glow | 2px block, sharp |
| node | squircle w/ 3D lip | glowing circle | diamond |
| type | rounded sans, heavy | condensed sans | monospace/pixel |
| corners | 14–16px | 14px | 0px (sharp) |
| mascot | "Max the dot" present | — | — |

Retro's pixel body font is applied to HUD/titles only; passage prose stays in a
legible fallback so long reading isn't punished. `glow` is a Neon-only flag that
adds a soft outer draw behind accents (approximated with layered translucent
strokes / `blur` on a background layer, since Compose has no free box-shadow
glow).

---

## 2. New game systems

### 2a. Combo meter

The streak already drives an XP bonus (`Leveling.xpForAnswer(streak)` →
`+2 XP per streak step, capped`). Promote it to a first-class HUD element:

- A `ComboMeter` shows the current multiplier tier and a fill bar toward the
  next tier. Tiers map from streak: x1 (0–1), x2 (2–3), x3 (4–6), x5 (7–10),
  x8 (11+). (Tuned in one place; display only — underlying XP math unchanged
  except we scale the displayed/awarded bonus to match tiers.)
- Each correct answer **pumps** the meter (scale + flare). A wrong answer
  **cracks** it (shake, desaturate) and resets streak to 0.
- No new persistence — combo is session state derived from the live streak.

### 2b. World-map hub (replaces the library list)

Each **book is a world**; a world is a **path of level nodes**; each node is one
passage (bosses are special nodes). This is a presentation layer over the
existing sequential passage progression — the book still streams passages in
order; we just discretize the path.

```kotlin
data class WorldNode(
    val index: Int,           // 0-based position on the path
    val kind: NodeKind,       // LEVEL or BOSS
    val state: NodeState,     // LOCKED, CURRENT, DONE
    val stars: Int,           // 0–3 based on accuracy on that node
)
enum class NodeKind { LEVEL, BOSS }
enum class NodeState { LOCKED, CURRENT, DONE }

data class World(
    val book: Book,
    val nodes: List<WorldNode>,
    val percent: Int,         // existing book progress
)
```

- Node count per world is derived from book length / target passage size
  (bounded, e.g. 12–40 nodes) so the path is finite and legible. A **boss node**
  is placed every `BOSS_INTERVAL` (e.g. 5) nodes and at the end.
- The map is a vertically scrolling winding path (alternating left/right) of
  `MapNode`s connected by links, themed per `NodeShape`. The current node
  pulses; tapping it starts play; done nodes show 0–3 stars; locked nodes are
  dimmed.
- **Stars per node:** 3 = perfect (no misses / no false flags), 2 = ≥ target
  accuracy, 1 = completed. Stored per node index in the existing progress map
  (extend `BookProgress` with a `starsByNode: Map<Int,Int>` or derive from
  per-passage results already recorded).
- Home shows a **selectable list of Worlds** (one card per book, with its
  percent, stars, and current-node preview); tapping a world opens its winding
  path. The import-a-book action is reframed as "add a new world." (A single
  most-recent world can be surfaced expanded at the top for one-tap "continue.")

### 2c. Boss passages

A boss node plays like a passage with battle framing:

- A **boss health bar** (max = number of mistakes in the passage) depletes one
  chunk per correct fix; a miss/false-flag does no damage (learning game — never
  punish into a loss in the default flow).
- Boss passages are longer / harder: bias toward `Difficulty.HARD` sizing for
  that node regardless of the player's difficulty, or +1 mistake tier.
- On victory: a bigger `CelebrationOverlay` (boss-defeated fanfare), extra XP
  reward, and star assignment as usual.
- Model: reuse existing `Passage`/`ProofPassage`; add a `PassageContext` flag
  `isBoss: Boolean` carried into `GameUiState` so the HUD swaps in the health
  bar and the results screen swaps in the victory framing.

### 2d. Lives + Blitz mode (opt-in)

Launched from a world via a "Blitz" button; a distinct high-stakes run:

- **3 hearts** — each miss (guided wrong answer, or Advanced false-flag/miss on
  check) costs a heart. At 0 hearts the run ends with a score summary.
- **Countdown timer** — a single per-run countdown for score-attack (fixed run
  length, e.g. 90s, topped up a little on each correct answer to reward flow);
  running out ends the run.
- Blitz is a **session mode**, not a difficulty; it reuses the normal passage
  generation and grading. It records XP/answers into the profile as usual (so it
  still feeds levels/achievements) but its heart/timer state is ephemeral.
- Neon energy is strongest here regardless of active theme accents (Blitz can
  lean into the accent/glow tokens).

```kotlin
data class BlitzState(
    val hearts: Int,          // starts at 3
    val secondsLeft: Int,
    val score: Int,
    val running: Boolean,
)
```

---

## 3. Juice & animation

The core of "fun UX." All additive UI; no logic change.

- **Correct:** success-color burst + a small particle sparkle at the word,
  checkmark pop, existing confirm haptic; the wrong word cross-fades to the
  correct word.
- **Wrong:** red flash + horizontal shake on the passage card + reject haptic;
  in Blitz, a heart drains with a pop.
- **Flying XP (`XpFly`):** a "+N" label spawns at the answered word and arcs up
  into the `XpBar`, which then fills with a spring; combo multiplier shown on the
  label (e.g. "+15 x3").
- **Combo pump:** meter scales/flares per tier-up; crack animation on break.
- **Passage / boss complete:** `CelebrationOverlay` — confetti or star burst
  (themed), an animated "Perfect!" / "Boss defeated!" stamp, and summary counts
  that tick up. Replaces the current quiet snackbar/summary card.
- **Level-up:** full-screen themed celebration (already have the level-up event;
  make it a real moment, and surface any newly-unlocked cosmetics).
- **Map:** node pop when newly unlocked; path draws in on entry; current node
  idle-pulses.

Implementation: Compose `Animatable` / `animate*AsState` / `AnimatedVisibility`;
a lightweight custom particle/confetti `Canvas` (no external lib). Respect an
existing "reduce motion" affordance if present, else fall back gracefully.

## 4. Sound effects

A small SFX set played through a simple `SoundManager` (Android `SoundPool`):

- `tap`, `correct`, `wrong`, `combo_up`, `passage_complete`, `boss_win`,
  `level_up`.
- Assets: short, royalty-free / generated clips bundled in `res/raw`.
- **Mute toggle** in Settings (default on), persisted alongside `hapticsEnabled`.
  SFX volume respects the system media stream.

## 5. Screen-by-screen changes

- **Home** → **world-map hub.** Themed title treatment, a top HUD strip (level,
  XP bar, day-streak flame), one world per book as a scrollable path (or a world
  carousel + selected path). "Import book" → "Add a world." Rewards/Settings
  entry points kept.
- **Play** → real **HUD** (combo meter always; hearts + timer in Blitz; boss
  health bar on boss nodes) above a themed `PassageCard`; juicy answer feedback
  and flying XP. Both Guided and Advanced modes flow through the same HUD.
- **Boss** → play screen with boss framing (health bar + name + victory
  celebration).
- **Results** → animated `CelebrationOverlay` instead of the snackbar/summary.
- **Settings** → headlined by a **theme picker** with three big live previews;
  plus SFX toggle, haptics, font scale, daily goal, difficulty, Guided/Advanced
  mode. `ThemeMode` (system/light/dark) is removed (absorbed by theme).
- **Rewards** → cosmetic unlocks reframed per theme (accent/palette variants
  unlocked by level) + achievements with punchier, animated presentation.

## 6. Reward rework (cosmetics)

Repurpose the level-gated unlock mechanic:

- Replace the 10 standalone gradient `Backgrounds` with **per-theme cosmetic
  variants** (e.g. accent color-ways / mascot skins for Playful; glow hues for
  Neon; palette swaps for Retro), each with an `unlockLevel`.
- Extend the profile: `selectedTheme: GameThemeId`, `selectedVariant:
  Map<GameThemeId,String>`, keep `unlockedAchievements`. Migrate the existing
  `selectedBackground` field into the new variant model (default variant if the
  old id doesn't map).
- Newly-unlocked variants surface in the level-up celebration.

## 7. Build phases

Shippable incrementally; each phase leaves the app working:

1. **Theme foundation** — `GameTheme` + `CompositionLocal` + component kit +
   three theme definitions + Settings theme picker. Retarget existing screens to
   the kit (no new systems yet). App looks like a game, three ways.
2. **Juice layer** — combo meter, flying XP, correct/wrong feedback,
   celebration overlay, level-up moment, on the existing play screen.
3. **World-map hub** — `World`/`WorldNode` model + map screen replacing the
   library list; stars.
4. **Boss passages** — boss nodes, health bar, victory framing.
5. **Lives + Blitz mode** — hearts, timer, Blitz entry + summary.
6. **SFX** — `SoundManager` + assets + mute toggle.
7. **Reward rework** — cosmetic variants + profile migration.

(SFX can move earlier if assets are ready; phases 3–5 are independent of each
other and can reorder.)

## 8. Testing

- **Core (pure, unit-tested):** combo-tier mapping from streak; world-node
  generation (count bounds, boss placement interval, star thresholds); boss
  health derivation; Blitz heart/timer transitions. These go in `core/` as
  pure functions so they're testable without Android.
- **App:** existing tests stay green; theme resolution returns a valid
  `GameTheme` for every `GameThemeId` + variant; profile migration from the old
  `selectedBackground` produces a valid variant.
- **Manual:** each theme rendered across every screen (light Playful vs dark
  Neon/Retro contrast checks); animation performance on a mid-range device;
  legibility of passage prose in each theme.

## 9. Out of scope (v1)

- A core-loop rewrite (arcade "zap the words") — explicitly not chosen.
- Online leagues / leaderboards / multiplayer.
- New challenge *types* beyond the existing four.
- Per-theme light/dark variants (each theme is one committed look for now).
- Custom user-authored themes.
