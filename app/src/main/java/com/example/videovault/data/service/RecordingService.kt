package com.example.videovault.data.service

import android.content.Context
import androidx.camera.view.LifecycleCameraController
import java.io.File

interface RecordingService {
    fun startRecording(controller: LifecycleCameraController, file: File, context: Context, onStart: () -> Unit, onError: (String) -> Unit, onFinish: () -> Unit)
    fun stopRecording(onStopped: () -> Unit, onError: (String) -> Unit)
}
