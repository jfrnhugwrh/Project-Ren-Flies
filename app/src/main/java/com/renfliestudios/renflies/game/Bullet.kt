package com.renfliestudios.renflies.game

import kotlin.math.hypot

/** A projectile. Used for both boss bullets and the player's shots at the boss. */
class Bullet {
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var radius: Float = 11f
    var fromPlayer: Boolean = false
    var active: Boolean = true

    fun launch(x: Float, y: Float, vx: Float, vy: Float, radius: Float, fromPlayer: Boolean) {
        this.x = x
        this.y = y
        this.vx = vx
        this.vy = vy
        this.radius = radius
        this.fromPlayer = fromPlayer
        this.active = true
    }

    fun update(dt: Float) {
        x += vx * dt
        y += vy * dt
    }

    val isOffScreen: Boolean
        get() = x < -radius * 4 || x > 2000f || y < -radius * 4 || y > 2000f
}
