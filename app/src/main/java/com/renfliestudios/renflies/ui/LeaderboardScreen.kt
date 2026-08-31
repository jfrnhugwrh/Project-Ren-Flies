package com.renfliestudios.renflies.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renfliestudios.renflies.data.Leaderboard
import com.renfliestudios.renflies.ui.theme.AccentCyan
import com.renfliestudios.renflies.ui.theme.AccentYellow
import com.renfliestudios.renflies.ui.theme.PanelBlue

/**
 * Mock leaderboard screen. Entirely local: fictional entries plus the
 * player's best score. Clearly labelled as mock - no networking anywhere.
 */
@Composable
fun LeaderboardScreen(bestScore: Int, onBack: () -> Unit) {
    val entries = Leaderboard.entries(playerBestScore = bestScore)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "LEADERBOARD",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = AccentYellow,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = "(mock · stored locally on this device)",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(entries) { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (entry.isPlayer) AccentCyan.copy(alpha = 0.25f) else PanelBlue)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (index < 3) AccentYellow else Color.Gray,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        text = entry.name + if (entry.isPlayer) " (you)" else "",
                        fontSize = 16.sp,
                        fontWeight = if (entry.isPlayer) FontWeight.Black else FontWeight.Normal,
                        color = if (entry.isPlayer) AccentCyan else Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = entry.score.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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

private val MaterialBackground = Color(0xFF0D1B2A)
