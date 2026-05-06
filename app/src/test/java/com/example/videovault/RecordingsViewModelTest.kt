package com.example.videovault

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import androidx.camera.view.LifecycleCameraController
import com.example.videovault.data.model.VideoRecording
import com.example.videovault.data.repository.DeleteRecordingResult
import com.example.videovault.data.repository.FileDeleteOutcome
import com.example.videovault.data.repository.VideoRecordingRepository
import com.example.videovault.data.service.RecordingService
import com.example.videovault.viewmodel.RecordingsViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: VideoRecordingRepository
    private lateinit var recordingService: RecordingService
    private lateinit var context: Context
    private lateinit var controller: LifecycleCameraController

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(TextUtils::class)
        mockkStatic(android.os.Process::class)

        repository = mockk<VideoRecordingRepository>()
        recordingService = mockk<RecordingService>()
        context = mockk<Context>()
        controller = mockk<LifecycleCameraController>()

        every { repository.getAllRecordings() } returns flowOf(emptyList())
        every { android.os.Process.myPid() } returns 124132
        every { android.os.Process.myUid() } returns 131345
        every { TextUtils.equals(any(), any()) } answers {
            firstArg<String>() == secondArg<String>()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(TextUtils::class)
        unmockkStatic(android.os.Process::class)
    }

    @Test
    fun `initialization loads recordings`() = runTest {
        val recordings = listOf(videoRecording(id = 1, counter = 1))
        every { repository.getAllRecordings() } returns flowOf(recordings)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel)
        assertEquals(recordings, viewModel.recordings.value)
    }

    @Test
    fun `file preparation failure sends failure message`() = runTest {
        val viewModel = createViewModel()
        grantRecordingPermissions()
        coEvery { repository.prepareFile(context) } returns Result.failure(Exception())

        val messages = mutableListOf<UiMessage>()
        val job = viewModel.uiMessages.onEach { messages.add(it) }.launchIn(this)

        viewModel.startRecording(context, controller)
        advanceUntilIdle()

        assertTrue(
            messages.any {
                it is UiMessage.StringResource && it.resId == R.string.failed_to_prepare_file
            }
        )
        job.cancel()
    }

    @Test
    fun `stop recording waits for finalize and save before emitting saved event`() = runTest {
        val outputFile = File.createTempFile("recording", ".mp4")
        outputFile.writeText("video")
        val viewModel = createViewModel()
        grantRecordingPermissions()

        lateinit var finishRecording: () -> Unit
        coEvery { repository.prepareFile(context) } returns Result.success(outputFile)
        coEvery { repository.getMaxCounter() } returns 0
        coEvery { repository.insert(any()) } just Runs
        every {
            recordingService.startRecording(any(), any(), any(), any(), any(), any())
        } answers {
            arg<() -> Unit>(3).invoke()
            finishRecording = arg<() -> Unit>(5)
        }
        every { recordingService.stopRecording(any()) } just Runs

        val savedEvent = async { viewModel.recordingSavedEvents.first() }

        viewModel.startRecording(context, controller)
        advanceUntilIdle()
        assertTrue(viewModel.isRecording.value)

        viewModel.stopRecording()
        advanceUntilIdle()
        assertTrue(viewModel.isRecording.value)
        assertTrue(viewModel.isStoppingRecording.value)
        assertFalse(savedEvent.isCompleted)

        finishRecording()
        savedEvent.await()
        advanceUntilIdle()

        assertFalse(viewModel.isRecording.value)
        assertFalse(viewModel.isStoppingRecording.value)
        coVerify {
            repository.insert(
                match {
                    it.counter == 1 &&
                        it.recordingName == outputFile.name &&
                        it.filePath == outputFile.absolutePath
                }
            )
        }
    }

    @Test
    fun `recording start error resets recording state and sends message`() = runTest {
        val outputFile = File.createTempFile("recording", ".mp4")
        val viewModel = createViewModel()
        grantRecordingPermissions()
        coEvery { repository.prepareFile(context) } returns Result.success(outputFile)
        every {
            recordingService.startRecording(any(), any(), any(), any(), any(), any())
        } answers {
            arg<() -> Unit>(3).invoke()
            arg<(String) -> Unit>(4).invoke("camera failed")
        }

        val messages = mutableListOf<UiMessage>()
        val job = viewModel.uiMessages.onEach { messages.add(it) }.launchIn(this)

        viewModel.startRecording(context, controller)
        advanceUntilIdle()

        assertFalse(viewModel.isRecording.value)
        assertFalse(viewModel.isStoppingRecording.value)
        assertTrue(
            messages.any {
                it is UiMessage.DynamicMessage && it.value == "camera failed"
            }
        )
        job.cancel()
    }

    @Test
    fun `file cleanup failure after delete sends warning message`() = runTest {
        val recording = videoRecording(id = 1)
        val viewModel = createViewModel()
        coEvery {
            repository.deleteRecording(recording)
        } returns DeleteRecordingResult.Success(FileDeleteOutcome.Failed)

        val messages = mutableListOf<UiMessage>()
        val job = viewModel.uiMessages.onEach { messages.add(it) }.launchIn(this)

        viewModel.deleteRecording(recording)
        advanceUntilIdle()

        assertTrue(
            messages.any {
                it is UiMessage.StringResource &&
                    it.resId == R.string.recording_file_cleanup_failed
            }
        )
        job.cancel()
    }

    @Test
    fun `database failure during delete sends failure message`() = runTest {
        val recording = videoRecording(id = 1)
        val viewModel = createViewModel()
        coEvery {
            repository.deleteRecording(recording)
        } returns DeleteRecordingResult.DatabaseFailure(RuntimeException("db failed"))

        val messages = mutableListOf<UiMessage>()
        val job = viewModel.uiMessages.onEach { messages.add(it) }.launchIn(this)

        viewModel.deleteRecording(recording)
        advanceUntilIdle()

        assertTrue(
            messages.any {
                it is UiMessage.StringResource && it.resId == R.string.failed_to_delete_recording
            }
        )
        job.cancel()
    }

    private fun createViewModel(): RecordingsViewModel {
        return RecordingsViewModel(repository, recordingService)
    }

    private fun grantRecordingPermissions() {
        every {
            context.checkPermission(Manifest.permission.CAMERA, any(), any())
        } returns PackageManager.PERMISSION_GRANTED
        every {
            context.checkPermission(Manifest.permission.RECORD_AUDIO, any(), any())
        } returns PackageManager.PERMISSION_GRANTED
    }

    private fun videoRecording(
        id: Int = 0,
        counter: Int = 1,
        recordingName: String = "recording.mp4",
        filePath: String = "/tmp/recording.mp4",
        videoSize: Double = 1.0
    ): VideoRecording {
        return VideoRecording(
            id = id,
            counter = counter,
            recordingName = recordingName,
            filePath = filePath,
            videoSize = videoSize
        )
    }
}
