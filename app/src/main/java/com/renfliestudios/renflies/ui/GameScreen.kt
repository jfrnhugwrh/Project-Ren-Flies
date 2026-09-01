package com.renfliestudios.renflies.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renfliestudios.renflies.data.ProgressUpdater
import com.renfliestudios.renflies.data.ProgressStore
import com.renfliestudios.renflies.game.Boss
import com.renfliestudios.renflies.game.DifficultyManager
import com.renfliestudios.renflies.game.GameEngine
import com.renfliestudios.renflies.game.GamePhase
import com.renfliestudios.renflies.game.Obstacle
import com.renfliestudios.renflies.game.PowerUpType
import com.renfliestudios.renflies.game.audio.AudioFeedback
import com.renfliestudios.renflies.ui.theme.AccentCyan
import com.renfliestudios.renflies.ui.theme.AccentGreen
import com.renfliestudios.renflies.ui.theme.AccentPurple
import com.renfliestudios.renflies.ui.theme.AccentRed
import com.renfliestudios.renflies.ui.theme.AccentYellow
import kotlinx.coroutines.isActive
import kotlin.math.min

/**
 * The gameplay screen: hosts the frame loop (delta-time driven), the Canvas
 * renderer for the placeholder vector artwork, and the HUD overlays
 * (score, powerups, boss health/shield, boss intro/clear, game over).
 */
@Composable
fun GameScreen(
    progressStore: ProgressStore,
    audio: AudioFeedback,
    loadout: Map<PowerUpType, Int> = emptyMap(),
    onExitToMenu: () -> Unit
) {
    val engine = remember(loadout) {
        GameEngine(
            initialBestScore = progressStore.bestScore,
            audio = audio,
            onRunEnded = { result -> ProgressUpdater.applyRunResult(progressStore, result) }
        ).also { it.startRun(loadout) }
    }

    // Recomposition trigger: bumped once per rendered frame.
    var tick by remember { mutableLongStateOf(0L) }

    // Delta-time game loop tied to the frame clock.
    LaunchedEffect(Unit) {
        var lastFrameNanos = androidx.compose.runtime.withFrameNanos { it }
        while (isActive) {
            androidx.compose.runtime.withFrameNanos { now ->
                val dt = (now - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = now
                engine.update(dt)
            }
            tick++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { engine.flap() }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawWorld(engine, tick)
        }
        Hud(engine, tick, onExitToMenu)
    }
}

// ---------------------------------------------------------------------------
// Renderer - placeholder vector artwork drawn procedurally on the Canvas.
// The engine simulates in a 720x1280 virtual world; we scale it to the screen.
// ---------------------------------------------------------------------------

private const val WORLD_W = 720f
private const val WORLD_H = 1280f

/**
 * How far (in world units) boundary geometry is extended past the world edges.
 * This guarantees complete camera coverage on every target aspect ratio so
 * letterboxed areas never expose empty space behind the ground or pipes.
 */
private const val BOUND_EXT = 2000f

private fun DrawScope.drawWorld(engine: GameEngine, tick: Long) {
    val cfg = engine.config
    val scale = min(size.width / WORLD_W, size.height / WORLD_H)
    val offsetX = (size.width - WORLD_W * scale) / 2f
    val offsetY = (size.height - WORLD_H * scale) / 2f

    // Sky gradient fills the whole physical screen.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B3A5C), Color(0xFF2C5F8A))
        ),
        size = size
    )

    withTransform({
        scale(scale, scale, pivot = Offset.Zero)
        translate(offsetX / scale, offsetY / scale)
    }) {
        // Distant background band, extended well past the viewport edges.
        drawRect(
            Color(0xFF12283F),
            Offset(-BOUND_EXT, cfg.groundY - 170f),
            Size(WORLD_W + 2f * BOUND_EXT, 170f + BOUND_EXT)
        )
        drawGround(cfg.groundY, tick)
        for (o in engine.obstacles) drawObstacle(o)
        for (p in engine.powerups) drawPowerUp(p, tick)
        engine.boss?.let { drawBoss(it, tick) }
        for (b in engine.bossBullets) {
            drawCircle(Color(0xFFFF5252), b.radius, Offset(b.x, b.y))
            drawCircle(Color(0xFFFFCDD2), b.radius * 0.45f, Offset(b.x, b.y))
        }
        for (b in engine.playerBullets) {
            drawCircle(AccentYellow, b.radius, Offset(b.x, b.y))
        }
        drawBird(engine, tick)
        if (engine.isBerserkActive) drawBerserkField(engine, tick)
    }

    // Speed-up safe expiry: rapid full-screen white flash (200ms visual cue).
    if (engine.isSpeedFlashing) {
        val alpha = engine.speedFlashTimer / engine.config.speedFlashDuration
        drawRect(Color.White.copy(alpha = alpha), size = size)
    }
}

private fun DrawScope.drawGround(groundY: Float, tick: Long) {
    // Extended beyond both horizontal edges (and below the bottom edge) so the
    // ground never cuts off on wide/ultrawide aspect ratios.
    drawRect(
        Color(0xFF3E7C4F),
        Offset(-BOUND_EXT, groundY),
        Size(WORLD_W + 2f * BOUND_EXT, WORLD_H - groundY + BOUND_EXT)
    )
    drawRect(
        Color(0xFF2E5C3B),
        Offset(-BOUND_EXT, groundY),
        Size(WORLD_W + 2f * BOUND_EXT, 14f)
    )
    // Scrolling stripes for motion feedback.
    val stripe = 90f
    val shift = (tick.toFloat() * 6f) % stripe
    var x = -BOUND_EXT - shift
    while (x < WORLD_W + BOUND_EXT) {
        drawRect(Color(0xFF356B45), Offset(x, groundY + 20f), Size(stripe / 2f, 10f))
        x += stripe
    }
}

private fun DrawScope.drawObstacle(o: Obstacle) {
    val color = if (o.pulled) AccentRed else AccentGreen
    // Top pipe: visually extended far past the top edge so it never cuts off
    // on tall aspect ratios (collision rects are unchanged).
    drawRoundRect(
        color = color,
        topLeft = Offset(o.topRect[0], o.topRect[1] - BOUND_EXT),
        size = Size(o.width, o.topRect[3] - o.topRect[1] + BOUND_EXT),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Bottom pipe.
    drawRoundRect(
        color = color,
        topLeft = Offset(o.bottomRect[0], o.bottomRect[1]),
        size = Size(o.width, o.bottomRect[3] - o.bottomRect[1]),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Pipe caps at the gap edges.
    drawRoundRect(
        color = Color(0xFF2E5C3B),
        topLeft = Offset(o.topRect[0] - 8f, o.topRect[3] - 34f),
        size = Size(o.width + 16f, 34f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = Color(0xFF2E5C3B),
        topLeft = Offset(o.bottomRect[0] - 8f, o.bottomRect[1]),
        size = Size(o.width + 16f, 34f),
        cornerRadius = CornerRadius(8f, 8f)
    )
}

private fun DrawScope.drawPowerUp(p: com.renfliestudios.renflies.game.PowerUp, tick: Long) {
    val center = Offset(p.x, p.displayY)
    val pulse = 1f + 0.08f * kotlin.math.sin(tick.toFloat() * 0.2f)
    when (p.type) {
        PowerUpType.SHIELD -> {
            drawCircle(AccentCyan, p.radius * pulse, center)
            drawCircle(Color.White, p.radius * 0.55f, center, style = Stroke(6f))
            drawCircle(AccentCyan, p.radius * 0.22f, center)
        }
        PowerUpType.SPEED_BOOST -> {
            drawCircle(Color(0xFFFFEE58), p.radius * pulse, center)
            // Lightning-bolt style wedge.
            drawPath(
                androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x - 8f, center.y - 16f)
                    lineTo(center.x + 10f, center.y - 16f)
                    lineTo(center.x - 2f, center.y - 2f)
                    lineTo(center.x + 12f, center.y - 2f)
                    lineTo(center.x - 10f, center.y + 18f)
                    lineTo(center.x - 2f, center.y + 2f)
                    lineTo(center.x - 14f, center.y + 2f)
                    close()
                },
                Color(0xFFFF8F00)
            )
        }
        PowerUpType.BERSERKER -> {
            drawCircle(AccentRed, p.radius * pulse, center)
            drawCircle(Color(0xFFFF8A80), p.radius * 0.6f * pulse, center)
            drawCircle(Color(0xFF7F0000), p.radius * 0.25f, center)
        }
    }
}

private fun DrawScope.drawBoss(boss: Boss, tick: Long) {
    val center = Offset(boss.x, boss.y)
    when (boss.visualState) {
        Boss.BossVisualState.DEFEATED -> return
        Boss.BossVisualState.RETREATING -> {
            drawCircle(AccentPurple, boss.radius, center)
        }
        else -> {
            // Body.
            drawCircle(AccentPurple, boss.radius, center)
            drawCircle(Color(0xFF4A148C), boss.radius * 0.55f, center)
            // Angry eyes.
            val eyeY = center.y - boss.radius * 0.2f
            drawCircle(Color(0xFFE1BEE7), 10f, Offset(center.x - 22f, eyeY))
            drawCircle(Color(0xFFE1BEE7), 10f, Offset(center.x + 22f, eyeY))
            drawCircle(Color.Black, 5f, Offset(center.x - 22f, eyeY))
            drawCircle(Color.Black, 5f, Offset(center.x + 22f, eyeY))
            // Mouth.
            drawRect(
                Color(0xFF4A148C),
                Offset(center.x - 24f, center.y + boss.radius * 0.35f),
                Size(48f, 10f)
            )
            // Shield: pulsing cyan ring while active.
            if (boss.shieldActive) {
                val alpha = 0.4f + 0.25f * kotlin.math.sin(tick.toFloat() * 0.25f)
                drawCircle(
                    AccentCyan.copy(alpha = alpha),
                    boss.radius + 14f,
                    center,
                    style = Stroke(8f)
                )
                drawCircle(
                    AccentCyan.copy(alpha = alpha * 0.35f),
                    boss.radius + 24f,
                    center,
                    style = Stroke(4f)
                )
            }
        }
    }
}

private fun DrawScope.drawBird(engine: GameEngine, tick: Long) {
    val p = engine.player
    val center = Offset(p.x, p.y)
    val r = p.radius

    // Speed boost trail.
    if (engine.isSpeedBoostActive) {
        for (i in 1..4) {
            drawCircle(
                AccentCyan.copy(alpha = 0.25f / i),
                r * (1f - i * 0.18f),
                Offset(center.x + i * 26f, center.y)
            )
        }
    }

    // Body.
    drawCircle(Color(0xFFFFC93C), r, center)
    // Wing (flaps visually with the tick).
    val wingY = center.y + r * 0.25f + kotlin.math.sin(tick.toFloat() * 0.35f) * 3f
    drawCircle(Color(0xFFE76F00), r * 0.45f, Offset(center.x - r * 0.3f, wingY))
    // Eye.
    drawCircle(Color.White, r * 0.32f, Offset(center.x + r * 0.4f, center.y - r * 0.25f))
    drawCircle(Color.Black, r * 0.15f, Offset(center.x + r * 0.5f, center.y - r * 0.25f))
    // Beak.
    drawPath(
        androidx.compose.ui.graphics.Path().apply {
            moveTo(center.x + r * 0.8f, center.y)
            lineTo(center.x + r * 1.45f, center.y + r * 0.1f)
            lineTo(center.x + r * 0.8f, center.y + r * 0.35f)
            close()
        },
        Color(0xFFFF7043)
    )

    // Stacking shield aura: one ring per active stack.
    if (p.hasShield) {
        val alpha = 0.5f + 0.3f * kotlin.math.sin(tick.toFloat() * 0.3f)
        for (i in 0 until p.shieldStacks) {
            drawCircle(AccentCyan.copy(alpha = alpha), r + 10f + i * 5f, center, style = Stroke(4f))
        }
    }
    // Post-hit invulnerability flicker.
    if (p.invulnTimer > 0f) {
        drawCircle(Color.White.copy(alpha = 0.3f), r + 6f, center, style = Stroke(3f))
    }
}

private fun DrawScope.drawBerserkField(engine: GameEngine, tick: Long) {
    val p = engine.player
    val center = Offset(p.x, p.y)
    val radius = engine.config.berserkFieldRadius
    val angle = (tick.toFloat() * 2f) % 360f
    rotate(degrees = angle, pivot = center) {
        drawCircle(AccentRed.copy(alpha = 0.10f), radius, center)
        for (i in 0 until 6) {
            rotate(degrees = i * 60f, pivot = center) {
                drawArc(
                    color = AccentRed.copy(alpha = 0.5f),
                    startAngle = 0f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(6f)
                )
            }
        }
    }
    drawCircle(AccentRed.copy(alpha = 0.25f), radius, center, style = Stroke(4f))
}

// ---------------------------------------------------------------------------
// HUD overlays
// ---------------------------------------------------------------------------

@Composable
private fun Hud(engine: GameEngine, tick: Long, onExitToMenu: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left: score & best.
        Column(modifier = Modifier.padding(top = 24.dp, start = 20.dp)) {
            Text(
                text = engine.score.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Best: ${engine.bestScore}",
                fontSize = 16.sp,
                color = Color.LightGray
            )
            Text(
                text = "Difficulty: ${DifficultyManager.current.displayName}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AccentPurple
            )
        }

        // Top-right: next boss milestone.
        Text(
            text = "Boss at: ${engine.nextBossMilestone}",
            fontSize = 14.sp,
            color = AccentPurple,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 28.dp, end = 20.dp)
        )

        // Active powerup indicator.
        engine.activePowerup?.let { type ->
            val durationText = when (type) {
                PowerUpType.SHIELD ->
                    "SHIELD ×${engine.player.shieldStacks}"
                PowerUpType.SPEED_BOOST ->
                    "SPEED BOOST  ${"%.1f".format(engine.speedBoostTimer)}s"
                PowerUpType.BERSERKER ->
                    "BERSERKER  ${"%.1f".format(engine.berserkTimer)}s"
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = durationText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (type) {
                        PowerUpType.SHIELD -> AccentCyan
                        PowerUpType.SPEED_BOOST -> AccentYellow
                        PowerUpType.BERSERKER -> AccentRed
                    }
                )
            }
        }

        // Boss health bar & shield status.
        engine.boss?.let { boss ->
            if (boss.visualState == Boss.BossVisualState.FIGHTING ||
                boss.visualState == Boss.BossVisualState.ENTERING
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 130.dp)
                        .fillMaxWidth(0.7f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = boss.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentPurple
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(boss.hp.toFloat() / boss.maxHp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(AccentRed)
                        )
                    }
                    Text(
                        text = if (boss.shieldActive)
                            "SHIELD ACTIVE ${"%.1f".format(boss.shieldTimer)}s"
                        else "SHIELD DOWN - ATTACK!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (boss.shieldActive) AccentCyan else AccentGreen
                    )
                }
            }
        }

        BossMessages(engine, tick)
        GameOverOverlay(engine, onExitToMenu)

        // Stored single-use loadout charges: tap to trigger mid-run.
        val storedTypes = PowerUpType.values().filter { engine.storedPowerupCount(it) > 0 }
        if (storedTypes.isNotEmpty() && engine.phase != GamePhase.GAME_OVER) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                storedTypes.forEach { type ->
                    Button(
                        onClick = { engine.useStoredPowerUp(type) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (type) {
                                PowerUpType.SHIELD -> AccentCyan
                                PowerUpType.SPEED_BOOST -> AccentYellow
                                PowerUpType.BERSERKER -> AccentRed
                            }
                        )
                    ) {
                        Text(
                            "${type.displayName} ×${engine.storedPowerupCount(type)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.BossMessages(engine: GameEngine, tick: Long) {
    // Boss intro warning.
    if (engine.phase == GamePhase.BOSS_INTRO) {
        val alpha = 0.6f + 0.4f * kotlin.math.sin(tick.toFloat() * 0.3f)
        Text(
            text = "⚠ BOSS INCOMING! ⚠",
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = AccentRed.copy(alpha = alpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }

    // Boss clear message.
    if (engine.phase == GamePhase.BOSS_CLEAR) {
        Text(
            text = engine.bossClearMessage,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = if (engine.bossClearMessage.startsWith("BOSS CLEARED"))
                AccentGreen else AccentYellow,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun GameOverOverlay(engine: GameEngine, onExitToMenu: () -> Unit) {
    if (engine.phase != GamePhase.GAME_OVER) return
    val result = engine.lastRunResult
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME OVER",
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = AccentRed
        )
        Spacer(Modifier.height(16.dp))
        Text("Score: ${result?.score ?: 0}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Best: ${result?.bestScore ?: engine.bestScore}" +
                if (result?.newBest == true) "  ★ NEW!" else "",
            fontSize = 18.sp,
            color = AccentYellow
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "XP Earned: ${result?.xpEarned ?: 0}",
            fontSize = 18.sp,
            color = AccentGreen
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { engine.startRun() },
            modifier = Modifier
                .width(220.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)
        ) {
            Text("RETRY", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Black)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onExitToMenu,
            modifier = Modifier
                .width(220.dp)
                .height(56.dp)
        ) {
            Text("MAIN MENU", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
