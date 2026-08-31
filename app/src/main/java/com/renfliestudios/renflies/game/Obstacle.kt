package com.renfliestudios.renflies.game

/**
 * A pair of vertical pipes with a gap between them. The obstacle moves from
 * the right edge of the world to the left.
 */
class Obstacle(
    private val config: GameConfig,
    var gapCenter: Float,
    gapSize: Float
) {
    var x: Float = config.worldWidth + config.obstacleWidth
    var gap: Float = gapSize
    var scored: Boolean = false

    /** Set when the Berserker field grabs this obstacle and drags it in. */
    var pulled: Boolean = false

    val width: Float get() = config.obstacleWidth

    // Top pipe rectangle (left, top, right, bottom).
    val topRect: FloatArray = FloatArray(4)
    // Bottom pipe rectangle (left, top, right, bottom).
    val bottomRect: FloatArray = FloatArray(4)

    init {
        refreshRects()
    }

    fun refreshRects() {
        val halfGap = gap / 2f
        val topBottom = gapCenter - halfGap
        val bottomTop = gapCenter + halfGap
        topRect[0] = x
        topRect[1] = 0f
        topRect[2] = x + width
        topRect[3] = topBottom
        bottomRect[0] = x
        bottomRect[1] = bottomTop
        bottomRect[2] = x + width
        bottomRect[3] = config.groundY
    }

    fun update(dt: Float, speed: Float) {
        x -= speed * dt
        refreshRects()
    }

    /** Pull the obstacle toward the given point (Berserker field). */
    fun pullToward(targetX: Float, targetY: Float, dt: Float, pullSpeed: Float) {
        val dx = targetX - (x + width / 2f)
        val dy = targetY - gapCenter
        val len = kotlin.math.hypot(dx, dy).coerceAtLeast(0.001f)
        val step = pullSpeed * dt
        x += (dx / len) * step
        gapCenter += (dy / len) * step
        refreshRects()
    }

    /** True when the obstacle has fully scrolled off the left edge. */
    val isOffScreen: Boolean get() = x + width < 0f
}
