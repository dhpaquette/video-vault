package com.example.videovault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videovault.viewmodel.PermissionsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    permissions: ImmutableList<String>,
    permissionsViewModel: PermissionsViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val permissionsState = rememberMultiplePermissionsState(permissions)
    val permissionsGranted by permissionsViewModel.permissionsGranted.collectAsStateWithLifecycle()
    
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        permissionsViewModel.updatePermissionsGranted(permissionsState.allPermissionsGranted)
    }

    if(permissionsGranted) {
        content()
    } else {
        PermissionsRequester(permissionsState)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsRequester(permissionsState: MultiplePermissionsState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Camera and audio permissions are required for this app")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
            Text("Request permissions")
        }
    }
}