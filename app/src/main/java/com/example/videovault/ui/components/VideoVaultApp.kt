package com.example.videovault.ui.components

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.videovault.Destination
import kotlinx.collections.immutable.toImmutableList

@Composable
fun VideoVaultApp() {
    val permissions = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).toImmutableList()

    PermissionHandler(permissions = permissions) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = Destination.HomeDestination.route) {
            composable(Destination.HomeDestination.route) { HomeScreen(navController) }
            composable(Destination.CameraDestination.route) { CameraScreen(navController) }
        }
    }
}