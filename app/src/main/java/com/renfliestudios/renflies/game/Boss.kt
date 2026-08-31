package com.renfliestudios.renflies.game

/**
 * The bullet-hell boss. The boss has three states inside the BOSS phase:
 *
 *  ENTERING  - flying in from the right edge.
 *  FIGHTING  - bobbing, firing bullet patterns. Shield is active for the first
 *              [GameConfig.bossShieldDuration] seconds of fighting and then
 *              shatters, after which the boss can take damage.
 *  RETREATING/DEFEATED - handled by the engine once the encounter timer runs
 *              out or HP reaches zero.
 *
 * Bullet patterns cycle deterministically:
 *   RADIAL -> SPREAD -> SPIRAL -> RADIAL -> ...
 */
class Boss(private val config: GameConfig, val milestone: Int) {

    enum class BossVisualState { ENTERING, FIGHTING, RETREATING, DEFEATED }
    enum class Pattern { RADIAL, SPREAD, SPIRAL }

    var x: Float = config.worldWidth + config.bossRadius * 2f
        private set
    var y: Float = config.worldHeight / 2f
        private set
    var hp: Int = maxHp
        private set
    var visualState: BossVisualState = BossVisualState.ENTERING
    var shieldActive: Boolean = true
        private set
    var shieldTimer: Float = config.bossShieldDuration
        private set

    var pattern: Pattern = Pattern.RADIAL
        private set
    var patternTimer: Float = config.bossPatternDuration
        private set
    var fireTimer: Float = 0f
        private set
    var spiralAngle: Float = 0f
        private set
    var bobPhase: Float = 0f
        private set
    var encounterTimer: Float = config.bossEncounterLimit
        private set

    val radius: Float get() = config.bossRadius
    val maxHp: Int get() = config.bossBaseHp + (milestone / 100) * config.bossHpPerMilestone
    val name: String get() = "SKY WARDEN MK${milestone / 100}"

    /** Called when the BOSS phase starts (after the intro warning). */
    fun onFightStart() {
        visualState = BossVisualState.FIGHTING
        shieldActive = true
        shieldTimer = config.bossShieldDuration
        pattern = Pattern.RADIAL
        patternTimer = config.bossPatternDuration
        fireTimer = 0.6f
        spiralAngle = 0f
    }

    fun update(dt: Float) {
        when (visualState) {
            BossVisualState.ENTERING -> {
                x -= config.bossEnterSpeed * dt
                if (x <= config.bossHomeX) {
                    x = config.bossHomeX
                    onFightStart()
                }
            }
            BossVisualState.FIGHTING -> {
                bobPhase += dt * config.bossBobSpeed
                y = config.worldHeight / 2f + kotlin.math.sin(bobPhase) * config.bossBobAmplitude

                encounterTimer -= dt

                if (shieldActive) {
                    shieldTimer -= dt
                    if (shieldTimer <= 0f) {
                        shieldActive = false
                        shieldTimer = 0f
                    }
                }

                patternTimer -= dt
                if (patternTimer <= 0f) {
                    pattern = when (pattern) {
                        Pattern.RADIAL -> Pattern.SPREAD
                        Pattern.SPREAD -> Pattern.SPIRAL
                        Pattern.SPIRAL -> Pattern.RADIAL
                    }
                    patternTimer = config.bossPatternDuration
                    fireTimer = 0.2f
                }

                fireTimer -= dt
                spiralAngle += dt * 2.6f
            }
            BossVisualState.RETREATING -> {
                x += config.bossEnterSpeed * 1.5f * dt
            }
            BossVisualState.DEFEATED -> {
                // Handled by the engine (explosion timer etc.).
            }
        }
    }


    /**
     * Engine asks the boss to perform its fire action. Returns the velocity
     * intents for each bullet. For the SPREAD pattern the shots are aimed at
     * the player's current position (fan-shaped), the others are absolute.
     */
    fun performFire(playerX: Float, playerY: Float): List<BulletIntent> {
        val speed = 240f + (milestone / 100) * 12f
        val intents = ArrayList<BulletIntent>(16)
        when (pattern) {
            // Pattern A - Radial Burst: bullets in all directions.
            Pattern.RADIAL -> {
                val count = 12
                for (i in 0 until count) {
                    val angle = (Math.PI * 2 * i / count).toFloat()
                    intents.add(
                        BulletIntent(
                            x, y,
                            kotlin.math.cos(angle) * speed,
                            kotlin.math.sin(angle) * speed
                        )
                    )
                }
                fireTimer = 2.2f
            }
            // Pattern B - Spread Shot: fan aimed at the player's current position.
            Pattern.SPREAD -> {
                val base = kotlin.math.atan2(playerY - y, playerX - x)
                val spread = Math.PI.toFloat() / 6f // 30 degrees total
                val count = 5
                for (i in 0 until count) {
                    val t = if (count == 1) 0f else (i.toFloat() / (count - 1)) - 0.5f
                    val angle = base + t * spread
                    intents.add(
                        BulletIntent(
                            x, y,
                            kotlin.math.cos(angle) * speed,
                            kotlin.math.sin(angle) * speed
                        )
                    )
                }
                fireTimer = 1.6f
            }
            // Pattern C - Rotating Spiral: one bullet per tick at a rotating angle,
            // fired as two mirrored arms for better coverage.
            Pattern.SPIRAL -> {
                for (mirror in listOf(0f, Math.PI.toFloat())) {
                    val angle = spiralAngle + mirror
                    intents.add(
                        BulletIntent(
                            x, y,
                            kotlin.math.cos(angle) * speed * 0.85f,
                            kotlin.math.sin(angle) * speed * 0.85f
                        )
                    )
                }
                fireTimer = 0.09f
            }
        }
        return intents
    }

    /** Applies damage. Returns false when the shield blocked it. */
    fun hit(damage: Int): Boolean {
        if (shieldActive || visualState != BossVisualState.FIGHTING) return false
        hp = (hp - damage).coerceAtLeast(0)
        if (hp == 0) visualState = BossVisualState.DEFEATED
        return true
    }

    fun beginRetreat() {
        visualState = BossVisualState.RETREATING
    }

    val isOffScreen: Boolean get() = x - radius > config.worldWidth + 100f

    /** Data holder describing one bullet the boss wants to spawn. */
    class BulletIntent(val x: Float, val y: Float, val vx: Float, val vy: Float)
}
