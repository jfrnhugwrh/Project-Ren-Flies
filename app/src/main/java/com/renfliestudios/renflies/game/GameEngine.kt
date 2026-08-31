package com.renfliestudios.renflies.game

import com.renfliestudios.renflies.game.audio.AudioFeedback
import com.renfliestudios.renflies.data.BattlePass
import kotlin.random.Random

/**
 * The pure-Kotlin game engine. Owns all gameplay state and advances it with
 * delta-time updates. Contains no Android dependencies so it can be unit
 * tested directly (see app/src/test).
 *
 * The world is simulated in a fixed virtual coordinate system defined by
 * [GameConfig]; the renderer maps it onto the real screen.
 */
class GameEngine(
    val config: GameConfig = GameConfig(),
    initialBestScore: Int = 0,
    private val random: Random = Random(System.nanoTime()),
    val audio: AudioFeedback = AudioFeedback.NoOp,
    private val onRunEnded: ((RunResult) -> Unit)? = null
) {
    // ---- Core state -------------------------------------------------------
    var phase: GamePhase = GamePhase.MENU
        private set
    var score: Int = 0
        private set
    var bestScore: Int = initialBestScore
        private set
    var obstaclesPassed: Int = 0
        private set
    var bossesDefeated: Int = 0
        private set
    var bossesEscaped: Int = 0
        private set
    var lastRunResult: RunResult? = null
        private set

    val player = Player(config)
    val obstacles = ArrayList<Obstacle>()
    val bossBullets = ArrayList<Bullet>()
    val playerBullets = ArrayList<Bullet>()
    val powerups = ArrayList<PowerUp>()

    var boss: Boss? = null
        private set

    // ---- Powerup state ----------------------------------------------------
    var activePowerup: PowerUpType? = null
        private set
    var speedBoostTimer: Float = 0f
        private set
    var berserkTimer: Float = 0f
        private set

    // ---- Internal timers --------------------------------------------------
    private var phaseTimer = 0f
    private var obstacleTimer = 0f
    private var powerupTimer = 0f
    private var bulletCooldown = 0f
    private var nextMilestone = 100
    private var bossClearWasDefeat = false

    val isSpeedBoostActive: Boolean get() = speedBoostTimer > 0f
    val isBerserkActive: Boolean get() = berserkTimer > 0f
    val nextBossMilestone: Int get() = nextMilestone

    /** Message shown during BOSS_CLEAR, e.g. "BOSS CLEARED! +25". */
    var bossClearMessage: String = ""
        private set

    // ---- Lifecycle --------------------------------------------------------

    /** Starts a fresh run from the menu or after game over. */
    fun startRun() {
        score = 0
        obstaclesPassed = 0
        bossesDefeated = 0
        bossesEscaped = 0
        obstacles.clear()
        bossBullets.clear()
        playerBullets.clear()
        powerups.clear()
        boss = null
        activePowerup = null
        speedBoostTimer = 0f
        berserkTimer = 0f
        phaseTimer = 0f
        obstacleTimer = 1.2f
        powerupTimer = random.nextFloat() * (config.powerupMaxInterval - config.powerupMinInterval) +
            config.powerupMinInterval
        bulletCooldown = 0f
        nextMilestone = 100
        bossClearMessage = ""
        player.reset()
        lastRunResult = null
        phase = GamePhase.PLAYING
    }

    /**
     * A tap on the screen. During normal play it flaps the bird; during the
     * boss fight it also fires a shot at the boss.
     */

    // ---- Main update ------------------------------------------------------

    /** Advances the simulation. [dt] is the frame delta time in seconds. */
    fun update(dt: Float) {
        if (dt <= 0f) return
        val clamped = dt.coerceAtMost(config.maxDeltaTime)
        when (phase) {
            GamePhase.MENU, GamePhase.GAME_OVER -> Unit
            GamePhase.PLAYING -> updatePlaying(clamped)
            GamePhase.BOSS_INTRO -> updateBossIntro(clamped)
            GamePhase.BOSS -> updateBossPhase(clamped)
            GamePhase.BOSS_CLEAR -> updateBossClear(clamped)
        }
    }

    private fun updatePlaying(dt: Float) {
        updateAmbient(dt)
        updateBerserkField(dt)

        val speed = config.obstacleSpeedFor(score) *
            (if (isSpeedBoostActive) config.speedBoostMultiplier else 1f)

        // Spawn obstacles.
        obstacleTimer -= dt
        if (obstacleTimer <= 0f) {
            val margin = 80f
            val halfGap = config.gapSizeFor(score) / 2f
            val low = halfGap + margin
            val high = config.groundY - halfGap - margin
            spawnObstacle(low + random.nextFloat() * (high - low))
            obstacleTimer = config.obstacleSpacing / speed
        }

        // Move obstacles, award pass scores.
        val it1 = obstacles.iterator()
        while (it1.hasNext()) {
            val o = it1.next()
            if (!o.pulled) o.update(dt, speed)
            if (o.isOffScreen) {
                it1.remove()
                continue
            }
            if (!o.scored && o.x + o.width < player.x - player.radius) {
                o.scored = true
                score++
                obstaclesPassed++
                audio.score()
                checkMilestone()
                if (phase != GamePhase.PLAYING) return
            }
        }

        // Bird vs obstacles.
        for (o in obstacles) {
            if (isSpeedBoostActive || o.pulled) continue
            if (playerHitsObstacle(o)) {
                onPlayerHitByObstacle(o)
                if (phase != GamePhase.PLAYING) return
            }
        }

        // Powerup pickup.
        collectPowerUpCollisions()

        // Spawn powerups.
        powerupTimer -= dt
        if (powerupTimer <= 0f) {
            spawnPowerUp()
            powerupTimer = random.nextFloat() *
                (config.powerupMaxInterval - config.powerupMinInterval) +
                config.powerupMinInterval
        }
    }

    private fun updateBossIntro(dt: Float) {
        updateAmbient(dt)
        phaseTimer -= dt
        if (phaseTimer <= 0f) {
            boss = Boss(config, nextMilestone)
            phase = GamePhase.BOSS
        }
    }

    private fun updateBossPhase(dt: Float) {
        updateAmbient(dt)
        bulletCooldown -= dt
        if (bulletCooldown < 0f) bulletCooldown = 0f

        val b = boss ?: return
        b.update(dt)

        // Boss fires bullet patterns.
        if (b.wantsToFire) {
            for (intent in b.performFire(player.x, player.y)) {
                val bullet = Bullet()
                bullet.launch(intent.x, intent.y, intent.vx, intent.vy, 11f, fromPlayer = false)
                bossBullets.add(bullet)
            }
            audio.bossShoot()
        }

        // Berserker field clears nearby boss bullets (defensive only).
        updateBerserkField(dt)

        // Player bullets vs boss.
        val bit = playerBullets.iterator()
        while (bit.hasNext()) {
            val bullet = bit.next()
            bullet.update(dt)
            if (bullet.isOffScreen) {
                bit.remove()
                continue
            }
            if (CollisionSystem.circlesOverlap(
                    bullet.x, bullet.y, bullet.radius, b.x, b.y, b.radius
                )
            ) {
                bit.remove()
                if (b.hit(1)) audio.bossHit()
            }
        }

        // Boss bullets vs player.
        val bbit = bossBullets.iterator()
        while (bbit.hasNext()) {
            val bullet = bbit.next()
            bullet.update(dt)
            if (bullet.isOffScreen || !bullet.active) {
                bbit.remove()
                continue
            }
            if (CollisionSystem.circlesOverlap(
                    bullet.x, bullet.y, bullet.radius, player.x, player.y, player.radius
                )
            ) {
                bbit.remove()
                onPlayerHitByBullet()
                if (phase != GamePhase.BOSS) return
            }
        }

        // Encounter resolution.
        when (b.visualState) {
            Boss.BossVisualState.DEFEATED -> {
                bossesDefeated++
                score += config.bossScoreBonus
                bossClearWasDefeat = true
                bossClearMessage = "BOSS CLEARED! +${config.bossScoreBonus}"
                nextMilestone += 100
                audio.bossClear()
                phase = GamePhase.BOSS_CLEAR
                phaseTimer = config.bossClearDuration
                boss = null
                bossBullets.clear()
                playerBullets.clear()
            }
            Boss.BossVisualState.RETREATING -> {
                if (b.isOffScreen) {
                    bossesEscaped++
                    bossClearWasDefeat = false
                    bossClearMessage = "BOSS ESCAPED!"
                    nextMilestone += 100
                    phase = GamePhase.BOSS_CLEAR
                    phaseTimer = config.bossClearDuration
                    boss = null
                    bossBullets.clear()
                    playerBullets.clear()
                }
            }
            else -> Unit
        }
    }

    private fun updateBossClear(dt: Float) {
        updateAmbient(dt)
        phaseTimer -= dt
        if (phaseTimer <= 0f) {
            bossClearMessage = ""
            phase = GamePhase.PLAYING
            obstacleTimer = 0.8f
        }
    }

    // ---- Shared per-frame systems ------------------------------------------

    /** Player physics, powerup timers, powerup drift; runs in all active phases. */
    private fun updateAmbient(dt: Float) {
        val gravityScale = if (isSpeedBoostActive) config.speedBoostGravityScale else 1f
        player.update(dt, gravityScale)

        if (player.isOnGround) {
            endRun()
            return
        }

        if (speedBoostTimer > 0f) {
            speedBoostTimer -= dt
            if (speedBoostTimer <= 0f) {
                speedBoostTimer = 0f
                if (activePowerup == PowerUpType.SPEED_BOOST) activePowerup = null
            }
        }
        if (berserkTimer > 0f) {
            berserkTimer -= dt
            if (berserkTimer <= 0f) {
                berserkTimer = 0f
                if (activePowerup == PowerUpType.BERSERKER) activePowerup = null
            }
        }

        val speed = config.obstacleSpeedFor(score) *
            (if (isSpeedBoostActive) config.speedBoostMultiplier else 1f)
        val pit = powerups.iterator()
        while (pit.hasNext()) {
            val p = pit.next()
            p.update(dt, speed)
            if (p.isOffScreen) pit.remove()
        }
    }

    /**
     * The Berserker field: pulls obstacles in and destroys them for score,
     * and vaporizes boss bullets (defensively) without damaging the boss.
     */
    private fun updateBerserkField(dt: Float) {
        if (!isBerserkActive) return

        val radius = config.berserkFieldRadius
        val oit = obstacles.iterator()
        while (oit.hasNext()) {
            val o = oit.next()
            val dist = minOf(
                CollisionSystem.distanceToRect(
                    player.x, player.y, o.topRect[0], o.topRect[1], o.topRect[2], o.topRect[3]
                ),
                CollisionSystem.distanceToRect(
                    player.x, player.y, o.bottomRect[0], o.bottomRect[1], o.bottomRect[2], o.bottomRect[3]
                )
            )
            if (dist <= radius) {
                if (!o.pulled) o.pulled = true
                o.pullToward(player.x, player.y, dt, config.berserkPullSpeed)
                val newDist = CollisionSystem.distanceToRect(
                    player.x, player.y,
                    o.topRect[0], o.topRect[1], o.topRect[2], o.topRect[3]
                )
                if (newDist <= 4f) {
                    // Obstacle consumed by the field: reward the player.
                    oit.remove()
                    score++
                    obstaclesPassed++
                    audio.score()
                    checkMilestone()
                    if (phase != GamePhase.PLAYING && phase != GamePhase.BOSS) return
                }
            }
        }

        val bit = bossBullets.iterator()
        while (bit.hasNext()) {
            val bullet = bit.next()
            val dx = bullet.x - player.x
            val dy = bullet.y - player.y
            if (dx * dx + dy * dy <= radius * radius) {
                bit.remove()
            }
        }
    }

    private fun collectPowerUpCollisions() {
        val it = powerups.iterator()
        while (it.hasNext()) {
            val p = it.next()
            if (CollisionSystem.circlesOverlap(
                    player.x, player.y, player.radius, p.x, p.displayY, p.radius
                )
            ) {
                it.remove()
                collectPowerUp(p.type)
            }
        }
    }

    private fun spawnPowerUp() {
        val roll = random.nextFloat()
        val type = when {
            roll < 0.40f -> PowerUpType.SHIELD
            roll < 0.75f -> PowerUpType.SPEED_BOOST
            else -> PowerUpType.BERSERKER
        }
        val p = PowerUp(type)
        val y = 200f + random.nextFloat() * (config.groundY - 400f)
        p.spawn(config.worldWidth + config.powerupRadius, y)
        powerups.add(p)
    }

    private fun playerHitsObstacle(o: Obstacle): Boolean {
        val r = player.radius
        return CollisionSystem.circleIntersectsRect(
            player.x, player.y, r,
            o.topRect[0], o.topRect[1], o.topRect[2], o.topRect[3]
        ) || CollisionSystem.circleIntersectsRect(
            player.x, player.y, r,
            o.bottomRect[0], o.bottomRect[1], o.bottomRect[2], o.bottomRect[3]
        )
    }

    private fun onPlayerHitByObstacle(o: Obstacle) {
        if (player.hasShield) {
            // One-hit shield: absorb exactly this collision.
            player.hasShield = false
            player.invulnTimer = config.invulnerabilityDuration
            activePowerup = null
            obstacles.remove(o)
            audio.shieldBreak()
        } else if (player.invulnTimer > 0f) {
            // Brief grace period after a shield break.
            return
        } else {
            endRun()
        }
    }

    private fun onPlayerHitByBullet() {
        if (player.hasShield) {
            player.hasShield = false
            player.invulnTimer = config.invulnerabilityDuration
            activePowerup = null
            audio.shieldBreak()
        } else if (player.invulnTimer > 0f) {
            return
        } else {
            endRun()
        }
    }

    private fun checkMilestone() {
        if (score >= nextMilestone) startBossIntro()
    }

    private fun startBossIntro() {
        phase = GamePhase.BOSS_INTRO
        phaseTimer = config.bossIntroDuration
        // Sweep the play field so the duel starts clean.
        obstacles.clear()
        bossBullets.clear()
        audio.bossWarning()
    }

    private fun endRun() {
        phase = GamePhase.GAME_OVER
        audio.gameOver()
        val newBest = score > bestScore
        if (newBest) bestScore = score
        val result = RunResult(
            score = score,
            bestScore = bestScore,
            bossesDefeated = bossesDefeated,
            bossesEscaped = bossesEscaped,
            obstaclesPassed = obstaclesPassed,
            xpEarned = BattlePass.xpForRun(score, bossesDefeated),
            newBest = newBest
        )
        lastRunResult = result
        onRunEnded?.invoke(result)
    }
}

    /** Grants a powerup directly (also used by the pickup collision path). */
    fun collectPowerUp(type: PowerUpType) {
        audio.powerupPickup()
        when (type) {
            PowerUpType.SHIELD -> {
                player.hasShield = true
                activePowerup = PowerUpType.SHIELD
            }
            PowerUpType.SPEED_BOOST -> {
                speedBoostTimer = config.speedBoostDuration
                activePowerup = PowerUpType.SPEED_BOOST
            }
            PowerUpType.BERSERKER -> {
                berserkTimer = config.berserkDuration
                activePowerup = PowerUpType.BERSERKER
                audio.berserkActivate()
            }
        }
    }

    /** Spawns an obstacle pair with the given gap center (public for tests). */
    fun spawnObstacle(gapCenter: Float) {
        val halfGap = config.gapSizeFor(score) / 2f
        val clamped = gapCenter.coerceIn(halfGap + 60f, config.groundY - halfGap - 60f)
        obstacles.add(Obstacle(config, clamped, config.gapSizeFor(score)))
    }

    private fun firePlayerBullet() {
        if (bulletCooldown > 0f) return
        val b = boss ?: return
        bulletCooldown = config.playerBulletCooldown
        val dx = b.x - player.x
        val dy = b.y - player.y
        val len = kotlin.math.hypot(dx, dy).coerceAtLeast(0.001f)
        val bullet = Bullet()
        bullet.launch(
            player.x + dx / len * (player.radius + 6f),
            player.y + dy / len * (player.radius + 6f),
            dx / len * config.playerBulletSpeed,
            dy / len * config.playerBulletSpeed,
            config.playerBulletRadius,
            fromPlayer = true
        )
        playerBullets.add(bullet)
        audio.playerShoot()
    }
