package com.renfliestudios.renflies.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.renfliestudios.renflies.data.ProgressStore
import com.renfliestudios.renflies.game.PowerUpType
import com.renfliestudios.renflies.game.audio.AudioFeedback

/** Simple screen-based navigation (no extra navigation dependency needed). */
sealed class Screen {
    data object Menu : Screen()
    data object Loadout : Screen()
    data object Game : Screen()
    data object Leaderboard : Screen()
    data object BattlePass : Screen()
}

@Composable
fun RenFliesApp(progressStore: ProgressStore, audio: AudioFeedback) {
    var screen by remember { mutableStateOf<Screen>(Screen.Menu) }
    var pendingLoadout by remember { mutableStateOf<Map<PowerUpType, Int>>(emptyMap()) }

    when (screen) {
        Screen.Menu -> MainMenuScreen(
            progressStore = progressStore,
            onPlay = { screen = Screen.Loadout },
            onLeaderboard = { screen = Screen.Leaderboard },
            onBattlePass = { screen = Screen.BattlePass }
        )
        Screen.Loadout -> LoadoutScreen(
            progressStore = progressStore,
            onStartRun = { loadout ->
                pendingLoadout = loadout
                screen = Screen.Game
            },
            onBack = { screen = Screen.Menu }
        )
        Screen.Game -> GameScreen(
            progressStore = progressStore,
            audio = audio,
            loadout = pendingLoadout,
            onExitToMenu = { screen = Screen.Menu }
        )
        Screen.Leaderboard -> LeaderboardScreen(
            bestScore = progressStore.bestScore,
            onBack = { screen = Screen.Menu }
        )
        Screen.BattlePass -> BattlePassScreen(
            progressStore = progressStore,
            onBack = { screen = Screen.Menu }
        )
    }
}
