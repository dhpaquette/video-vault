package com.example.videovault.di

import com.example.videovault.data.service.RecordingService
import com.example.videovault.data.service.RecordingServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
@InstallIn(SingletonComponent::class)
@Module
abstract class RecordingServiceModule {
        @Binds
        abstract fun bindRecordingService(impl: RecordingServiceImpl): RecordingService
}