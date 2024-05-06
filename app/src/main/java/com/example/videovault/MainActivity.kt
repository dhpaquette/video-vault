package com.example.videovault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.videovault.ui.components.CameraScreen
import com.example.videovault.ui.components.HomeScreen
import com.example.videovault.ui.theme.VideoVaultTheme
import com.example.videovault.util.Util
import com.example.videovault.util.Util.hasPermissions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(this, Util.CAMERAX_PERMISSIONS,0)
        }
        setContent {
            VideoVaultTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Destination.HomeDestination.route) {
                    composable(Destination.HomeDestination.route) { HomeScreen(navController) }
                    composable(Destination.CameraDestination.route) { CameraScreen(navController) }
                }
            }
        }
    }
}