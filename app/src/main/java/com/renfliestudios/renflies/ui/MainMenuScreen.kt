package com.renfliestudios.renflies.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renfliestudios.renflies.R
import com.renfliestudios.renflies.data.BattlePass
import com.renfliestudios.renflies.data.ProgressStore
import com.renfliestudios.renflies.ui.theme.AccentCyan
import com.renfliestudios.renflies.ui.theme.AccentGreen
import com.renfliestudios.renflies.ui.theme.AccentPurple
import com.renfliestudios.renflies.ui.theme.AccentYellow

@Composable
fun MainMenuScreen(
    progressStore: ProgressStore,
    onPlay: () -> Unit,
    onLeaderboard: () -> Unit,
    onBattlePass: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_bird),
            contentDescription = "Bird mascot",
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "REN FLIES",
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = AccentYellow
        )
        Text(
            text = "flap · dodge · survive the bullet hell",
            fontSize = 14.sp,
            color = AccentCyan
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Best Score: ${progressStore.bestScore}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        val bp = BattlePass.progressFor(progressStore.totalXp)
        Text(
            text = "Battle Pass · Level ${bp.level} · ${progressStore.totalXp} XP",
            fontSize = 14.sp,
            color = AccentPurple
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onPlay,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)
        ) {
            Text("PLAY", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.Black)
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onLeaderboard,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_trophy),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Leaderboard", fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = onBattlePass,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_pass),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Battle Pass", fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { progressStore.soundEnabled = !progressStore.soundEnabled; progressStore.save() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (progressStore.soundEnabled) "Sound: ON" else "Sound: OFF",
                    fontSize = 14.sp,
                    color = AccentGreen
                )
            }
            Text(
                text = "Runs: ${progressStore.gamesPlayed}  Bosses: ${progressStore.bossesDefeated}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
