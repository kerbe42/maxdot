# MaxDot

A punctuation & grammar game for Android. Classic books are shown with their punctuation stripped out and sneaky grammar mistakes mixed in — tap a highlighted spot, pick the right fix from the choices, and restore the original text.

## Features

- **Play through real books** — bundled public-domain classics (Pride and Prejudice, Alice in Wonderland, A Tale of Two Cities, Moby-Dick), with your progress saved per book.
- **Import your own books** — EPUB, MOBI, and PDF files from your phone are converted to playable text on-device. Plain `.txt` works too.
- **Mistake variety** — missing punctuation, word confusions (their/there/they're, its/it's, affect/effect, …), subject–verb agreement, apostrophes, capitalization, and decoy spots on Hard where nothing is wrong.
- **Score & accuracy** — every answer is tracked; per-passage summaries and lifetime accuracy.
- **XP, levels & rewards** — earn XP with streak bonuses, level up, and unlock 10 background themes.
- **Achievements, streaks & daily goals** — 14 achievements, answer streaks, and a configurable daily XP goal.
- **Hints** — a limited hint supply that eliminates wrong options.
- **Reading comfort** — adjustable text size, serif/sans font choice, and light/dark/system theme.
- **Difficulty levels** — Easy, Medium, and Hard change mistake density and option counts.

## Project layout

- `core/` — pure Kotlin (JVM) module: text cleaning, sentence splitting, EPUB/MOBI parsing, challenge generation, leveling, and achievements. Fully unit-tested and Android-free.
- `app/` — the Android app: Jetpack Compose UI (Material 3), MVVM, SharedPreferences persistence, PDF import via PdfBox-Android.

## Building

```
./gradlew :app:assembleDebug   # build the app (requires Android SDK)
./gradlew :core:test           # run the core engine unit tests (JVM only)
```

The `core` module has no Android dependencies, so its tests run on any machine with a JDK.
