package com.example.videovault

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import androidx.camera.view.LifecycleCameraController
import com.example.videovault.data.model.VideoRecording
import com.example.videovault.data.repository.VideoRecordingRepository
import com.example.videovault.data.service.RecordingService
import com.example.videovault.viewmodel.RecordingsViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var viewModel: RecordingsViewModel
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
    fun `Test RecordingsViewModel Initialization`() = runTest {
        viewModel = RecordingsViewModel(repository, recordingService)
        val mockRecordings = emptyList<VideoRecording>()
        coEvery { repository.getAllRecordings() } returns flowOf(mockRecordings)
        assertNotNull(viewModel)
    }

    @Test
    fun `Test Load Recordings`() = runTest {
        viewModel = RecordingsViewModel(repository, recordingService)
        val listOfRecordings = listOf(
            VideoRecording(0, "path1", "path1",15.0),
            VideoRecording(1, "path2", "path2",10.0)
        )
        coEvery { repository.getAllRecordings() } returns flowOf(listOfRecordings)

        viewModel.loadRecordings()
        advanceUntilIdle()

        assertEquals(listOfRecordings, viewModel.recordings.value)
    }

    @Test
    fun `Test File Preparation Fails and Sends UiMessage for Failure`() = runTest {
        viewModel = RecordingsViewModel(repository, recordingService)
        val outputFileResult = Result.failure<File>(Exception())
        coEvery { repository.prepareFile(context) } returns outputFileResult

        val listOfRecordings = listOf(
            VideoRecording(0, "path1", "path1",15.0),
            VideoRecording(1, "path2", "path2",10.0)
        )
        coEvery { repository.getAllRecordings() } returns flowOf(listOfRecordings)

        every { context.checkPermission(Manifest.permission.CAMERA,any(),any() ) } returns PackageManager.PERMISSION_GRANTED
        every { context.checkPermission(Manifest.permission.RECORD_AUDIO,any(),any() ) } returns PackageManager.PERMISSION_GRANTED


        val messages = mutableListOf<UiMessage>()
        val job = viewModel.uiMessages.onEach { messages.add(it) }.launchIn(this)
        coEvery { recordingService.startRecording(any(), any(), any(), any(), any(), any()) } just Runs

        viewModel.startRecording(context, controller)
        advanceUntilIdle()

        assertTrue(messages.any { it is UiMessage.StringResource && it.resId == R.string.failed_to_prepare_file })
        job.cancel()

    }

    @Test
    fun `Test Stop Recording Works`() = runTest {
        viewModel = RecordingsViewModel(repository, recordingService)
        val outputFileResult = Result.success(File(""))
        coEvery { repository.prepareFile(context) } returns outputFileResult

        val listOfRecordings = listOf(
            VideoRecording(0, "path1", "path1",15.0),
            VideoRecording(1, "path2", "path2",10.0)
        )
        coEvery { repository.getAllRecordings() } returns flowOf(listOfRecordings)

        every { context.checkPermission(Manifest.permission.CAMERA,any(),any() ) } returns PackageManager.PERMISSION_GRANTED
        every { context.checkPermission(Manifest.permission.RECORD_AUDIO,any(),any() ) } returns PackageManager.PERMISSION_GRANTED
        coEvery { recordingService.startRecording(any(), any(), any(), any(), any(), any()) } just Runs
        coEvery {
            recordingService.stopRecording(onStopped = captureLambda(), onError = any())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        viewModel.startRecording(context, controller)
        advanceUntilIdle()
        assertEquals(true, viewModel.isRecording.value)

        viewModel.stopRecording()
        assertEquals(false, viewModel.isRecording.value)
    }

}