package com.example.videovault.data.repository

import android.content.Context
import com.example.videovault.data.dao.VideoRecordingDao
import com.example.videovault.data.model.VideoRecording

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import java.io.File

class VideoRecordingRepositoryImpl @Inject constructor(private val videoRecordingDao: VideoRecordingDao) : VideoRecordingRepository {

    override fun getAllRecordings(): Flow<List<VideoRecording>> = videoRecordingDao.getAllRecordings()

    override suspend fun insert(videoRecording: VideoRecording) = videoRecordingDao.insert(videoRecording)

    override suspend fun deleteRecording(videoRecording: VideoRecording): DeleteRecordingResult {
        return try {
            videoRecordingDao.deleteById(videoRecording.id)
            val fileDeleteOutcome = deleteRecordingFile(File(videoRecording.filePath))
            DeleteRecordingResult.Success(fileDeleteOutcome)
        } catch (throwable: Throwable) {
            DeleteRecordingResult.DatabaseFailure(throwable)
        }
    }

    override suspend fun prepareFile(context: Context): Result<File> = runCatching {
        val timestampMillis = System.currentTimeMillis()
        var suffix = 0
        var file = File(context.filesDir, createRecordingFileName(timestampMillis, suffix))
        while (file.exists()) {
            suffix += 1
            file = File(context.filesDir, createRecordingFileName(timestampMillis, suffix))
        }
        file
    }

    override suspend fun getMaxCounter(): Int {
        return videoRecordingDao.getMaxCounter() ?: 0
    }

    private fun createRecordingFileName(timestampMillis: Long, suffix: Int): String {
        return if (suffix == 0) {
            "recording_$timestampMillis.mp4"
        } else {
            "recording_${timestampMillis}_$suffix.mp4"
        }
    }

    private fun deleteRecordingFile(file: File): FileDeleteOutcome {
        return when {
            !file.exists() -> FileDeleteOutcome.Missing
            file.delete() -> FileDeleteOutcome.Deleted
            else -> FileDeleteOutcome.Failed
        }
    }
}
