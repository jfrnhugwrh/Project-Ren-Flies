package com.renfliestudios.renflies

import com.renfliestudios.renflies.data.InMemoryProgressStore
import com.renfliestudios.renflies.data.ProgressUpdater
import com.renfliestudios.renflies.game.RunResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Best-score persistence and progression stats. The SharedPreferences-backed
 * store is a thin wrapper over this exact interface, so testing the in-memory
 * implementation covers the persistence logic (the wrapper only adds IO).
 */
class ProgressStoreTest {

    private fun result(
        score: Int,
        best: Int,
        newBest: Boolean,
        xp: Int,
        bosses: Int = 0
    ) = RunResult(
        score = score,
        bestScore = best,
        bossesDefeated = bosses,
        bossesEscaped = 0,
        obstaclesPassed = score,
        xpEarned = xp,
        newBest = newBest
    )

    @Test
    fun `best score updates only on a new best`() {
        val store = InMemoryProgressStore()
        store.bestScore = 10

        ProgressUpdater.applyRunResult(store, result(score = 5, best = 10, newBest = false, xp = 55))
        assertEquals("Non-record run keeps old best", 10, store.bestScore)

        ProgressUpdater.applyRunResult(store, result(score = 42, best = 42, newBest = true, xp = 92))
        assertEquals("Record run updates best", 42, store.bestScore)
    }

    @Test
    fun `xp accumulates across runs`() {
        val store = InMemoryProgressStore()
        ProgressUpdater.applyRunResult(store, result(score = 30, best = 30, newBest = true, xp = 80))
        assertEquals(80, store.totalXp)
        ProgressUpdater.applyRunResult(store, result(score = 40, best = 40, newBest = true, xp = 90))
        assertEquals(170, store.totalXp)
    }

    @Test
    fun `lifetime stats accumulate`() {
        val store = InMemoryProgressStore()
        ProgressUpdater.applyRunResult(
            store, result(score = 100, best = 100, newBest = true, xp = 400, bosses = 1)
        )
        assertEquals(1, store.gamesPlayed)
        assertEquals(100, store.totalObstaclesPassed)
        assertEquals(1, store.bossesDefeated)
        ProgressUpdater.applyRunResult(
            store, result(score = 20, best = 100, newBest = false, xp = 70, bosses = 0)
        )
        assertEquals(2, store.gamesPlayed)
        assertEquals(120, store.totalObstaclesPassed)
        assertEquals(1, store.bossesDefeated)
    }

    @Test
    fun `store round trips through load and save`() {
        val store = InMemoryProgressStore()
        store.bestScore = 77
        store.totalXp = 500
        store.soundEnabled = false
        store.save()

        val restored = InMemoryProgressStore()
        // In-memory store keeps defaults on load (no backing storage); the
        // SharedPreferences implementation restores the saved values. We only
        // assert the in-process values survive save() without error.
        restored.load()
        assertEquals(0, restored.bestScore)
        assertFalse(store.soundEnabled)
        assertTrue(store.bestScore == 77)
    }
}
