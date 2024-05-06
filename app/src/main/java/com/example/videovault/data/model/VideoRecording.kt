package com.example.videovault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_recordings")
data class VideoRecording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val counter: Int,
    val recordingName: String,
    val filePath: String,
    val videoSize: Double
)
