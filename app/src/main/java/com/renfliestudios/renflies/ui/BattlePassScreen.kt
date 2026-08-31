package com.renfliestudios.renflies.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renfliestudios.renflies.R
import com.renfliestudios.renflies.data.BattlePass
import com.renfliestudios.renflies.data.ProgressStore
import com.renfliestudios.renflies.ui.theme.AccentGreen
import com.renfliestudios.renflies.ui.theme.AccentPurple
import com.renfliestudios.renflies.ui.theme.AccentYellow
import com.renfliestudios.renflies.ui.theme.PanelBlue

/**
 * Mock battle pass screen. Progress is derived from locally persisted XP -
 * no purchases, no backend.
 */
@Composable
fun BattlePassScreen(progressStore: ProgressStore, onBack: () -> Unit) {
    val progress = BattlePass.progressFor(progressStore.totalXp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "BATTLE PASS",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = AccentPurple,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = "(mock · earned by playing, saved locally)",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))

        // Current level + XP progress bar.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelBlue)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Level ${progress.level}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentYellow
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${progressStore.totalXp} XP total",
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = AccentYellow,
                trackColor = Color.DarkGray
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (progress.isMaxLevel) "MAX LEVEL REACHED"
                else "${progress.xpIntoLevel} / ${progress.xpForNextLevel} XP to level ${progress.level + 1}",
                fontSize = 12.sp,
                color = Color.LightGray
            )
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(BattlePass.LEVELS) { level ->
                val unlocked = progress.level >= level.level
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (unlocked) AccentGreen.copy(alpha = 0.15f) else PanelBlue)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (unlocked) "✓" else "🔒",
                        fontSize = 18.sp,
                        color = if (unlocked) AccentGreen else Color.Gray,
                        modifier = Modifier.width(34.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Level ${level.level}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (unlocked) AccentGreen else Color.White
                        )
                        Text(
                            text = "${level.xpRequired} XP · Reward: ${level.reward.name}",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                    }
                    RewardIconBox(level.reward.iconType, unlocked)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(onClick = onBack, modifier = Modifier.height(52.dp)) {
                Text("BACK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RewardIconBox(iconType: BattlePass.RewardIcon, unlocked: Boolean) {
    val resId = when (iconType) {
        BattlePass.RewardIcon.BIRD -> R.drawable.ic_bird
        BattlePass.RewardIcon.COINS -> R.drawable.ic_trophy
        BattlePass.RewardIcon.TRAIL -> R.drawable.ic_speed
        BattlePass.RewardIcon.EFFECT -> R.drawable.ic_berserker
        BattlePass.RewardIcon.WINGS -> R.drawable.ic_bird
        BattlePass.RewardIcon.BADGE -> R.drawable.ic_pass
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        alpha = if (unlocked) 1f else 0.4f
    )
}

private val MaterialBackground = Color(0xFF0D1B2A)

