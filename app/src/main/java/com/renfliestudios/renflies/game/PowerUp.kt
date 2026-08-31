package com.renfliestudios.renflies.game

/** The three powerups. Types are ordered from least to most powerful. */
enum class PowerUpType {
    SHIELD,
    SPEED_BOOST,
    BERSERKER;

    val displayName: String
        get() = when (this) {
            SHIELD -> "SHIELD"
            SPEED_BOOST -> "SPEED BOOST"
            BERSERKER -> "BERSERKER"
        }
}

/** A collectible powerup drifting across the screen. */
class PowerUp(val type: PowerUpType) {
    var x: Float = 0f
    var y: Float = 0f
    var collected: Boolean = false
    var bobPhase: Float = 0f

    val radius: Float get() = 30f

    fun spawn(x: Float, y: Float) {
        this.x = x
        this.y = y
        this.collected = false
        this.bobPhase = 0f
    }

    fun update(dt: Float, speed: Float) {
        x -= speed * dt
        bobPhase += dt * 3f
    }

    val displayY: Float get() = y + kotlin.math.sin(bobPhase) * 8f

    val isOffScreen: Boolean get() = x + radius < 0f
}
