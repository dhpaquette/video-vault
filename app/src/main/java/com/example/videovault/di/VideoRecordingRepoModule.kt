package com.example.videovault.di

import com.example.videovault.data.repository.VideoRecordingRepository
import com.example.videovault.data.repository.VideoRecordingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class VideoRecordingRepoModule {
    @Binds
    abstract fun bindVideoRecordingRepo(impl: VideoRecordingRepositoryImpl): VideoRecordingRepository
}