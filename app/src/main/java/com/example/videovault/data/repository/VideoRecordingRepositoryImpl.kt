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

    override suspend fun deleteById(id: Int) = videoRecordingDao.deleteById(id)

    override suspend fun prepareFile(context: Context): Result<File> = runCatching {
        val nextFileIndex = getNextFileIndex()
        val fileName = "recording${nextFileIndex}.mp4"
        File(context.filesDir, fileName)
    }

    override suspend fun getMaxCounter(): Int {
        return videoRecordingDao.getMaxCounter() ?: 0
    }
    internal suspend fun getNextFileIndex(): Int = videoRecordingDao.getCount() + 1
}
