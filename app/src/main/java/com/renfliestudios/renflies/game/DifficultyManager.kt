package com.renfliestudios.renflies.game

/**
 * Global difficulty system. The [GameEngine] reads [DifficultyManager.current]
 * every frame, so difficulty can be switched from the menu at any time.
 *
 * Logic matrix:
 * | Difficulty | Scroll | Pipe complexity     | Powerups | Special              |
 * |------------|--------|---------------------|----------|----------------------|
 * | EASY       | 0.75x  | Simple (2 patterns) | High     | Wide gap margins     |
 * | MEDIUM     | 1.0x   | Standard procedural | Moderate | Default              |
 * | HARD       | 1.25x  | High complexity     | Rare     | Tight gap margins    |
 * | DEVILISH   | 1.5x   | Maximum complexity  | Zero     | Loadout balancing    |
 */
enum class PipeComplexity {
    /** Alternates between exactly two spawn patterns. */
    SIMPLE,

    /** Uniform procedural generation. */
    STANDARD,

    /** Procedural with frequent extreme placements. */
    HIGH,

    /** Most extreme (but still navigable) procedural generation. */
    MAXIMUM
}

enum class Difficulty(
    val displayName: String,
    /** Multiplier applied to the base obstacle scroll speed. */
    val speedMultiplier: Float,
    /** Pipe layout generation strategy. */
    val complexity: PipeComplexity,
    /** Multiplier applied to the random powerup spawn interval (higher = rarer). */
    val powerupIntervalMultiplier: Float,
    /** Devilish disables powerup spawns entirely. */
    val powerupsEnabled: Boolean,
    /** Multiplier applied to the current gap size (Easy = wider gaps). */
    val gapSizeMultiplier: Float,
    /** Corridor margin in px kept between every pipe gap edge and floor/ceiling. */
    val edgeMargin: Float,
    /** Maximum vertical distance (px) between consecutive pipe gap centres. */
    val maxGapShift: Float
) {
    EASY("EASY", 0.75f, PipeComplexity.SIMPLE, 0.6f, true, 1.15f, 120f, 320f),
    MEDIUM("MEDIUM", 1.0f, PipeComplexity.STANDARD, 1.0f, true, 1.0f, 80f, 460f),
    HARD("HARD", 1.25f, PipeComplexity.HIGH, 1.8f, true, 0.9f, 60f, 600f),
    DEVILISH("DEVILISH", 1.5f, PipeComplexity.MAXIMUM, 1.0f, false, 0.82f, 50f, 700f)
}

/** Global access point for the currently selected difficulty. */
object DifficultyManager {
    var current: Difficulty = Difficulty.MEDIUM
}