package com.renfliestudios.renflies.game.audio

/**
 * Audio abstraction. The pure game engine only knows this interface, so it
 * stays testable without an Android audio stack. The production app wires a
 * ToneGenerator-backed implementation; tests use a no-op or a recorder.
 */
interface AudioFeedback {
    fun flap()
    fun score()
    fun powerupPickup()
    fun shieldBreak()
    fun berserkActivate()
    fun bossWarning()
    fun bossShoot()
    fun playerShoot()
    fun bossHit()
    fun bossClear()
    fun gameOver()

    object NoOp : AudioFeedback {
        override fun flap() {}
        override fun score() {}
        override fun powerupPickup() {}
        override fun shieldBreak() {}
        override fun berserkActivate() {}
        override fun bossWarning() {}
        override fun bossShoot() {}
        override fun playerShoot() {}
        override fun bossHit() {}
        override fun bossClear() {}
        override fun gameOver() {}
    }
}
