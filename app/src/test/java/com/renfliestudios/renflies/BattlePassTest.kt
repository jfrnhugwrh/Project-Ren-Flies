package com.renfliestudios.renflies

import com.renfliestudios.renflies.data.BattlePass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BattlePassTest {

    @Test
    fun `xp for run combines score boss bonus and run bonus`() {
        // score 50, no bosses: 50 + 0 + 50 = 100
        assertEquals(100, BattlePass.xpForRun(score = 50, bossesDefeated = 0))
        // score 125, one boss: 125 + 250 + 50 = 425
        assertEquals(425, BattlePass.xpForRun(score = 125, bossesDefeated = 1))
    }

    @Test
    fun `level 1 starts at zero xp`() {
        assertEquals(1, BattlePass.levelForXp(0))
        assertEquals(1, BattlePass.levelForXp(99))
        assertEquals(0, BattlePass.xpForLevel(1))
    }

    @Test
    fun `levels unlock at correct xp thresholds`() {
        // Level 2 requires 100 XP, level 3 requires 250 XP (100 + 150).
        assertEquals(2, BattlePass.levelForXp(100))
        assertEquals(2, BattlePass.levelForXp(249))
        assertEquals(3, BattlePass.levelForXp(250))
        assertEquals(3, BattlePass.xpForLevel(3))
    }

    @Test
    fun `level never decreases with more xp`() {
        var last = 1
        for (xp in 0..20000 step 137) {
            val level = BattlePass.levelForXp(xp)
            assertTrue(level >= last)
            last = level
        }
    }

    @Test
    fun `progress reports fraction within the current level`() {
        val p = BattlePass.progressFor(totalXp = 175)
        assertEquals(2, p.level)
        assertEquals(75, p.xpIntoLevel)          // 175 - 100
        assertEquals(150, p.xpForNextLevel)      // 250 - 100
        assertEquals(0.5f, p.fraction, 0.001f)
        assertTrue(!p.isMaxLevel)
    }

    @Test
    fun `max level handled gracefully`() {
        val huge = 10_000_000
        val p = BattlePass.progressFor(huge)
        assertEquals(BattlePass.MAX_LEVEL, p.level)
        assertTrue(p.isMaxLevel)
        assertEquals(1f, p.fraction, 0.0001f)
    }

    @Test
    fun `every level has a reward and increasing thresholds`() {
        var previous = -1
        for (level in BattlePass.LEVELS) {
            assertTrue(level.xpRequired > previous || level.level == 1)
            previous = level.xpRequired
            assertTrue(level.reward.name.isNotBlank())
        }
        assertEquals(BattlePass.MAX_LEVEL, BattlePass.LEVELS.size)
    }
}
