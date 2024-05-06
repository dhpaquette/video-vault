package com.example.videovault.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.videovault.data.dao.VideoRecordingDao
import com.example.videovault.data.model.VideoRecording

@Database(entities = [VideoRecording::class], version = 1, exportSchema = false)
abstract class VideoRecorderDatabase : RoomDatabase() {
    abstract fun videoRecordingDao(): VideoRecordingDao
}
