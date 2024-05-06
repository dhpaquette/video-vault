package com.example.videovault.data.repository

import android.content.Context
import com.example.videovault.data.model.VideoRecording
import kotlinx.coroutines.flow.Flow
import java.io.File

interface VideoRecordingRepository {

        fun getAllRecordings(): Flow<List<VideoRecording>>

        suspend fun insert(videoRecording: VideoRecording)

        suspend fun deleteById(id: Int)

        suspend fun prepareFile(context: Context): Result<File>

        suspend fun getMaxCounter(): Int

}
