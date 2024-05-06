package com.example.videovault.ui.components

import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.videovault.Destination
import com.example.videovault.viewmodel.RecordingsViewModel


@Composable
fun CameraScreen(
    navController: NavHostController,
    viewModel: RecordingsViewModel = hiltViewModel()
) {
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()


    val appContext = LocalContext.current.applicationContext
    val controller = remember {
        LifecycleCameraController(appContext).apply {
            setEnabledUseCases(CameraController.VIDEO_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    LaunchedEffect(key1 = true) {
        viewModel.uiMessages.collect { uiMessage ->
            val message = uiMessage.asString(appContext)
            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {

        CameraPreview(controller)
        Button(
            onClick = {
                if (isRecording) {
                    viewModel.stopRecording()
                    navController.navigate(Destination.HomeDestination.route)
                } else {
                    viewModel.startRecording(appContext, controller)
                }
            },
            colors = ButtonDefaults.buttonColors(
               containerColor = if (isRecording) Color.Red else Color.Green
            ),
            shape = RectangleShape
        ) {
            Text(text = if (isRecording) "Stop Recording" else "Start Recording")
        }
    }
}
@Composable
fun CameraPreview(
    controller: LifecycleCameraController
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    this.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}