package com.renfliestudios.renflies.data

import android.content.Context

/**
 * Local persistence for player progression. The interface is pure Kotlin so
 * tests can use [InMemoryProgressStore]; the app wires the SharedPreferences
 * implementation in [SharedPreferencesProgressStore].
 */
interface ProgressStore {
    var bestScore: Int
    var totalXp: Int
    var gamesPlayed: Int
    var totalObstaclesPassed: Int
    var bossesDefeated: Int
    var soundEnabled: Boolean

    fun load()
    fun save()
}

/** Simple in-memory store used by unit tests (and as a safe fallback). */
class InMemoryProgressStore : ProgressStore {
    override var bestScore: Int = 0
    override var totalXp: Int = 0
    override var gamesPlayed: Int = 0
    override var totalObstaclesPassed: Int = 0
    override var bossesDefeated: Int = 0
    override var soundEnabled: Boolean = true

    override fun load() = Unit
    override fun save() = Unit
}

/** Persists progression locally with SharedPreferences. No account needed. */
class SharedPreferencesProgressStore(context: Context) : ProgressStore {

    private val prefs = context.getSharedPreferences("renflies_progress", Context.MODE_PRIVATE)

    override var bestScore: Int = 0
    override var totalXp: Int = 0
    override var gamesPlayed: Int = 0
    override var totalObstaclesPassed: Int = 0
    override var bossesDefeated: Int = 0
    override var soundEnabled: Boolean = true

    override fun load() {
        bestScore = prefs.getInt(KEY_BEST_SCORE, 0)
        totalXp = prefs.getInt(KEY_TOTAL_XP, 0)
        gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0)
        totalObstaclesPassed = prefs.getInt(KEY_OBSTACLES, 0)
        bossesDefeated = prefs.getInt(KEY_BOSSES, 0)
        soundEnabled = prefs.getBoolean(KEY_SOUND, true)
    }

    override fun save() {
        prefs.edit()
            .putInt(KEY_BEST_SCORE, bestScore)
            .putInt(KEY_TOTAL_XP, totalXp)
            .putInt(KEY_GAMES_PLAYED, gamesPlayed)
            .putInt(KEY_OBSTACLES, totalObstaclesPassed)
            .putInt(KEY_BOSSES, bossesDefeated)
            .putBoolean(KEY_SOUND, soundEnabled)
            .apply()
    }

    private companion object {
        const val KEY_BEST_SCORE = "best_score"
        const val KEY_TOTAL_XP = "total_xp"
        const val KEY_GAMES_PLAYED = "games_played"
        const val KEY_OBSTACLES = "obstacles_passed"
        const val KEY_BOSSES = "bosses_defeated"
        const val KEY_SOUND = "sound_enabled"
    }
}

/**
 * Applies a finished run to the store: best score, XP, lifetime stats.
 * Pure logic lives here so it can be tested without Android.
 */
object ProgressUpdater {
    fun applyRunResult(store: ProgressStore, result: com.renfliestudios.renflies.game.RunResult) {
        if (result.newBest) store.bestScore = result.bestScore
        store.totalXp += result.xpEarned
        store.gamesPlayed += 1
        store.totalObstaclesPassed += result.obstaclesPassed
        store.bossesDefeated += result.bossesDefeated
        store.save()
    }
}
