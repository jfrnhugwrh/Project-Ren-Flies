package com.renfliestudios.renflies.game

/** The player-controlled bird. Pure logic, no Android dependencies. */
class Player(private val config: GameConfig) {
    var x: Float = config.playerX
        private set
    var y: Float = config.worldHeight / 2f
    var vy: Float = 0f
    var invulnTimer: Float = 0f

    /**
     * Active shield stacks (heavy armor, max [GameConfig.shieldMaxStacks]).
     * Each stack adds an incremental gravity/weight penalty.
     */
    var shieldStacks: Int = 0
        private set

    /** Set for exactly the frame in which the bird bumps the ceiling. */
    var hitCeiling: Boolean = false
        private set

    val radius: Float get() = config.playerRadius

    /** A shield is only useful while at least one stack is held. */
    val hasShield: Boolean get() = shieldStacks > 0

    /**
     * Incremental mass penalty: Gravity_active = Gravity_base * this value.
     */
    val weightMultiplier: Float
        get() = 1f + shieldStacks * config.shieldWeightModifier

    /** Simple circle bounds used by the collision system. */
    val boundsX: Float get() = x
    val boundsY: Float get() = y

    fun reset() {
        y = config.worldHeight / 2f
        vy = 0f
        shieldStacks = 0
        invulnTimer = 0f
    }

    /** Adds one shield stack. Returns the number of stacks actually gained (0 when full). */
    fun addShieldStack(): Int {
        if (shieldStacks >= config.shieldMaxStacks) return 0
        shieldStacks++
        return 1
    }

    /** Consumes one shield stack (pipe collisions only). */
    fun consumeShieldStack() {
        if (shieldStacks > 0) shieldStacks--
    }

    fun flap() {
        vy = config.flapImpulse
    }

    fun update(dt: Float, gravityScale: Float = 1f) {
        hitCeiling = false
        vy = (vy + config.gravity * gravityScale * dt).coerceAtMost(config.maxFallSpeed)
        y += vy * dt

        // Ceiling: flag the bump; the engine decides the outcome (it is lethal
        // regardless of active shield stacks).
        if (y - radius < 0f) {
            y = radius
            hitCeiling = true
            if (vy < 0f) vy = config.ceilingBounceSpeed
        }

        if (invulnTimer > 0f) {
            invulnTimer -= dt
            if (invulnTimer < 0f) invulnTimer = 0f
        }
    }

    val isOnGround: Boolean get() = y + radius >= config.groundY
}
