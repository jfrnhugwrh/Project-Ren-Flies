package com.renfliestudios.renflies.game

/**
 * High level phases of a run. Transitions are driven only by [GameEngine]
 * so they stay deterministic and easy to debug:
 *
 * MENU -> PLAYING -> BOSS_INTRO -> BOSS -> BOSS_CLEAR -> PLAYING -> ... -> GAME_OVER
 */
enum class GamePhase {
    MENU,
    PLAYING,
    BOSS_INTRO,
    BOSS,
    BOSS_CLEAR,
    GAME_OVER
}

/** Summary of a finished run, used for persistence, XP and the game over screen. */
data class RunResult(
    val score: Int,
    val bestScore: Int,
    val bossesDefeated: Int,
    val bossesEscaped: Int,
    val obstaclesPassed: Int,
    val xpEarned: Int,
    val newBest: Boolean
)
