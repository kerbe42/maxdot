# Advanced Mode (Dear Reader–style proofreading) — Design

**Date:** 2026-07-18
**Status:** Approved, implementing

## Goal

Add a second way to play MaxDot alongside the existing guided experience. The
current mode highlights every planted mistake and offers multiple-choice fixes.
The new **Advanced** mode hides the mistakes: the player is told *how many*
errors are in the passage (broken down by type) and must find each one and type
the correction in place — no highlights, no multiple choice. This mimics the
feel of the game *Dear Reader*.

## Terms

- **Guided mode** — the mode that exists today. Mistakes highlighted, tap →
  dialog → multiple choice.
- **Advanced mode** — the new mode described here.

(The player-facing labels are "Guided" and "Advanced". "Easy/Medium/Hard" stays
as the separate **Difficulty** axis, so we avoid colliding with `Difficulty.EASY`.)

## Axes

`GameMode { GUIDED, ADVANCED }` is independent of `Difficulty`.

- **Difficulty** keeps controlling *how many* mistakes and passage length.
- **Mode** controls *how you answer*.

So Advanced + Hard = a long passage with many mistakes to hunt and type.

## 1. The brief (replaces highlighting)

At the top of each Advanced passage, a card states the count by type, e.g.:

> **5 mistakes to find** — 2 punctuation · 2 word choice · 1 capitalization

Type labels: `PUNCTUATION → "punctuation"`, `GRAMMAR → "word choice"`,
`APOSTROPHE → "apostrophe"`, `CAPITALIZATION → "capitalization"`. Only
categories that actually occur are listed. Nothing in the passage is highlighted.

## 2. Interaction — one unified "type it in place" mechanic

The passage renders as plain text where **every word is tappable** (no hint
which are wrong). Tapping a word opens a small inline editor pre-filled with that
word, with a punctuation shortcut row (`. , ; : ! ? '`) above the keyboard. The
player retypes/fixes the word and confirms.

This single mechanic covers all four mistake kinds:

- Word choice: `their` → `there`
- Apostrophe: `dont` → `don't`
- Capitalization: `the` → `The`
- Missing punctuation: `said` → `said,` (punctuation attaches to the **preceding
  word**, matching the existing tokenizer convention)

No multiple choice anywhere. Decoy "nothing belongs here" slots are dropped in
Advanced — the false-flag mechanic (below) already provides that tension.

## 3. Commit-then-grade

The player edits freely (re-edit / revert allowed), then taps **Check passage**.
Grading reveals, per token:

- ✅ **Caught** — a real mistake fixed correctly
- ❌ **Missed** — a real mistake left unfixed (revealed with the correct text)
- ⚠️ **False flag** — a word the player changed that was actually correct (or
  fixed the wrong way)

The brief's count tells the player how many to hunt for. Comparison is
whitespace-lenient (trim, collapse internal whitespace, drop spaces before
punctuation) and case-sensitive (capitalization is itself a challenge type).

**Perfect** = all mistakes caught AND zero false flags → existing
perfect-passage XP bonus. False flags block "perfect" but never push XP negative
(this is a learning game).

## 4. Hints

The guided hint (eliminate multiple-choice options) is meaningless here. In
Advanced, spending a hint (same `consumeHint()`) reveals the **location** of one
still-uncaught mistake.

## Data model (core)

```kotlin
enum class GameMode(val label: String) { GUIDED("Guided"), ADVANCED("Advanced") }

/** One editable word-token in an Advanced passage. */
data class ProofToken(
    val index: Int,
    val leadingSpace: Boolean,      // render a space before this token
    val shownText: String,          // possibly-corrupted word + trailing punctuation
    val correctText: String,        // correct word + trailing punctuation
    val mistakeType: ChallengeType? = null,
    val explanation: String? = null,
) { val isMistake get() = mistakeType != null }

data class ProofPassage(
    val tokens: List<ProofToken>,
    val mistakeCounts: Map<ChallengeType, Int>,
) { val mistakeTotal get() = tokens.count { it.isMistake } }

enum class TokenOutcome { CAUGHT, MISSED, FALSE_FLAG, UNTOUCHED_CORRECT }

data class ProofResult(
    val outcomes: Map<Int, TokenOutcome>,
    val caught: Int, val missed: Int, val falseFlags: Int, val totalMistakes: Int,
) { val perfect get() = caught == totalMistakes && falseFlags == 0 }
```

- `ChallengeGenerator.generateAdvanced(sentences, difficulty): ProofPassage`
  reuses the existing `tokenize` / `findCandidates` / `selectCandidates`
  machinery (decoys filtered out), corrupting each chosen token instead of
  building multiple-choice `Challenge`s.
- `ProofGrader.grade(passage, edits: Map<Int,String>): ProofResult` — pure,
  unit-tested.

## App wiring

- `SettingsState.gameMode` (default `GUIDED`) + setter + SharedPreferences
  persistence; a "Mode" segmented control in the Gameplay settings card.
- `GameViewModel` branches on mode: build a guided `Passage` or an advanced
  `ProofPassage`. New advanced state (edits, open token, checked, result,
  revealed hints) and actions (`openToken`, `editToken`, `checkPassage`,
  `useProofHint`). On check, record one `recordAnswer(caught)` per real mistake
  plus `recordPassageComplete(perfect)`, so XP/streaks/levels/achievements keep
  working.
- New `GameScreenAdvanced.kt` composables (brief, tappable tokens, edit dialog,
  check button, per-token result colors). `GameScreen` branches on mode and
  shows mode-appropriate "How to play" text.

## Testing

Core unit tests: count breakdown correctness; corrupted `shownText` differs from
`correctText` for mistakes and equals it for non-mistakes; reassembling
`correctText` reproduces the source; `ProofGrader` classification
(caught/missed/false-flag/perfect) and whitespace-lenient compare.

## Out of scope

Live per-edit feedback, timers, per-type separate scoring, and reworking the
guided mode.
