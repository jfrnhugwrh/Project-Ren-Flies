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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renfliestudios.renflies.R
import com.renfliestudios.renflies.data.ProgressStore
import com.renfliestudios.renflies.game.Difficulty
import com.renfliestudios.renflies.game.DifficultyManager
import com.renfliestudios.renflies.game.PowerUpType
import com.renfliestudios.renflies.ui.theme.AccentCyan
import com.renfliestudios.renflies.ui.theme.AccentGreen
import com.renfliestudios.renflies.ui.theme.AccentPurple
import com.renfliestudios.renflies.ui.theme.AccentRed
import com.renfliestudios.renflies.ui.theme.AccentYellow
import com.renfliestudios.renflies.ui.theme.PanelBlue

/**
 * Pre-game loadout & difficulty selection. Players may equip ONE instance of
 * each unlocked (owned) main power-up; equipped items are single-use
 * consumables consumed at run start and reset post-game.
 */
@Composable
fun LoadoutScreen(
    progressStore: ProgressStore,
    onStartRun: (Map<PowerUpType, Int>) -> Unit,
    onBack: () -> Unit
) {
    var selectedDifficulty by remember { mutableStateOf(DifficultyManager.current) }
    var equipped by remember { mutableStateOf(setOf<PowerUpType>()) }

    fun ownedCount(type: PowerUpType): Int = when (type) {
        PowerUpType.SHIELD -> progressStore.ownedShields
        PowerUpType.SPEED_BOOST -> progressStore.ownedSpeedBoosts
        PowerUpType.BERSERKER -> progressStore.ownedBerserkers
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "LOADOUT",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = AccentPurple
        )
        Text(
            text = "equip one of each owned power-up · single use per run",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(Modifier.height(16.dp))

        // ---- Difficulty selector -------------------------------------------
        Text("DIFFICULTY", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Difficulty.values().forEach { d ->
                val selected = selectedDifficulty == d
                OutlinedButton(
                    onClick = { selectedDifficulty = d },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) AccentYellow else PanelBlue,
                        contentColor = if (selected) Color.Black else Color.White
                    )
                ) {
                    Text(d.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Power-up equipment --------------------------------------------
        Text(
            "SINGLE-USE CONSUMABLES",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AccentYellow
        )
        Spacer(Modifier.height(8.dp))
        PowerUpType.values().forEach { type ->
            PowerUpLoadoutRow(
                type = type,
                owned = ownedCount(type),
                isEquipped = type in equipped,
                onToggle = {
                    equipped = if (type in equipped) equipped - type else equipped + type
                }
            )
            Spacer(Modifier.height(8.dp))
        }
        StartRunButton(
            onStart = {
                // Equip = consume: deduct from the inventory and hand the
                // charges to the engine for this run (reset post-game).
                DifficultyManager.current = selectedDifficulty
                for (type in equipped) {
                    when (type) {
                        PowerUpType.SHIELD -> progressStore.ownedShields -= 1
                        PowerUpType.SPEED_BOOST -> progressStore.ownedSpeedBoosts -= 1
                        PowerUpType.BERSERKER -> progressStore.ownedBerserkers -= 1
                    }
                }
                progressStore.save()
                onStartRun(equipped.associateWith { 1 })
            },
            onBack = onBack
        )
    }
}

@Composable
private fun PowerUpLoadoutRow(
    type: PowerUpType,
    owned: Int,
    isEquipped: Boolean,
    onToggle: () -> Unit
) {
    val accent = when (type) {
        PowerUpType.SHIELD -> AccentCyan
        PowerUpType.SPEED_BOOST -> AccentYellow
        PowerUpType.BERSERKER -> AccentRed
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelBlue)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = type.iconRes()),
            contentDescription = null,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(type.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(
                text = if (owned > 0) "Owned: $owned" else "Locked — earn via Battle Pass",
                fontSize = 11.sp,
                color = if (owned > 0) AccentGreen else Color.Gray
            )
        }
        Button(
            onClick = onToggle,
            enabled = owned > 0,
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEquipped) AccentGreen else AccentPurple,
                disabledContainerColor = Color.DarkGray
            )
        ) {
            Text(
                if (isEquipped) "EQUIPPED" else "EQUIP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StartRunButton(onStart: () -> Unit, onBack: () -> Unit) {
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)
    ) {
        Text("START RUN", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Black)
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onBack,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text("BACK", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** Drawable resource for a power-up type (kept local to the UI layer). */
private fun PowerUpType.iconRes(): Int = when (this) {
    PowerUpType.SHIELD -> R.drawable.ic_shield
    PowerUpType.SPEED_BOOST -> R.drawable.ic_speed
    PowerUpType.BERSERKER -> R.drawable.ic_berserker
}