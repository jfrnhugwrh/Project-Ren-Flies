package com.renfliestudios.renflies.ui

import android.media.AudioManager
import android.media.ToneGenerator
import com.renfliestudios.renflies.game.audio.AudioFeedback

/**
 * Placeholder audio implementation using the built-in ToneGenerator, so the
 * game has audible feedback without shipping any copyrighted sound files.
 * Sounds are generated on the fly; failures are silently ignored because
 * audio must never crash the game loop.
 */
class ToneAudioFeedback(
    private val isEnabled: () -> Boolean
) : AudioFeedback {

    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    } catch (_: Exception) {
        null
    }

    private fun play(toneType: Int, durationMs: Int) {
        if (!isEnabled()) return
        try {
            tone?.startTone(toneType, durationMs)
        } catch (_: Exception) {
            // Ignore audio failures.
        }
    }

    override fun flap() = play(ToneGenerator.TONE_PROP_BEEP, 60)
    override fun score() = play(ToneGenerator.TONE_PROP_ACK, 80)
    override fun powerupPickup() = play(ToneGenerator.TONE_PROP_BEEP2, 150)
    override fun shieldBreak() = play(ToneGenerator.TONE_SUP_ERROR, 200)
    override fun berserkActivate() = play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
    override fun bossWarning() = play(ToneGenerator.TONE_CDMA_ABBR_ALERT, 400)
    override fun bossShoot() = play(ToneGenerator.TONE_PROP_NACK, 50)
    override fun playerShoot() = play(ToneGenerator.TONE_PROP_BEEP, 40)
    override fun bossHit() = play(ToneGenerator.TONE_PROP_NACK, 60)
    override fun bossClear() = play(ToneGenerator.TONE_CDMA_CONFIRM, 400)
    override fun gameOver() = play(ToneGenerator.TONE_SUP_BUSY, 500)
}
