package com.renfliestudios.renflies.game

/** The player-controlled bird. Pure logic, no Android dependencies. */
class Player(private val config: GameConfig) {
    var x: Float = config.playerX
        private set
    var y: Float = config.worldHeight / 2f
    var vy: Float = 0f
    var hasShield: Boolean = false
    var invulnTimer: Float = 0f

    val radius: Float get() = config.playerRadius

    /** Simple circle bounds used by the collision system. */
    val boundsX: Float get() = x
    val boundsY: Float get() = y

    fun reset() {
        y = config.worldHeight / 2f
        vy = 0f
        hasShield = false
        invulnTimer = 0f
    }

    fun flap() {
        vy = config.flapImpulse
    }

    fun update(dt: Float, gravityScale: Float = 1f) {
        vy = (vy + config.gravity * gravityScale * dt).coerceAtMost(config.maxFallSpeed)
        y += vy * dt

        // Ceiling: bounce down softly instead of dying (classic flappy behaviour).
        if (y - radius < 0f) {
            y = radius
            if (vy < 0f) vy = config.ceilingBounceSpeed
        }

        if (invulnTimer > 0f) {
            invulnTimer -= dt
            if (invulnTimer < 0f) invulnTimer = 0f
        }
    }

    val isOnGround: Boolean get() = y + radius >= config.groundY
}
