package com.mramfix.subtracker

import android.Manifest
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mramfix.subtracker.domain.model.AppSettings
import com.mramfix.subtracker.domain.model.AppThemeMode
import com.mramfix.subtracker.ui.SubTrackerApp
import com.mramfix.subtracker.ui.theme.SubTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Root()
        }
    }

    @Composable
    private fun Root() {
        val app = application as SubTrackerApplication
        val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(settings.notificationsEnabled) {
            if (settings.notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        SubTrackerTheme(
            darkTheme = when (settings.themeMode) {
                AppThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
        ) {
            val background = androidx.compose.material3.MaterialTheme.colorScheme.background
            SideEffect {
                window.setBackgroundDrawable(ColorDrawable(background.toArgb()))
            }
            SubTrackerApp()
        }
    }
}
