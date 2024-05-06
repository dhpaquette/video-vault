package com.example.videovault.data.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.videovault.data.model.VideoRecording
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoRecordingDao {
    @Query("SELECT * FROM video_recordings ORDER BY id ASC")
    fun getAllRecordings(): Flow<List<VideoRecording>>

    @Insert
    suspend fun insert(videoRecording: VideoRecording)

    @Query("DELETE FROM video_recordings WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(id) FROM video_recordings")
    suspend fun getCount(): Int

    @Query("SELECT MAX(counter) FROM video_recordings")
    suspend fun getMaxCounter(): Int?
}
