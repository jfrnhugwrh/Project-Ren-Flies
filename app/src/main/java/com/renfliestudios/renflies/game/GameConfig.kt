package com.renfliestudios.renflies.game

/**
 * Central tuning constants for the game.
 *
 * The game simulates in a fixed virtual coordinate system (720 x 1280) so the
 * gameplay is identical on every device. The renderer scales this world onto
 * the actual screen.
 */
data class GameConfig(
    // Virtual world
    val worldWidth: Float = 720f,
    val worldHeight: Float = 1280f,
    val groundHeight: Float = 100f,

    // Player
    val playerX: Float = 180f,
    val playerRadius: Float = 26f,
    val gravity: Float = 1900f,
    val flapImpulse: Float = -620f,
    val maxFallSpeed: Float = 1000f,
    val ceilingBounceSpeed: Float = 120f,

    // Obstacles
    val obstacleWidth: Float = 96f,
    val obstacleBaseSpeed: Float = 260f,
    val obstacleMaxSpeed: Float = 480f,
    val obstacleSpeedPerPoint: Float = 2f,
    val baseGapSize: Float = 430f,
    val minGapSize: Float = 330f,
    val gapShrinkPerPoint: Float = 1f,
    val obstacleSpacing: Float = 380f,

    // Powerups
    val powerupRadius: Float = 30f,
    val powerupMinInterval: Float = 9f,
    val powerupMaxInterval: Float = 15f,
    val speedBoostDuration: Float = 6f,
    val speedBoostMultiplier: Float = 1.7f,
    val speedBoostGravityScale: Float = 0.75f,
    val berserkDuration: Float = 7f,
    val berserkFieldRadius: Float = 230f,
    val berserkPullSpeed: Float = 520f,

    // Boss
    val bossIntroDuration: Float = 2.5f,
    val bossClearDuration: Float = 2f,
    val bossRadius: Float = 62f,
    val bossHomeX: Float = 560f,
    val bossEnterSpeed: Float = 320f,
    val bossBobAmplitude: Float = 90f,
    val bossBobSpeed: Float = 1.6f,
    val bossBaseHp: Int = 8,
    val bossHpPerMilestone: Int = 2,
    val bossShieldDuration: Float = 5f,
    val bossEncounterLimit: Float = 25f,
    val bossPatternDuration: Float = 3.5f,
    val bossScoreBonus: Int = 25,
    val playerBulletSpeed: Float = 820f,
    val playerBulletCooldown: Float = 0.15f,
    val playerBulletRadius: Float = 9f,

    // Post-hit grace period after a shield absorbs a hit.
    val invulnerabilityDuration: Float = 1f,

    // Delta-time clamp so a stalled frame never teleports entities.
    val maxDeltaTime: Float = 0.05f
) {
    val groundY: Float get() = worldHeight - groundHeight

    fun obstacleSpeedFor(score: Int): Float =
        (obstacleBaseSpeed + score * obstacleSpeedPerPoint).coerceAtMost(obstacleMaxSpeed)

    fun gapSizeFor(score: Int): Float =
        (baseGapSize - score * gapShrinkPerPoint).coerceAtLeast(minGapSize)
}
