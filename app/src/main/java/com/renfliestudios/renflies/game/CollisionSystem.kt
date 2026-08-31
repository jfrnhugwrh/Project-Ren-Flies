package com.renfliestudios.renflies.game

/**
 * Shared, allocation-free collision helpers. The game uses circles for the
 * bird, bullets, powerups and the boss, and axis-aligned rectangles for pipes.
 */
object CollisionSystem {

    fun circlesOverlap(
        ax: Float, ay: Float, aradius: Float,
        bx: Float, by: Float, bradius: Float
    ): Boolean {
        val dx = ax - bx
        val dy = ay - by
        val r = aradius + bradius
        return dx * dx + dy * dy <= r * r
    }

    fun circleIntersectsRect(
        cx: Float, cy: Float, cradius: Float,
        left: Float, top: Float, right: Float, bottom: Float
    ): Boolean {
        val nearestX = cx.coerceIn(left, right)
        val nearestY = cy.coerceIn(top, bottom)
        val dx = cx - nearestX
        val dy = cy - nearestY
        return dx * dx + dy * dy <= cradius * cradius
    }

    /** Shortest distance from a circle center to the nearest point of a rect. */
    fun distanceToRect(
        cx: Float, cy: Float,
        left: Float, top: Float, right: Float, bottom: Float
    ): Float {
        val nearestX = cx.coerceIn(left, right)
        val nearestY = cy.coerceIn(top, bottom)
        val dx = cx - nearestX
        val dy = cy - nearestY
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
