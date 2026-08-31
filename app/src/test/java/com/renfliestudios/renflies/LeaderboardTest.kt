package com.renfliestudios.renflies

import com.renfliestudios.renflies.data.Leaderboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardTest {

    @Test
    fun `player entry is included and list is sorted descending`() {
        val entries = Leaderboard.entries(playerBestScore = 5000)
        assertTrue(entries.any { it.isPlayer && it.score == 5000 })
        for (i in 0 until entries.size - 1) {
            assertTrue(entries[i].score >= entries[i + 1].score)
        }
    }

    @Test
    fun `player with top score is ranked first`() {
        val entries = Leaderboard.entries(playerBestScore = 99999)
        assertEquals("You", entries.first().name)
        assertTrue(entries.first().isPlayer)
        assertEquals(99999, entries.first().score)
    }

    @Test
    fun `player with zero score still appears`() {
        val entries = Leaderboard.entries(playerBestScore = 0)
        assertEquals(Leaderboard.entries(0).size, entries.size)
        assertTrue(entries.last().score == 0 || entries.last().isPlayer)
        assertTrue(entries.any { it.isPlayer && it.score == 0 })
    }

    @Test
    fun `mock entries are present`() {
        val entries = Leaderboard.entries(playerBestScore = 1)
        assertTrue(entries.any { it.name == "SkyMaster" && it.score == 12450 })
        assertTrue(entries.any { it.name == "FlapKing" && it.score == 9320 })
    }
}
