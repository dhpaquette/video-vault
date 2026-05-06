package com.example.videovault

import android.content.Context
import com.example.videovault.data.dao.VideoRecordingDao
import com.example.videovault.data.model.VideoRecording
import com.example.videovault.data.repository.DeleteRecordingResult
import com.example.videovault.data.repository.FileDeleteOutcome
import com.example.videovault.data.repository.VideoRecordingRepositoryImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VideoRecordingRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `prepareFile uses timestamp names and does not reuse an existing file`() = runTest {
        val dao = mockk<VideoRecordingDao>(relaxed = true)
        val repository = VideoRecordingRepositoryImpl(dao)
        val context = mockk<Context>()
        every { context.filesDir } returns temporaryFolder.root

        val firstFile = repository.prepareFile(context).getOrThrow()
        assertTrue(firstFile.createNewFile())

        val secondFile = repository.prepareFile(context).getOrThrow()

        assertTrue(firstFile.name.startsWith("recording_"))
        assertTrue(secondFile.name.startsWith("recording_"))
        assertTrue(firstFile.name.endsWith(".mp4"))
        assertTrue(secondFile.name.endsWith(".mp4"))
        assertNotEquals(firstFile.name, secondFile.name)
        coVerify(exactly = 0) { dao.getCount() }
    }

    @Test
    fun `deleteRecording deletes database row and existing file`() = runTest {
        val dao = mockk<VideoRecordingDao>()
        val repository = VideoRecordingRepositoryImpl(dao)
        val file = temporaryFolder.newFile("recording_1.mp4")
        val recording = videoRecording(filePath = file.absolutePath)
        coEvery { dao.deleteById(recording.id) } just Runs

        val result = repository.deleteRecording(recording)

        assertEquals(
            DeleteRecordingResult.Success(FileDeleteOutcome.Deleted),
            result
        )
        assertFalse(file.exists())
    }

    @Test
    fun `deleteRecording treats missing file as successful row deletion`() = runTest {
        val dao = mockk<VideoRecordingDao>()
        val repository = VideoRecordingRepositoryImpl(dao)
        val missingFile = File(temporaryFolder.root, "missing.mp4")
        val recording = videoRecording(filePath = missingFile.absolutePath)
        coEvery { dao.deleteById(recording.id) } just Runs

        val result = repository.deleteRecording(recording)

        assertEquals(
            DeleteRecordingResult.Success(FileDeleteOutcome.Missing),
            result
        )
    }

    @Test
    fun `deleteRecording reports file cleanup failure after deleting database row`() = runTest {
        val dao = mockk<VideoRecordingDao>()
        val repository = VideoRecordingRepositoryImpl(dao)
        val nonEmptyDirectory = temporaryFolder.newFolder("recording_1.mp4")
        File(nonEmptyDirectory, "child").writeText("content")
        val recording = videoRecording(filePath = nonEmptyDirectory.absolutePath)
        coEvery { dao.deleteById(recording.id) } just Runs

        val result = repository.deleteRecording(recording)

        assertEquals(
            DeleteRecordingResult.Success(FileDeleteOutcome.Failed),
            result
        )
        assertTrue(nonEmptyDirectory.exists())
    }

    @Test
    fun `deleteRecording does not delete file when database deletion fails`() = runTest {
        val dao = mockk<VideoRecordingDao>()
        val repository = VideoRecordingRepositoryImpl(dao)
        val file = temporaryFolder.newFile("recording_1.mp4")
        val recording = videoRecording(filePath = file.absolutePath)
        coEvery { dao.deleteById(recording.id) } throws RuntimeException("db failed")

        val result = repository.deleteRecording(recording)

        assertTrue(result is DeleteRecordingResult.DatabaseFailure)
        assertTrue(file.exists())
    }

    private fun videoRecording(
        id: Int = 1,
        counter: Int = 1,
        recordingName: String = "recording_1.mp4",
        filePath: String
    ): VideoRecording {
        return VideoRecording(
            id = id,
            counter = counter,
            recordingName = recordingName,
            filePath = filePath,
            videoSize = 1.0
        )
    }
}
