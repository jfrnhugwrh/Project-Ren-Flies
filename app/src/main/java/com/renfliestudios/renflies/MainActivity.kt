package com.renfliestudios.renflies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.renfliestudios.renflies.data.SharedPreferencesProgressStore
import com.renfliestudios.renflies.ui.RenFliesApp
import com.renfliestudios.renflies.ui.ToneAudioFeedback
import com.renfliestudios.renflies.ui.theme.RenFliesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = SharedPreferencesProgressStore(this)
        store.load()
        val audio = ToneAudioFeedback(isEnabled = { store.soundEnabled })

        setContent {
            RenFliesTheme {
                RenFliesApp(progressStore = store, audio = audio)
            }
        }
    }
}
