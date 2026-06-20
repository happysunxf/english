package com.morningenglish.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.morningenglish.app.MorningEnglishApp
import com.morningenglish.app.ui.home.HomeScreen
import com.morningenglish.app.ui.permission.PermissionScreen
import com.morningenglish.app.ui.settings.SettingsScreen
import com.morningenglish.app.ui.theme.MorningEnglishTheme

/**
 * Main host activity. Hosts Compose navigation graph:
 *   permission -> home <-> settings
 *
 * Permission screen is shown first on fresh install until critical
 * permissions (notifications, exact alarm, battery whitelist) are granted.
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled in PermissionScreen */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pre-request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MorningEnglishTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()
    val app = MorningEnglishApp.instance
    val settingsRepo = app.settingsRepository

    // Observe permission state
    val settings by settingsRepo.settingsFlow.collectAsState(initial = null)
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings) {
        val s = settings ?: return@LaunchedEffect
        startDestination = if (s.permissionsGranted) "home" else "permission"
    }

    val start = startDestination ?: return

    NavHost(navController = navController, startDestination = start) {
        composable("permission") {
            PermissionScreen(
                onAllGranted = {
                    navController.navigate("home") {
                        popUpTo("permission") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}