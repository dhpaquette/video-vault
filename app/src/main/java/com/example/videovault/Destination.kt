package com.example.videovault

sealed class Destination(val route: String) {
    object HomeDestination: Destination("home")
    object CameraDestination: Destination("camera")
}