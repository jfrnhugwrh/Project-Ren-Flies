package com.renfliestudios.renflies

import com.renfliestudios.renflies.game.Difficulty
import com.renfliestudios.renflies.game.DifficultyManager
import com.renfliestudios.renflies.game.GameConfig
import com.renfliestudios.renflies.game.GameEngine
import com.renfliestudios.renflies.game.GamePhase
import com.renfliestudios.renflies.game.Obstacle
import com.renfliestudios.renflies.game.PowerUpType
import com.renfliestudios.renflies.game.audio.AudioFeedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for the pure-Kotlin game engine. The tests drive the engine with
 * a deterministic seed and a simple "autopilot" that pins the bird to the gap
 * center (and clears boss bullets) so runs are reproducible.
 */
class GameEngineTest {

    private val dt = 1f / 60f

    private fun newEngine(
        seed: Long = 42L,
        bestScore: Int = 0,
        audio: AudioFeedback = AudioFeedback.NoOp,
        onRunEnded: ((com.renfliestudios.renflies.game.RunResult) -> Unit)? = null
    ): GameEngine = GameEngine(
        config = GameConfig(),
        initialBestScore = bestScore,
        random = Random(seed),
        audio = audio,
        onRunEnded = onRunEnded
    )

    private fun step(engine: GameEngine) {
        // Autopilot: hover the bird at a fixed height so it never dies.
        engine.player.vy = 0f
        engine.player.y = 400f
        engine.update(dt)
    }

    private fun stepSeconds(engine: GameEngine, seconds: Float) {
        var remaining = seconds
        while (remaining > 0f) {
            step(engine)
            remaining -= dt
        }
    }

    /** Like [stepSeconds] but also clears boss bullets so the bird survives duels. */
    private fun stepSecondsGodMode(engine: GameEngine, seconds: Float) {
        var remaining = seconds
        while (remaining > 0f) {
            step(engine)
            engine.bossBullets.clear()
            remaining -= dt
        }
    }

    /** Autopilot that flies through the gap of the nearest obstacle. */
    private fun stepPlayingUntilScore(engine: GameEngine, targetScore: Int, maxSeconds: Float = 400f) {
        var elapsed = 0f
        while (engine.score < targetScore && elapsed < maxSeconds) {
            val nearest = engine.obstacles
                // Match the engine's score/collision geometry: a pipe is "the
                // current one" until its right edge clears the bird's LEFT edge
                // (player.x - radius), not until it clears the bird's centre.
                .filter { !it.scored && it.x + it.width > engine.player.x - engine.player.radius }
                .minByOrNull { it.x }
            if (nearest != null && nearest.x < 500f) {
                engine.player.y = nearest.gapCenter
                engine.player.vy = 0f
            } else {
                engine.player.y = 640f
                engine.player.vy = 0f
            }
            engine.update(dt)
            elapsed += dt
        }
        assertTrue("Score did not reach $targetScore in time", engine.score >= targetScore)
    }

    // ---- Score ------------------------------------------------------------

    @Test
    fun `score increments when obstacle passes player`() {
        val engine = newEngine()
        engine.startRun()

        val before = engine.score
        engine.spawnObstacle(gapCenter = 400f)
        val o = engine.obstacles.last()

        var frames = 0
        while (!o.scored && frames < 600) {
            engine.player.vy = 0f
            engine.player.y = 400f
            engine.update(dt)
            frames++
        }
        assertEquals(before + 1, engine.score)
        assertEquals(1, engine.obstaclesPassed)
    }

    @Test
    fun `delta time scaling moves obstacles consistently`() {
        val a = newEngine()
        val b = newEngine()
        a.startRun()
        b.startRun()
        a.spawnObstacle(400f)
        b.spawnObstacle(400f)

        // 60 frames at dt=1/60 (1s) vs 20 frames at dt=1/6 (clamped to 0.05 each = 1s).
        repeat(60) {
            a.player.vy = 0f
            a.player.y = 400f
            a.update(dt)
        }
        repeat(20) {
            b.player.vy = 0f
            b.player.y = 400f
            b.update(1f / 6f)
        }
        val ax = a.obstacles.first().x
        val bx = b.obstacles.first().x
        assertTrue("Expected similar obstacle x, got $ax vs $bx", Math.abs(ax - bx) < 2f)
    }

    // ---- Boss milestones --------------------------------------------------

    @Test
    fun `hundred point milestone triggers boss intro then boss`() {
        val engine = newEngine()
        engine.startRun()
        stepPlayingUntilScore(engine, targetScore = 100)

        assertEquals("Boss intro should trigger at 100", GamePhase.BOSS_INTRO, engine.phase)
        assertNull(engine.boss)

        stepSeconds(engine, 3f) // intro duration is 2.5s
        assertEquals("Boss phase should start after the intro", GamePhase.BOSS, engine.phase)
        assertNotNull(engine.boss)
        assertEquals("Milestone must not advance before the encounter ends", 100, engine.nextBossMilestone)
    }

    @Test
    fun `boss milestone triggers exactly once and next boss comes at 200`() {
        val engine = newEngine()
        engine.startRun()
        stepPlayingUntilScore(engine, targetScore = 100)

        assertEquals(GamePhase.BOSS_INTRO, engine.phase)
        stepSeconds(engine, 3f)
        assertEquals(GamePhase.BOSS, engine.phase)

        // God-mode duel: clear bullets each frame, keep shooting at the boss.
        var frames = 0
        while (engine.phase == GamePhase.BOSS && frames < 60 * 60) {
            engine.player.vy = 0f
            engine.player.y = 400f
            engine.flap() // fires a player bullet during the boss phase
            engine.update(dt)
            engine.bossBullets.clear()
            frames++
        }
        assertEquals("Boss should be defeated", GamePhase.BOSS_CLEAR, engine.phase)
        assertTrue(engine.bossClearMessage.startsWith("BOSS CLEARED"))
        assertEquals("Milestone advances after the encounter", 200, engine.nextBossMilestone)
        assertEquals(1, engine.bossesDefeated)

        stepSeconds(engine, 2.5f)
        assertEquals("Normal gameplay resumes", GamePhase.PLAYING, engine.phase)

        stepPlayingUntilScore(engine, targetScore = 200)
        assertEquals("Second boss triggers at 200", GamePhase.BOSS_INTRO, engine.phase)
    }

    // ---- Powerups ---------------------------------------------------------

    @Test
    fun `shield absorbs exactly one obstacle hit per stack`() {
        val engine = newEngine()
        engine.startRun()
        engine.collectPowerUp(PowerUpType.SHIELD)
        assertTrue(engine.player.hasShield)

        // Force a collision: gap well above the bird so it overlaps the pipe.
        val o = Obstacle(engine.config, 700f, 400f)
        o.x = engine.config.playerX - engine.config.obstacleWidth / 2f
        engine.obstacles.add(o)
        step(engine)

        assertEquals("Player should survive the hit", GamePhase.PLAYING, engine.phase)
        assertFalse("Shield stack must be consumed", engine.player.hasShield)
        assertTrue("Obstacle removed on shield break", engine.obstacles.isEmpty())

        // A second hit (after the brief invulnerability) ends the run.
        stepSeconds(engine, 1.2f)
        val o2 = Obstacle(engine.config, 700f, 400f)
        o2.x = engine.config.playerX - engine.config.obstacleWidth / 2f
        engine.obstacles.add(o2)
        step(engine)
        assertEquals("Second hit without shield ends the run", GamePhase.GAME_OVER, engine.phase)
    }

    @Test
    fun `shield stacks cap at five and each pipe hit consumes one stack`() {
        val engine = newEngine()
        engine.startRun()

        // Six pickups, only five stacks stick.
        repeat(6) { engine.collectPowerUp(PowerUpType.SHIELD) }
        assertEquals(5, engine.player.shieldStacks)

        repeat(5) {
            val o = Obstacle(engine.config, 700f, 400f)
            o.x = engine.config.playerX - engine.config.obstacleWidth / 2f
            engine.obstacles.add(o)
            step(engine)
            stepSeconds(engine, 1.2f) // outlast the invulnerability grace
            assertEquals(GamePhase.PLAYING, engine.phase)
            assertEquals(4 - it, engine.player.shieldStacks)
        }

        // Sixth hit with zero stacks left is fatal.
        val o = Obstacle(engine.config, 700f, 400f)
        o.x = engine.config.playerX - engine.config.obstacleWidth / 2f
        engine.obstacles.add(o)
        step(engine)
        assertEquals(GamePhase.GAME_OVER, engine.phase)
    }

    @Test
    fun `ceiling collision ends the run regardless of shield stacks`() {
        val engine = newEngine()
        engine.startRun()
        repeat(5) { engine.collectPowerUp(PowerUpType.SHIELD) }
        assertEquals(5, engine.player.shieldStacks)

        // Slam into the ceiling: shields never protect against it.
        engine.player.y = engine.player.radius
        engine.player.vy = -800f
        step(engine)

        assertEquals(GamePhase.GAME_OVER, engine.phase)
    }

    @Test
    fun `each shield stack adds an incremental gravity weight penalty`() {
        val base = newEngine()
        val heavy = newEngine()
        base.startRun()
        heavy.startRun()
        repeat(5) { heavy.collectPowerUp(PowerUpType.SHIELD) }

        base.player.y = 300f
        base.player.vy = 0f
        heavy.player.y = 300f
        heavy.player.vy = 0f
        step(base)
        step(heavy)

        assertTrue(
            "Heavier bird must fall further in the same frame",
            heavy.player.y > base.player.y
        )
    }

    @Test
    fun `speed boost expiry flashes and clears every on-screen obstacle`() {
        val engine = newEngine()
        engine.startRun()
        engine.collectPowerUp(PowerUpType.SPEED_BOOST)

        val o = Obstacle(engine.config, 700f, 400f)
        o.x = engine.config.playerX
        engine.obstacles.add(o)

        // Step until the boost has just expired.
        var frames = 0
        while (engine.isSpeedBoostActive && frames < 600) {
            engine.player.vy = 0f
            engine.player.y = 400f
            engine.update(dt)
            frames++
        }
        assertFalse(engine.isSpeedBoostActive)
        assertTrue("White flash must be visible right after expiry", engine.isSpeedFlashing)
        assertTrue("All on-screen obstacles must despawn", engine.obstacles.isEmpty())
    }

    @Test
    fun `speed boost prevents normal obstacle damage while active`() {
        val engine = newEngine()
        engine.startRun()
        engine.collectPowerUp(PowerUpType.SPEED_BOOST)
        assertTrue(engine.isSpeedBoostActive)

        val o = Obstacle(engine.config, 700f, 400f)
        o.x = engine.config.playerX - engine.config.obstacleWidth / 2f
        engine.obstacles.add(o)
        step(engine)

        assertEquals("Boost lets the bird pass through obstacles", GamePhase.PLAYING, engine.phase)

        // After the boost expires, the same collision would be lethal.
        stepSeconds(engine, engine.config.speedBoostDuration + 0.2f)
        assertFalse(engine.isSpeedBoostActive)
        val o2 = Obstacle(engine.config, 700f, 400f)
        o2.x = engine.config.playerX - engine.config.obstacleWidth / 2f
        engine.obstacles.add(o2)
        step(engine)
        assertEquals(GamePhase.GAME_OVER, engine.phase)
    }

    @Test
    fun `berserker destroys obstacles inside its radius and awards score`() {
        val engine = newEngine()
        engine.startRun()
        engine.collectPowerUp(PowerUpType.BERSERKER)
        assertTrue(engine.isBerserkActive)

        val before = engine.score
        val o = Obstacle(engine.config, 400f, 400f)
        o.x = engine.player.x + 80f // inside the 230px field radius
        engine.obstacles.add(o)

        var frames = 0
        while (engine.obstacles.isNotEmpty() && frames < 300) {
            engine.player.vy = 0f
            engine.player.y = 400f
            engine.update(dt)
            frames++
        }
        assertTrue("Obstacle should be destroyed by the field", engine.obstacles.isEmpty())
        assertEquals("Destroyed obstacle awards score", before + 1, engine.score)
    }

    // ---- Boss shield & bullets ---------------------------------------------

    @Test
    fun `boss shield prevents boss damage while active`() {
        val engine = newEngine()
        engine.startRun()
        stepPlayingUntilScore(engine, targetScore = 100)
        stepSeconds(engine, 3f)
        assertEquals(GamePhase.BOSS, engine.phase)
        val boss = engine.boss!!
        assertTrue(boss.shieldActive)

        val maxHp = boss.maxHp
        // Shoot at the shielded boss.
        repeat(10) {
            engine.player.vy = 0f
            engine.player.y = 400f
            engine.flap()
            engine.update(dt)
            engine.bossBullets.clear()
        }
        assertEquals("Shield must block all damage", maxHp, boss.hp)
        assertTrue(boss.shieldActive)

        // Let the shield drain (fight starts ~0.3s into this step; shield lasts 5s).
        stepSecondsGodMode(engine, 6.5f)
        assertFalse(boss.shieldActive)
        var frames = 0
        while (boss.hp == maxHp && frames < 300) {
            engine.player.vy = 0f
            engine.player.y = 400f
            engine.flap()
            engine.update(dt)
            engine.bossBullets.clear()
            frames++
        }
        assertTrue("Boss should take damage after shield breaks", boss.hp < maxHp)
    }

    @Test
    fun `boss bullets ignore shield stacks and end the run`() {
        val engine = newEngine()
        engine.startRun()
        stepPlayingUntilScore(engine, targetScore = 100)
        stepSeconds(engine, 3f)
        assertEquals(GamePhase.BOSS, engine.phase)
        engine.collectPowerUp(PowerUpType.SHIELD)
        engine.collectPowerUp(PowerUpType.SHIELD)
        assertTrue(engine.player.hasShield)

        // Place a boss bullet on top of the player and let it register.
        // Shields protect ONLY against pipe collisions.
        val bullet = com.renfliestudios.renflies.game.Bullet()
        bullet.launch(
            engine.player.x, engine.player.y, 0f, 0f, 11f, fromPlayer = false
        )
        engine.bossBullets.add(bullet)
        engine.player.vy = 0f
        engine.player.y = 400f
        engine.update(dt)

        assertEquals("Bullets must bypass shield stacks", GamePhase.GAME_OVER, engine.phase)
    }

    // ---- Difficulty system ---------------------------------------------------

    @Test
    fun `difficulty scales scroll speed`() {
        val easy = newEngine()
        DifficultyManager.current = Difficulty.EASY
        easy.startRun()
        easy.spawnObstacle(400f)

        val hard = newEngine()
        DifficultyManager.current = Difficulty.HARD
        hard.startRun()
        hard.spawnObstacle(400f)

        repeat(60) {
            easy.player.vy = 0f; easy.player.y = 400f; easy.update(dt)
            hard.player.vy = 0f; hard.player.y = 400f; hard.update(dt)
        }
        val easyX = easy.obstacles.first().x
        val hardX = hard.obstacles.first().x
        assertTrue("Hard (1.25x) must scroll faster than Easy (0.75x)", hardX < easyX)
        DifficultyManager.current = Difficulty.MEDIUM
    }

    @Test
    fun `devilish mode never spawns powerups`() {
        DifficultyManager.current = Difficulty.DEVILISH
        val engine = newEngine()
        engine.startRun()
        var elapsed = 0f
        while (engine.phase == GamePhase.PLAYING && elapsed < 40f) {
            // Gap-following autopilot keeps the bird alive indefinitely.
            val nearest = engine.obstacles
                .filter { !it.scored && it.x + it.width > engine.player.x - engine.player.radius }
                .minByOrNull { it.x }
            if (nearest != null && nearest.x < 500f) {
                engine.player.y = nearest.gapCenter
            } else {
                engine.player.y = 640f
            }
            engine.player.vy = 0f
            engine.update(dt)
            elapsed += dt
        }
        assertEquals(GamePhase.PLAYING, engine.phase)
        assertTrue("Devilish must have zero powerup spawns", engine.powerups.isEmpty())
        DifficultyManager.current = Difficulty.MEDIUM
    }

    @Test
    fun `consecutive gap centers stay within the max shift clamp`() {
        DifficultyManager.current = Difficulty.HARD
        val engine = newEngine()
        engine.startRun()
        val centers = ArrayList<Float>()
        var elapsed = 0f
        while (elapsed < 50f && engine.phase == GamePhase.PLAYING) {
            val nearest = engine.obstacles
                .filter { !it.scored && it.x + it.width > engine.player.x - engine.player.radius }
                .minByOrNull { it.x }
            if (nearest != null && nearest.x < 500f) {
                engine.player.y = nearest.gapCenter
            } else {
                engine.player.y = 640f
            }
            engine.player.vy = 0f
            engine.update(dt)
            elapsed += dt
            // A fresh spawn sits exactly at the right edge on its first frame.
            engine.obstacles.firstOrNull {
                it.x >= engine.config.worldWidth + engine.config.obstacleWidth - 0.5f
            }?.let {
                if (centers.isEmpty() || centers.last() != it.gapCenter) centers.add(it.gapCenter)
            }
        }
        assertTrue("Expected many spawns, got ${centers.size}", centers.size >= 10)
        val maxShift = Difficulty.HARD.maxGapShift
        for (i in 1 until centers.size) {
            assertTrue(
                "Gap moved " + Math.abs(centers[i] - centers[i - 1]) + "px > $maxShift",
                Math.abs(centers[i] - centers[i - 1]) <= maxShift + 1f
            )
        }
        DifficultyManager.current = Difficulty.MEDIUM
    }

    // ---- Loadout ---------------------------------------------------------

    @Test
    fun `loadout charges are stored once per run and usable mid-run`() {
        val engine = newEngine()
        engine.startRun(
            mapOf(PowerUpType.SHIELD to 2, PowerUpType.SPEED_BOOST to 1)
        )
        assertEquals(2, engine.storedPowerupCount(PowerUpType.SHIELD))
        assertEquals(1, engine.storedPowerupCount(PowerUpType.SPEED_BOOST))

        assertTrue(engine.useStoredPowerUp(PowerUpType.SPEED_BOOST))
        assertFalse("Charges are single-use", engine.useStoredPowerUp(PowerUpType.SPEED_BOOST))
        assertTrue(engine.isSpeedBoostActive)

        assertTrue(engine.useStoredPowerUp(PowerUpType.SHIELD))
        assertEquals(1, engine.player.shieldStacks)
        assertEquals(1, engine.storedPowerupCount(PowerUpType.SHIELD))

        // A fresh run resets the loadout charges.
        engine.startRun()
        assertEquals(0, engine.storedPowerupCount(PowerUpType.SHIELD))
    }

    @Test
    fun `berserker shifts upcoming pipes away from the bird and absorbs projectiles`() {
        val engine = newEngine()
        engine.startRun()
        engine.collectPowerUp(PowerUpType.BERSERKER)

        val o = Obstacle(engine.config, engine.player.y + 20f, 400f)
        o.x = engine.player.x + 700f // upcoming, outside the field radius
        engine.obstacles.add(o)
        val before = o.gapCenter

        var hookCalls = 0
        engine.onProjectileAbsorbed = { _, _ -> hookCalls++ }
        val bullet = com.renfliestudios.renflies.game.Bullet()
        bullet.launch(engine.player.x, engine.player.y, 0f, 0f, 11f, fromPlayer = false)
        engine.bossBullets.add(bullet)

        repeat(30) {
            engine.player.vy = 0f
            engine.player.y = 400f
            engine.update(dt)
        }

        assertTrue("Upcoming gap must drift away from the bird", o.gapCenter > before)
        assertEquals(1, engine.projectilesAbsorbed)
        assertEquals(1, hookCalls)
        assertTrue(engine.bossBullets.isEmpty())
    }
}
