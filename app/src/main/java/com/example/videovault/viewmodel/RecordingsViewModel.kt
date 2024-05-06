package com.example.videovault.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videovault.R
import com.example.videovault.data.service.RecordingService
import com.example.videovault.UiMessage
import com.example.videovault.data.model.VideoRecording
import com.example.videovault.data.repository.VideoRecordingRepository
import com.example.videovault.util.Util
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val repository: VideoRecordingRepository,
    private val recordingService: RecordingService
) : ViewModel() {

    private val uiMessageChannel = Channel<UiMessage>()
    val uiMessages = uiMessageChannel.receiveAsFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordings = MutableStateFlow<List<VideoRecording>>(emptyList())
    val recordings: StateFlow<List<VideoRecording>> = _recordings.asStateFlow()

    init {
        print("Reaches init")
        loadRecordings()
    }
    fun loadRecordings() {
        viewModelScope.launch {
            repository.getAllRecordings().collect { listOfRecordings ->
                _recordings.value = listOfRecordings
            }
        }
    }
    fun sendUiMessage(uiMessage: UiMessage) {
        viewModelScope.launch {
            uiMessageChannel.send(uiMessage)
        }
    }

    //Suppressing this on the AudioConfig as my Util already checks permissions
    @SuppressLint("MissingPermission")
    fun startRecording(context: Context, controller: LifecycleCameraController) {
        viewModelScope.launch(Dispatchers.Main) {
            if (!Util.hasPermissions(context)) {
                sendUiMessage(
                    UiMessage.StringResource(
                        R.string.enable_permissions_to_record_video
                    )
                )
                return@launch
            }
            val outputFileResult = repository.prepareFile(context)
            outputFileResult.fold(
                onSuccess = { outputFile ->
                    println("Reaches onSuccess")
                    _isRecording.value = true
                    recordingService.startRecording(
                        controller,
                        outputFile,
                        context,
                        onStart = {
                        },
                        onError = { error: String ->
                            sendUiMessage(
                                UiMessage.DynamicMessage(error)
                            )
                        },
                        onFinish = {
                            sendUiMessage(
                                UiMessage.StringResource(
                                    R.string.recording_finished_successfully
                                )
                            )
                            saveRecordingDetails(outputFile)
                        }
                    )
                },
                onFailure = { error ->

                    println("Reaches onFailure")
                    sendUiMessage(
                        UiMessage.StringResource(
                            R.string.failed_to_prepare_file
                        )
                    )
                }
            )
        }
    }
    private fun saveRecordingDetails(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("deletionTest", repository.getMaxCounter().toString())
                val videoRecording = VideoRecording(
                    counter = repository.getMaxCounter() + 1,
                    recordingName = file.name,
                    filePath = file.absolutePath,
                    videoSize = (file.length() / (1024.0* 1024.0))
                )
                repository.insert(videoRecording)
            } catch (e: Exception) {
                sendUiMessage(
                    UiMessage.StringResource(
                        R.string.failed_to_save_video_details
                    )
                )
            }
        }
    }
    fun stopRecording() {
        if (!_isRecording.value) {
            return
        }
        recordingService.stopRecording(onStopped = {
            sendUiMessage(
                UiMessage.StringResource(
                    R.string.recording_stopped_successfully
                )
            )
            _isRecording.value = false
        }, onError = { error ->
            sendUiMessage(
                UiMessage.DynamicMessage(error)
            )
            _isRecording.value = false
        })
    }
    fun deleteRecording(recordingId: Int) {
        viewModelScope.launch {
            repository.deleteById(recordingId)
        }
    }
}
