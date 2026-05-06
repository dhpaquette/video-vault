package com.example.videovault.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videovault.R
import com.example.videovault.data.service.RecordingService
import com.example.videovault.UiMessage
import com.example.videovault.data.model.VideoRecording
import com.example.videovault.data.repository.DeleteRecordingResult
import com.example.videovault.data.repository.FileDeleteOutcome
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

    private val uiMessageChannel = Channel<UiMessage>(Channel.BUFFERED)
    val uiMessages = uiMessageChannel.receiveAsFlow()

    private val recordingSavedChannel = Channel<Unit>(Channel.BUFFERED)
    val recordingSavedEvents = recordingSavedChannel.receiveAsFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isStoppingRecording = MutableStateFlow(false)
    val isStoppingRecording: StateFlow<Boolean> = _isStoppingRecording.asStateFlow()

    private val _recordings = MutableStateFlow<List<VideoRecording>>(emptyList())
    val recordings: StateFlow<List<VideoRecording>> = _recordings.asStateFlow()

    init {
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
                    recordingService.startRecording(
                        controller,
                        outputFile,
                        context,
                        onStart = {
                            _isRecording.value = true
                            _isStoppingRecording.value = false
                        },
                        onError = { error: String ->
                            resetRecordingState()
                            sendUiMessage(
                                UiMessage.DynamicMessage(error)
                            )
                        },
                        onFinish = {
                            saveRecordingDetails(outputFile)
                        }
                    )
                },
                onFailure = {
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
                val nextCounter = repository.getMaxCounter() + 1
                val videoRecording = VideoRecording(
                    counter = nextCounter,
                    recordingName = file.name,
                    filePath = file.absolutePath,
                    videoSize = (file.length() / (1024.0* 1024.0))
                )
                repository.insert(videoRecording)
                resetRecordingState()
                sendUiMessage(
                    UiMessage.StringResource(
                        R.string.recording_finished_successfully
                    )
                )
                recordingSavedChannel.send(Unit)
            } catch (e: Exception) {
                resetRecordingState()
                sendUiMessage(
                    UiMessage.StringResource(
                        R.string.failed_to_save_video_details
                    )
                )
            }
        }
    }
    fun stopRecording() {
        if (!_isRecording.value || _isStoppingRecording.value) {
            return
        }
        _isStoppingRecording.value = true
        recordingService.stopRecording(onError = { error ->
            resetRecordingState()
            sendUiMessage(
                UiMessage.DynamicMessage(error)
            )
        })
    }

    fun deleteRecording(videoRecording: VideoRecording) {
        viewModelScope.launch {
            when (val result = repository.deleteRecording(videoRecording)) {
                is DeleteRecordingResult.Success -> {
                    if (result.fileDeleteOutcome == FileDeleteOutcome.Failed) {
                        sendUiMessage(
                            UiMessage.StringResource(
                                R.string.recording_file_cleanup_failed
                            )
                        )
                    }
                }
                is DeleteRecordingResult.DatabaseFailure -> {
                    sendUiMessage(
                        UiMessage.StringResource(
                            R.string.failed_to_delete_recording
                        )
                    )
                }
            }
        }
    }

    private fun resetRecordingState() {
        _isRecording.value = false
        _isStoppingRecording.value = false
    }
}
