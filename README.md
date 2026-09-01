# Ren Flies 🐦

A complete, playable Android game built in **Kotlin + Jetpack Compose**: classic
Flappy Bird-style gameplay with bullet-hell **boss encounters** every 100 points,
three powerups, a mock leaderboard, and a fake battle-pass progression system.

> All visual assets are simple placeholder vector artwork (procedural Canvas
> drawing + Android vector drawables + original SVG sources). The leaderboard
> and battle pass are **intentionally mock/local** — no networking, accounts,
> or monetization anywhere.

## Features

- **Flappy gameplay loop** — tap to flap, gravity pulls you down, pipes scroll
  right-to-left, +1 score per pipe passed, gradually increasing difficulty.
- **Global difficulty system** — `DifficultyManager` with Easy (0.75x speed,
  two-pattern layouts, wide margins, frequent powerups), Medium (default),
  Hard (1.25x, extreme layouts, tight margins, rare powerups) and Devilish
  (1.5x, maximum complexity, zero powerup spawns). Procedural pipe generation
  guarantees navigable gaps: corridor-clamped heights plus a max-shift clamp
  between consecutive gaps.
- **Pre-game loadout** — equip one instance of each owned single-use
  consumable before a run; charges are consumed at run start, reset post-game,
  and triggerable mid-run from HUD buttons.
- **Boss encounters at every 100-point milestone** with a deliberate
  `BOSS INCOMING!` intro, health bar, and timed encounter.
- **Boss shield** — the boss is invulnerable until its shield drains
  (survive the opening seconds), with clear shield-active / shield-broken /
  defeated visuals.
- **Three bullet patterns** — radial burst, player-aimed spread fan, and a
  rotating double spiral. Tap during a boss fight to flap *and* shoot back.
- **Powerups**:
  - 🛡 **Shield (stacking heavy armor)** — up to 5 stacks; each pipe hit
    consumes one stack; every active stack adds an incremental gravity/weight
    penalty. Protects ONLY against pipe collisions — ground/ceiling hits are
    always fatal.
  - ⚡ **Speed Boost** — pass through obstacles safely for 6s, faster scroll,
    floatier gravity. On expiry: a 200ms full-screen white flash plus instant
    despawn of every on-screen obstacle (safe expiry). Does *not* protect
    against boss bullets.
  - 💀 **Berserker** — a destructive magnetic field that drags obstacles in,
    destroys them, and awards their score; upcoming pipe gaps shift away from
    the bird while active; vaporizes boss bullets defensively via a projectile
    absorption hook (ready for Milestone 3 boss systems). Boss shields can't
    be bypassed.
- **Game states** — `MENU → PLAYING → BOSS_INTRO → BOSS → BOSS_CLEAR → GAME_OVER`.
- **Mock leaderboard** — fictional entries ranked together with your best score.
- **Fake battle pass** — 20 levels with XP thresholds and rewards across three
  unlock types (Skins, Emotes, Single-Use Consumables), earned from score
  (+1 XP/point), bosses (+250 XP each) and run completion (+50 XP).
- **Persistence** — best score, XP, lifetime stats via SharedPreferences.
- **Delta-time game loop** — frame-rate independent, allocation-light updates.
- **Generated audio** — placeholder sound effects via `ToneGenerator` behind an
  `AudioFeedback` abstraction (no-op in tests).

## Controls

- **Tap anywhere** to flap.
- During a boss encounter, tapping also fires a shot at the boss.

## Architecture

```
app/src/main/java/com/renfliestudios/renflies/
├── MainActivity.kt              # Compose entry point, wiring
├── game/                        # PURE Kotlin - no Android deps, fully unit tested
│   ├── GameConfig.kt            # All tuning constants + difficulty curves
│   ├── DifficultyManager.kt     # Difficulty enum/matrix + global manager
│   ├── GameState.kt             # GamePhase enum + RunResult
│   ├── GameEngine.kt            # Delta-time update loop, scoring, milestones,
│   │                            #   boss lifecycle, powerup effects, collisions
│   ├── Player.kt                # Bird physics
│   ├── Obstacle.kt              # Pipe pairs (top/bottom rects + gap)
│   ├── Bullet.kt                # Boss bullets & player shots
│   ├── Boss.kt                  # Boss entity, shield, 3 bullet patterns
│   ├── PowerUp.kt               # Powerup types & collectibles
│   ├── CollisionSystem.kt       # Circle/rect intersection helpers
│   └── audio/AudioFeedback.kt   # Sound abstraction + no-op
├── data/
│   ├── ProgressStore.kt         # Interface, in-memory + SharedPreferences impls
│   ├── Leaderboard.kt           # Mock leaderboard
│   └── BattlePass.kt            # XP curve, levels, rewards
└── ui/
    ├── RenFliesApp.kt           # Screen navigation
    ├── MainMenuScreen.kt        # Title, play, best score, buttons, sound toggle
    ├── LoadoutScreen.kt         # Difficulty picker + pre-game consumable loadout
    ├── GameScreen.kt            # Frame loop + Canvas renderer + HUD + overlays
    ├── LeaderboardScreen.kt     # Mock leaderboard UI
    ├── BattlePassScreen.kt      # Levels, XP bar, rewards
    ├── ToneAudioFeedback.kt     # ToneGenerator implementation
    └── theme/Theme.kt
```

The game simulates in a fixed **720×1280 virtual world** and the renderer
scales it to any screen, so gameplay is identical on all devices.

## Build instructions

Requirements: JDK 17, Android SDK (Android Studio or command-line tools).

```bash
# Unit tests
./gradlew test

# Debug APK
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

## How to run the app

1. Open the project in **Android Studio**.
2. Let Gradle sync (wrapper: Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24, Compose BOM).
3. Press **Run** on a device/emulator with Android 8.0+ (minSdk 26),
   or install the debug APK directly:
   `adb install app/build/outputs/apk/debug/app-debug.apk`.

## How to run tests

```bash
./gradlew test
```

Coverage includes: score increments, 100/200-point boss milestones (each fires
exactly once), one-hit shield behaviour, speed-boost obstacle immunity,
Berserker field destruction + scoring, boss shield blocking damage, boss-bullet
shield consumption, delta-time consistency, battle-pass XP/levels, leaderboard
ranking, and best-score persistence.

## GitHub Actions

`.github/workflows/build.yml` runs on every push and pull request:

1. Sets up JDK 17 (Temurin) and Gradle.
2. Runs `./gradlew test`.
3. Builds the **debug APK** (no signing credentials required).
4. Uploads the APK as the `renflies-debug-apk` artifact.

## Mock systems (on purpose)

- **Leaderboard**: hardcoded fictional pilots (SkyMaster, WingLegend, …) merged
  with your local best score. Entirely on-device.
- **Battle pass**: XP comes purely from playing; rewards are cosmetic labels.
  No purchases, no backend, no authentication.

## Placeholder assets

All art is placeholder: gameplay visuals are drawn procedurally on a Compose
`Canvas` (bird, pipes, boss, bullets, powerups, field effects), menu icons are
Android vector drawables in `res/drawable/`, and the original SVG sources are
kept in `app/src/main/assets/svg/`. Nothing is copied from any copyrighted game.
