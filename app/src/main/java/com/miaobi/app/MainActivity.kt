package com.miaobi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.miaobi.app.ui.navigation.MiaobiNavHost
import com.miaobi.app.ui.screens.welcome.WelcomeGuide
import com.miaobi.app.ui.theme.MiaobiTheme
import com.miaobi.app.util.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showWelcome by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                showWelcome = !settingsManager.welcomeShown.first()
            }

            MiaobiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (showWelcome) {
                        null -> { /* Loading */ }
                        true -> WelcomeGuide(
                            onDismiss = {
                                lifecycleScope.launch {
                                    settingsManager.setWelcomeShown()
                                }
                                showWelcome = false
                            },
                            onStartWriting = {
                                lifecycleScope.launch {
                                    settingsManager.setWelcomeShown()
                                }
                                showWelcome = false
                            }
                        )
                        false -> MiaobiNavHost()
                    }
                }
            }
        }
    }
}
