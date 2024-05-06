package com.example.videovault.di

import android.content.Context
import androidx.room.Room
import com.example.videovault.data.dao.VideoRecordingDao
import com.example.videovault.data.database.VideoRecorderDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext appContext: Context): VideoRecorderDatabase {
        return Room.databaseBuilder(
            appContext,
            VideoRecorderDatabase::class.java,
            "video_recorder_database"
        ).build()
    }

    @Singleton
    @Provides
    fun provideVideoRecordingDao(database: VideoRecorderDatabase): VideoRecordingDao {
        return database.videoRecordingDao()
    }
}
