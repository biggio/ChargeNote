package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.EVAppTheme
import com.example.ui.viewmodel.EVViewModel

enum class AppScreen {
    DASHBOARD,
    HISTORY,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private val viewModel: EVViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeConfig by viewModel.themeConfig.collectAsState()

            EVAppTheme(themeConfig = themeConfig) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

                    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
                        currentScreen = AppScreen.DASHBOARD
                    }

                    when (currentScreen) {
                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToHistory = { currentScreen = AppScreen.HISTORY },
                                onNavigateToSettings = { currentScreen = AppScreen.SETTINGS }
                            )
                        }
                        AppScreen.HISTORY -> {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
                            )
                        }
                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
                            )
                        }
                    }
                }
            }
        }
    }
}
