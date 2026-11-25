package com.zobaer53.plantapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zobaer53.plantapp.navigation.AppNavDisplay
import com.zobaer53.plantapp.navigation.GuideLauncher
import com.zobaer53.plantapp.ui.theme.PlantAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must precede super.onCreate: swaps Theme.Plantapp.Starting for postSplashScreenTheme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Transparent status bar, system-managed navigation bar scrim, icon colours from the
        // system dark-mode setting (the same one PlantAppTheme follows).
        enableEdgeToEdge()
        setContent {
            PlantAppTheme {
                // Full-bleed background only; each screen applies its own window insets.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavDisplay(onOpenGuide = ::openGuide)
                }
            }
        }
    }

    private fun openGuide(url: String) {
        if (!GuideLauncher.open(this, url)) {
            Toast.makeText(this, R.string.guide_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}
