package com.example.videovault.data.service

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingServiceImpl @Inject constructor() : RecordingService {
    private var currentRecording: Recording? = null
    @SuppressLint("MissingPermission")
    override fun startRecording(controller: LifecycleCameraController, file: File, context: Context, onStart: () -> Unit, onError: (String) -> Unit, onFinish: () -> Unit) {
        val outputOptions = FileOutputOptions.Builder(file).build()
        try {

            currentRecording = controller.startRecording(
                outputOptions,
                AudioConfig.create(true),
                ContextCompat.getMainExecutor(context)
            ) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            onError("Error during recording")
                        } else {
                            onFinish()
                        }
                    }
                }
            }
            onStart()
        } catch (e: Exception) {
            onError("Failed to start recording: ${e.localizedMessage}")
        }
    }
    override fun stopRecording(onStopped: () -> Unit, onError: (String) -> Unit) {
        try {
            currentRecording?.stop()
            currentRecording = null
            onStopped()
        } catch (e: Exception) {
            onError("Failed to stop recording: ${e.localizedMessage}")
        }
    }
}
